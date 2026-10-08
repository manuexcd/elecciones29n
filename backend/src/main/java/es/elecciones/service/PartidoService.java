package es.elecciones.service;

import es.elecciones.cache.CachedSource;
import es.elecciones.cache.FuenteNoDisponibleException;
import es.elecciones.client.WikidataClient;
import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Composicion;
import es.elecciones.model.Partido;
import es.elecciones.model.Respuesta;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.text.Collator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Lista de partidos: QUÉ partidos aparecen lo decide el Congreso (formaciones con escaños en la
 * legislatura saliente); Wikidata solo aporta la ficha (nombre, logo, web, fundación).
 *
 * <p>Si Wikidata no responde, la lista se sirve igual con los datos del Congreso y marcada como
 * desactualizada: la caída de la fuente secundaria no tumba la página.
 */
@Service
public class PartidoService {

    static final String FUENTE = "congreso+wikidata";

    private static final Logger log = LoggerFactory.getLogger(PartidoService.class);

    private final CongresoService congreso;
    private final CachedSource<List<Partido>> wikidata;
    private final Map<String, String> qidPorFormacion;

    public PartidoService(WikidataClient client, CongresoService congreso, ElectionProperties props, MeterRegistry registry) {
        this.congreso = congreso;
        this.qidPorFormacion = props.formaciones().stream()
                .collect(Collectors.toUnmodifiableMap(ElectionProperties.Formacion::congreso, ElectionProperties.Formacion::wikidata));
        List<String> qids = qidPorFormacion.values().stream().distinct().sorted().toList();
        this.wikidata = new CachedSource<>("wikidata", () -> client.partidos(qids), props.wikidata().ttl());

        // Una métrica por fuente: alerta si "ahora - última actualización" crece demasiado.
        Gauge.builder("elecciones.fuente.ultima.actualizacion.segundos", wikidata, CachedSource::lastSuccessEpochSeconds)
                .tag("fuente", "wikidata")
                .register(registry);
        Gauge.builder("elecciones.fuente.fallos.consecutivos", wikidata, CachedSource::consecutiveFailures)
                .tag("fuente", "wikidata")
                .register(registry);
    }

    public Respuesta<List<Partido>> partidos() {
        Respuesta<Composicion> composicion = congreso.composicion(); // sin Congreso no hay lista → 503
        Respuesta<List<Partido>> fichas;
        try {
            fichas = wikidata.get();
        } catch (FuenteNoDisponibleException e) {
            fichas = new Respuesta<>(List.of(), composicion.actualizado(), true, "wikidata");
        }
        List<Partido> lista = combinar(composicion.datos(), qidPorFormacion, fichas.datos());
        // La fecha que se muestra es la del dato más antiguo de los dos.
        Instant actualizado = composicion.actualizado().isBefore(fichas.actualizado())
                ? composicion.actualizado() : fichas.actualizado();
        return new Respuesta<>(lista, actualizado, composicion.desactualizado() || fichas.desactualizado(), FUENTE);
    }

    public Optional<Respuesta<Partido>> partido(String id) {
        Respuesta<List<Partido>> todos = partidos();
        return todos.datos().stream()
                .filter(p -> id.equals(p.id()))
                .findFirst()
                .map(p -> new Respuesta<>(p, todos.actualizado(), todos.desactualizado(), todos.fuente()));
    }

    /**
     * Visible para tests. Una entrada por QID (las formaciones del Congreso con el mismo QID se suman,
     * p. ej. el PSOE y sus federaciones) o por formación si no tiene QID. Orden alfabético por nombre.
     */
    static List<Partido> combinar(Composicion composicion, Map<String, String> qidPorFormacion, List<Partido> fichas) {
        Map<String, Partido> fichaPorQid = fichas.stream().collect(Collectors.toMap(Partido::id, Function.identity(), (a, b) -> a));
        // Clave de agrupación: el QID, o la propia formación si no tiene. Conserva el orden de llegada.
        Map<String, List<Composicion.Escanos>> grupos = new LinkedHashMap<>();
        for (Composicion.Escanos f : composicion.porFormacion()) {
            String qid = qidPorFormacion.get(f.nombre());
            if (qid == null) {
                log.warn("Formación del Congreso sin QID en elecciones.formaciones: '{}'", f.nombre());
            }
            grupos.computeIfAbsent(qid != null ? qid : "formacion:" + f.nombre(), k -> new ArrayList<>()).add(f);
        }

        List<Partido> lista = new ArrayList<>();
        grupos.forEach((clave, candidaturas) -> {
            String qid = clave.startsWith("formacion:") ? null : clave;
            Partido ficha = qid == null ? null : fichaPorQid.get(qid);
            List<Composicion.Escanos> desglose = candidaturas.stream()
                    .sorted(Comparator.comparingInt(Composicion.Escanos::escanos).reversed()
                            .thenComparing(Composicion.Escanos::nombre))
                    .toList();
            String siglas = desglose.getFirst().nombre();
            int escanos = desglose.stream().mapToInt(Composicion.Escanos::escanos).sum();
            lista.add(new Partido(
                    qid,
                    ficha != null ? ficha.nombre() : siglas,
                    siglas,
                    ficha != null ? ficha.web() : null,
                    ficha != null ? ficha.logo() : null,
                    ficha != null ? ficha.fundacion() : null,
                    ficha != null ? ficha.color() : null,
                    escanos,
                    desglose,
                    qid != null ? "https://www.wikidata.org/wiki/" + qid : null));
        });
        Collator collator = Collator.getInstance(Locale.of("es"));
        lista.sort(Comparator.comparing(Partido::nombre, collator).thenComparing(Partido::siglas, collator));
        return List.copyOf(lista);
    }

    /** Precalienta la caché al arrancar para que el primer visitante no espere a la fuente. */
    @EventListener(ApplicationReadyEvent.class)
    void precalentar() {
        try {
            wikidata.get();
        } catch (RuntimeException e) {
            log.warn("No se pudo precalentar la caché de Wikidata; se reintentará con la primera petición", e);
        }
    }
}
