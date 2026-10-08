package es.elecciones.service;

import es.elecciones.cache.CachedSource;
import es.elecciones.client.CongresoClient;
import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Composicion;
import es.elecciones.model.Respuesta;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class CongresoService {

    private static final Logger log = LoggerFactory.getLogger(CongresoService.class);

    private final CachedSource<Composicion> congreso;

    public CongresoService(CongresoClient client, ElectionProperties props, MeterRegistry registry) {
        this.congreso = new CachedSource<>("congreso", client::composicion, props.congreso().ttl());

        Gauge.builder("elecciones.fuente.ultima.actualizacion.segundos", congreso, CachedSource::lastSuccessEpochSeconds)
                .tag("fuente", "congreso")
                .register(registry);
        Gauge.builder("elecciones.fuente.fallos.consecutivos", congreso, CachedSource::consecutiveFailures)
                .tag("fuente", "congreso")
                .register(registry);
    }

    public Respuesta<Composicion> composicion() {
        return congreso.get();
    }

    @EventListener(ApplicationReadyEvent.class)
    void precalentar() {
        try {
            congreso.get();
        } catch (RuntimeException e) {
            log.warn("No se pudo precalentar la caché del Congreso; se reintentará con la primera petición", e);
        }
    }
}
