package es.elecciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import es.elecciones.config.AppConfig;
import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Calendario;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalendarioServiceTest {

    private static final ElectionProperties PROPS = new ElectionProperties(
            "test",
            new ElectionProperties.Wikidata("http://x", Duration.ofHours(1)),
            null,
            LocalDate.of(2026, 11, 29),
            LocalDate.of(2026, 11, 24),
            List.of(
                    new ElectionProperties.Hito(LocalDate.of(2026, 11, 29), "Elecciones", "BOE"),
                    new ElectionProperties.Hito(LocalDate.of(2026, 10, 6), "BOE", "BOE")),
            List.of());

    private CalendarioService en(String fechaHora) {
        var instante = LocalDateTime.parse(fechaHora).atZone(AppConfig.ZONA).toInstant();
        return new CalendarioService(PROPS, Clock.fixed(instante, AppConfig.ZONA));
    }

    @Test
    void ordenaLosHitosCronologicamente() {
        Calendario c = en("2026-10-07T10:00:00").calendario();
        assertThat(c.hitos()).extracting(Calendario.Hito::titulo).containsExactly("BOE", "Elecciones");
    }

    @Test
    void antesDeLaVedaSePuedenPublicarEncuestas() {
        assertThat(en("2026-11-23T23:59:59").calendario().encuestasPublicables()).isTrue();
    }

    @Test
    void desdeElDia24NoSePuedenPublicarEncuestas() {
        assertThat(en("2026-11-24T00:00:00").calendario().encuestasPublicables()).isFalse();
        assertThat(en("2026-11-29T12:00:00").calendario().encuestasPublicables()).isFalse();
    }
}
