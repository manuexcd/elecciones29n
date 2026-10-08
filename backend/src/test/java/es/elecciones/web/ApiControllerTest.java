package es.elecciones.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import es.elecciones.cache.FuenteNoDisponibleException;
import es.elecciones.model.Encuestas;
import es.elecciones.model.Partido;
import es.elecciones.model.Respuesta;
import es.elecciones.service.CalendarioService;
import es.elecciones.service.CongresoService;
import es.elecciones.service.EncuestasService;
import java.time.Duration;
import java.time.LocalDate;
import es.elecciones.service.PartidoService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApiController.class)
class ApiControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean PartidoService partidos;
    @MockitoBean CalendarioService calendario;
    @MockitoBean CongresoService congreso;
    @MockitoBean EncuestasService encuestas;

    private static final Partido PARTIDO =
            new Partido("Q1", "Álamo Unido", "AU", "https://au.example", null, 1982, "#1D84CE", 7, List.of(), "https://www.wikidata.org/wiki/Q1");

    @Test
    void listaDePartidosIncluyeCabecerasParaLaCdn() throws Exception {
        given(partidos.partidos()).willReturn(new Respuesta<>(List.of(PARTIDO), Instant.parse("2026-10-07T08:00:00Z"), false, "wikidata"));

        mvc.perform(get("/api/partidos"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("s-maxage=300")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("stale-while-revalidate=3600")))
                .andExpect(jsonPath("$.datos[0].nombre").value("Álamo Unido"))
                .andExpect(jsonPath("$.datos[0].escanos").value(7))
                .andExpect(jsonPath("$.desactualizado").value(false))
                .andExpect(jsonPath("$.actualizado").value("2026-10-07T08:00:00Z"));
    }

    @Test
    void partidoInexistenteDevuelve404() throws Exception {
        given(partidos.partido("Q999")).willReturn(Optional.empty());
        mvc.perform(get("/api/partidos/Q999")).andExpect(status().isNotFound());
    }

    @Test
    void fuenteCaidaSinCacheDevuelve503() throws Exception {
        given(partidos.partidos()).willThrow(new FuenteNoDisponibleException("wikidata", new RuntimeException("timeout")));
        mvc.perform(get("/api/partidos"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Fuente de datos no disponible"));
    }

    @Test
    void encuestasNoSePuedenCachearMasAllaDelInicioDeLaVeda() throws Exception {
        var datos = new Encuestas(List.of("PP"), List.of(new Encuestas.Encuesta("Zeta", "1–6 Oct",
                LocalDate.of(2026, 10, 6), 1000, List.of(new Encuestas.Estimacion("PP", 34.1, "146/148")), null)), "https://w");
        given(encuestas.encuestas()).willReturn(Optional.of(new Respuesta<>(datos, Instant.parse("2026-11-23T22:00:00Z"), false, "wikipedia")));
        given(calendario.hastaLaVeda()).willReturn(Duration.ofMinutes(10));

        mvc.perform(get("/api/encuestas"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=60, public, s-maxage=300, stale-if-error=300, stale-while-revalidate=300"))
                .andExpect(jsonPath("$.datos.encuestas[0].estimaciones[0].escanos").value("146/148"));
    }

    @Test
    void duranteLaVedaLasEncuestasDevuelven451SinCache() throws Exception {
        given(encuestas.encuestas()).willReturn(Optional.empty());

        mvc.perform(get("/api/encuestas"))
                .andExpect(status().is(451))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.title").value("Periodo de veda de encuestas"));
    }

    @Test
    void cabecerasDeCacheHastaLaVeda() {
        // Lejos de la veda: lo mismo que el resto de la API.
        assertThat(ApiController.hastaLaVeda(Duration.ofDays(10)).getHeaderValue())
                .isEqualTo("max-age=60, public, s-maxage=300, stale-if-error=86400, stale-while-revalidate=3600");
        // A 3 minutos: nada puede servirse pasados esos 3 minutos.
        assertThat(ApiController.hastaLaVeda(Duration.ofMinutes(3)).getHeaderValue())
                .isEqualTo("max-age=60, public, s-maxage=180");
        assertThat(ApiController.hastaLaVeda(Duration.ZERO).getHeaderValue()).isEqualTo("no-store");
    }
}
