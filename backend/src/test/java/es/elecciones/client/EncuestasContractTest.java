package es.elecciones.client;

import static org.assertj.core.api.Assertions.assertThat;

import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Encuestas;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Test de CONTRATO: lee la tabla de sondeos de Wikipedia de verdad (ver {@link WikidataContractTest}). */
@Tag("contract")
class EncuestasContractTest {

    @Test
    void laTablaDeSondeosSigueTeniendoElFormatoEsperado() {
        ElectionProperties props = ConfiguracionReal.cargar();
        Encuestas e = new EncuestasClient(RestClient.builder(), props).encuestas();

        System.out.println(e.encuestas().size() + " encuestas de " + e.partidos() + "; fuente " + e.fuente());

        assertThat(e.fuente()).startsWith("https://en.wikipedia.org/w/index.php?oldid=");
        assertThat(e.partidos()).contains("PP", "PSOE", "Vox", "Sumar");
        assertThat(e.encuestas()).hasSizeGreaterThan(20);
        // Si la tabla se desordena, la mayoría de encuestas dejarían de traer a los dos grandes.
        assertThat(e.encuestas().stream().filter(x -> x.estimaciones().stream().anyMatch(es -> es.partido().equals("PP"))))
                .hasSizeGreaterThan(e.encuestas().size() * 9 / 10);
        assertThat(e.encuestas()).allSatisfy(x -> assertThat(x.fin().getYear()).isEqualTo(props.encuestas().anio()));
        assertThat(e.encuestas().stream().filter(x -> x.enlace() != null)).hasSizeGreaterThan(e.encuestas().size() / 2);
    }
}
