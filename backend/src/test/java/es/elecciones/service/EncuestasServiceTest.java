package es.elecciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import es.elecciones.client.EncuestasClient;
import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Encuestas;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class EncuestasServiceTest {

    private final EncuestasClient client = mock(EncuestasClient.class);
    private final CalendarioService calendario = mock(CalendarioService.class);
    private final EncuestasService service = new EncuestasService(client, calendario,
            new ElectionProperties("test", null, null, null, null, List.of(), List.of(),
                    new ElectionProperties.Encuestas("http://x", "p", 2026, Duration.ofHours(1))),
            new SimpleMeterRegistry());

    @Test
    void antesDeLaVedaDevuelveLasEncuestas() {
        given(calendario.encuestasPublicables()).willReturn(true);
        given(client.encuestas()).willReturn(new Encuestas(List.of(), List.of(), "https://w"));

        assertThat(service.encuestas()).isPresent();
    }

    @Test
    void duranteLaVedaNoDevuelveNadaNiConsultaLaFuente() {
        given(calendario.encuestasPublicables()).willReturn(false);

        assertThat(service.encuestas()).isEmpty();
        verify(client, never()).encuestas();
    }
}
