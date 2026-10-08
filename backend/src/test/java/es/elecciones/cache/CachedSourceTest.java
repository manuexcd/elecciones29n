package es.elecciones.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.elecciones.model.Respuesta;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CachedSourceTest {

    /** Reloj manual. */
    static final class TestClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-07T08:00:00Z"));

        void advance(Duration d) {
            now.updateAndGet(i -> i.plus(d));
        }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }

    private final TestClock clock = new TestClock();

    private CachedSource<Integer> source(java.util.function.Supplier<Integer> loader) {
        // executor síncrono: el refresco "en segundo plano" se ejecuta al momento y el test es determinista
        return new CachedSource<>("test", loader, Duration.ofHours(1), Duration.ofMinutes(1), clock, Runnable::run);
    }

    @Test
    void cargaUnaVezDentroDelTtl() {
        AtomicInteger llamadas = new AtomicInteger();
        var s = source(llamadas::incrementAndGet);

        assertThat(s.get().datos()).isEqualTo(1);
        clock.advance(Duration.ofMinutes(59));
        assertThat(s.get().datos()).isEqualTo(1);
        assertThat(llamadas).hasValue(1);
    }

    @Test
    void trasCaducarSirveElDatoAnteriorYRefrescaParaLaSiguiente() {
        AtomicInteger llamadas = new AtomicInteger();
        var s = source(llamadas::incrementAndGet);
        s.get();

        clock.advance(Duration.ofHours(2));
        Respuesta<Integer> durante = s.get(); // stale-while-revalidate: devuelve el viejo y refresca
        Respuesta<Integer> despues = s.get();

        assertThat(durante.datos()).isEqualTo(1);
        assertThat(despues.datos()).isEqualTo(2);
        assertThat(despues.desactualizado()).isFalse();
    }

    @Test
    void siFallaElRefrescoConservaElUltimoDatoBuenoYLoMarcaDesactualizado() {
        AtomicInteger llamadas = new AtomicInteger();
        var s = source(() -> {
            if (llamadas.incrementAndGet() > 1) {
                throw new IllegalStateException("fuente caída");
            }
            return 42;
        });
        Instant primera = s.get().actualizado();

        clock.advance(Duration.ofHours(2));
        s.get(); // dispara el refresco fallido
        Respuesta<Integer> r = s.get();

        assertThat(r.datos()).isEqualTo(42);
        assertThat(r.desactualizado()).isTrue();
        assertThat(r.actualizado()).isEqualTo(primera); // la fecha NO avanza: sigue siendo la del último éxito
        assertThat(s.consecutiveFailures()).isEqualTo(1);
    }

    @Test
    void noMartilleaLaFuenteCaidaAntesDelRetryDelay() {
        AtomicInteger llamadas = new AtomicInteger();
        var s = source(() -> {
            if (llamadas.incrementAndGet() > 1) {
                throw new IllegalStateException("fuente caída");
            }
            return 1;
        });
        s.get();
        clock.advance(Duration.ofHours(2));
        s.get(); // 2.ª llamada al loader (falla)
        s.get();
        s.get();

        assertThat(llamadas).hasValue(2); // nada más hasta pasado el retryDelay

        clock.advance(Duration.ofMinutes(2));
        s.get();
        assertThat(llamadas).hasValue(3);
    }

    @Test
    void sinDatoPrevioLaPrimeraFallaLanzaExcepcion() {
        var s = source(() -> {
            throw new IllegalStateException("fuente caída");
        });
        assertThatThrownBy(s::get).isInstanceOf(FuenteNoDisponibleException.class);
    }

    @Test
    void siSeRecuperaSeReseteaElContadorDeFallos() {
        AtomicInteger llamadas = new AtomicInteger();
        var s = source(() -> {
            int n = llamadas.incrementAndGet();
            if (n == 2) {
                throw new IllegalStateException("fallo puntual");
            }
            return n;
        });
        s.get();
        clock.advance(Duration.ofHours(2));
        s.get();
        assertThat(s.consecutiveFailures()).isEqualTo(1);

        clock.advance(Duration.ofMinutes(2));
        s.get();
        assertThat(s.consecutiveFailures()).isZero();
        assertThat(s.get().desactualizado()).isFalse();
    }
}
