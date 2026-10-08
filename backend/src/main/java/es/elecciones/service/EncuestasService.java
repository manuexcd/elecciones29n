package es.elecciones.service;

import es.elecciones.cache.CachedSource;
import es.elecciones.client.EncuestasClient;
import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Encuestas;
import es.elecciones.model.Respuesta;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** Sondeos de Wikipedia. Durante la veda no se devuelven aunque estén en caché. */
@Service
public class EncuestasService {

    private static final Logger log = LoggerFactory.getLogger(EncuestasService.class);

    private final CachedSource<Encuestas> wikipedia;
    private final CalendarioService calendario;

    public EncuestasService(EncuestasClient client, CalendarioService calendario, ElectionProperties props, MeterRegistry registry) {
        this.calendario = calendario;
        this.wikipedia = new CachedSource<>("wikipedia", client::encuestas, props.encuestas().ttl());

        Gauge.builder("elecciones.fuente.ultima.actualizacion.segundos", wikipedia, CachedSource::lastSuccessEpochSeconds)
                .tag("fuente", "wikipedia")
                .register(registry);
        Gauge.builder("elecciones.fuente.fallos.consecutivos", wikipedia, CachedSource::consecutiveFailures)
                .tag("fuente", "wikipedia")
                .register(registry);
    }

    /** Vacío durante la veda: ni siquiera se consulta la caché. */
    public Optional<Respuesta<Encuestas>> encuestas() {
        return calendario.encuestasPublicables() ? Optional.of(wikipedia.get()) : Optional.empty();
    }

    @EventListener(ApplicationReadyEvent.class)
    void precalentar() {
        try {
            encuestas();
        } catch (RuntimeException e) {
            log.warn("No se pudo precalentar la caché de encuestas; se reintentará con la primera petición", e);
        }
    }
}
