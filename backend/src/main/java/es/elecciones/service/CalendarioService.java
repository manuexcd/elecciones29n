package es.elecciones.service;

import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Calendario;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CalendarioService {

    private final ElectionProperties props;
    private final Clock clock;

    public CalendarioService(ElectionProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    public Calendario calendario() {
        List<Calendario.Hito> hitos = props.hitos().stream()
                .map(h -> new Calendario.Hito(h.fecha(), h.titulo(), h.fuente()))
                .sorted(Comparator.comparing(Calendario.Hito::fecha))
                .toList();
        return new Calendario(props.fechaElecciones(), hitos, props.vedaEncuestasDesde(), encuestasPublicables());
    }

    /**
     * false desde el primer día de la veda (LOREG art. 69.7: los cinco días anteriores a la votación)
     * en adelante, en hora peninsular. Es el único sitio que decide si se muestran encuestas.
     */
    public boolean encuestasPublicables() {
        return LocalDate.now(clock).isBefore(props.vedaEncuestasDesde());
    }

    /**
     * Tiempo que falta hasta las 00:00 del primer día de la veda (cero o negativo si ya ha empezado).
     * Sirve para que ningún caché pueda guardar encuestas más allá de ese momento.
     */
    public Duration hastaLaVeda() {
        return Duration.between(clock.instant(), props.vedaEncuestasDesde().atStartOfDay(clock.getZone()));
    }
}
