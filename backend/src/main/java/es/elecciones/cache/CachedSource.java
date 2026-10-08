package es.elecciones.cache;

import es.elecciones.model.Respuesta;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caché en memoria de una fuente externa con semántica stale-while-revalidate / stale-if-error:
 *
 * <ul>
 *   <li>La primera petición carga el dato de forma síncrona (si falla, lanza
 *       {@link FuenteNoDisponibleException}).
 *   <li>Cuando el dato caduca, se sigue sirviendo el anterior y se refresca en segundo plano,
 *       así el visitante nunca espera a la fuente.
 *   <li>Si el refresco falla, se conserva el último dato bueno, se marca como desactualizado y no
 *       se reintenta hasta pasado {@code retryDelay} (para no martillear una fuente caída).
 * </ul>
 */
public final class CachedSource<T> {

    private static final Logger log = LoggerFactory.getLogger(CachedSource.class);

    private record Entry<T>(T value, Instant updated, boolean stale, Instant nextRefresh) {}

    private final String fuente;
    private final Supplier<T> loader;
    private final Duration ttl;
    private final Duration retryDelay;
    private final Clock clock;
    private final Executor executor;

    private final ReentrantLock lock = new ReentrantLock(); // no synchronized: evita fijar hilos virtuales
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile Entry<T> entry;

    public CachedSource(String fuente, Supplier<T> loader, Duration ttl) {
        this(fuente, loader, ttl, Duration.ofMinutes(1), Clock.systemUTC(),
                task -> Thread.ofVirtual().name("refresh-" + fuente).start(task));
    }

    /** Constructor completo, usado por los tests (reloj y executor controlables). */
    public CachedSource(String fuente, Supplier<T> loader, Duration ttl, Duration retryDelay,
                        Clock clock, Executor executor) {
        this.fuente = fuente;
        this.loader = loader;
        this.ttl = ttl;
        this.retryDelay = retryDelay;
        this.clock = clock;
        this.executor = executor;
    }

    public Respuesta<T> get() {
        Entry<T> current = entry;
        if (current == null) {
            current = refresh();
        } else if (!clock.instant().isBefore(current.nextRefresh()) && refreshing.compareAndSet(false, true)) {
            try {
                executor.execute(() -> {
                    try {
                        refresh();
                    } catch (RuntimeException ignored) {
                        // ya registrado en refresh()
                    } finally {
                        refreshing.set(false);
                    }
                });
            } catch (RuntimeException e) {
                refreshing.set(false);
                throw e;
            }
        }
        return new Respuesta<>(current.value(), current.updated(), current.stale(), fuente);
    }

    private Entry<T> refresh() {
        lock.lock();
        try {
            Entry<T> previous = entry;
            Instant now = clock.instant();
            if (previous != null && now.isBefore(previous.nextRefresh())) {
                return previous; // otro hilo ya lo refrescó mientras esperábamos el lock
            }
            try {
                Entry<T> fresh = new Entry<>(loader.get(), now, false, now.plus(ttl));
                consecutiveFailures.set(0);
                entry = fresh;
                return fresh;
            } catch (RuntimeException e) {
                consecutiveFailures.incrementAndGet();
                if (previous == null) {
                    throw new FuenteNoDisponibleException(fuente, e);
                }
                log.warn("Fuente '{}' no disponible; se sirve el último dato bueno de {}", fuente, previous.updated(), e);
                Entry<T> stale = new Entry<>(previous.value(), previous.updated(), true, now.plus(retryDelay));
                entry = stale;
                return stale;
            }
        } finally {
            lock.unlock();
        }
    }

    /** Segundos epoch de la última carga correcta (0 si nunca). Para la métrica de Prometheus. */
    public double lastSuccessEpochSeconds() {
        Entry<T> e = entry;
        return e == null ? 0 : e.updated().getEpochSecond();
    }

    /** Fallos consecutivos desde la última carga correcta. Para alertas. */
    public double consecutiveFailures() {
        return consecutiveFailures.get();
    }
}
