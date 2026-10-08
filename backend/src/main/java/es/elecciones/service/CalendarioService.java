package es.elecciones.service;

import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Calendario;
import java.time.Clock;
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
        LocalDate hoy = LocalDate.now(clock);
        boolean publicables = hoy.isBefore(props.vedaEncuestasDesde());
        return new Calendario(props.fechaElecciones(), hitos, props.vedaEncuestasDesde(), publicables);
    }
}
