package es.elecciones.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Encuestas;
import es.elecciones.model.Encuestas.Encuesta;
import es.elecciones.model.Encuestas.Estimacion;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class EncuestasClientTest {

    private static final String FUENTE = "https://en.wikipedia.org/w/index.php?oldid=1";

    /**
     * Reproduce la estructura real: encabezado del año envuelto en div.mw-heading, dos filas de
     * cabecera (logos y colores), rowspan en las filas del CIS, notas [a], «?», «–» y referencias.
     * Después viene la tabla de 2025, que no debe leerse.
     */
    private static final String HTML = """
            <div class="mw-heading mw-heading5"><h5 id="2026">2026</h5></div>
            <style>.sticky{}</style>
            <table class="wikitable collapsible sticky-header"><tbody>
            <tr>
              <th rowspan="2">Polling firm/Commissioner</th><th rowspan="2">Fieldwork date</th>
              <th rowspan="2">Sample size</th><th rowspan="2">Turnout</th>
              <th><a href="/wiki/PP" title="PP"><img alt="PP" src="x.png"></a></th>
              <th><a href="/wiki/Adelante" title="Adelante Andalucía (2021)"><img src="y.png"></a></th>
              <th><a href="/wiki/Vox" title="Vox (political party)">Vox</a></th>
              <th rowspan="2">Lead</th>
            </tr>
            <tr><th style="background:#00f"></th><th style="background:#0f0"></th><th style="background:#0a0"></th></tr>
            <tr>
              <td>Zeta/Diario<sup class="reference"><a href="#cite_note-1">[1]</a></sup></td>
              <td>1–6 Oct</td><td>1,000</td><td>63.1</td>
              <td style="background:#C2E2F8"><b>34.1</b><br>146/148</td><td>–</td>
              <td>18.2<sup class="reference"><a href="#cite_note-a">[a]</a></sup><br>61</td><td>15.9</td>
            </tr>
            <tr>
              <td>CIS<sup class="reference"><a href="#cite_note-2">[2]</a></sup></td>
              <td rowspan="2">28 Dec 2025–9 Jan</td><td rowspan="2">4,000</td><td>?</td>
              <td>25.5</td><td>?<br>1</td><td>16.6</td><td>?</td>
            </tr>
            <tr>
              <td>CIS (Re-estimación)</td><td>?</td>
              <td>? 127</td><td>–</td><td>–</td><td>?</td>
            </tr>
            <tr><td colspan="8">Convocatoria de elecciones</td></tr>
            </tbody></table>
            <div class="mw-heading mw-heading5"><h5 id="2025">2025</h5></div>
            <table class="wikitable"><tbody><tr><th>Otra</th></tr></tbody></table>
            <ol class="references">
              <li id="cite_note-1"><a class="external text" href="https://diario.example/encuesta">Encuesta</a></li>
              <li id="cite_note-2"><a class="external text" href="javascript:alert(1)">CIS</a></li>
            </ol>
            """;

    @Test
    void leeLaTablaDelAnioConRowspanNotasYReferencias() {
        Encuestas e = EncuestasClient.parse(HTML, 2026, FUENTE);

        assertThat(e.fuente()).isEqualTo(FUENTE);
        // Alfabético (Wikipedia los ordena por resultado), sin "(2021)" y sin Turnout ni Lead. Si la
        // cabecera tiene texto se usa ese ("Vox"), no el título del enlace ("Vox (political party)").
        assertThat(e.partidos()).containsExactly("Adelante Andalucía", "PP", "Vox");
        assertThat(e.encuestas()).hasSize(3);

        Encuesta zeta = e.encuestas().getFirst();
        assertThat(zeta.encuestadora()).isEqualTo("Zeta/Diario");
        assertThat(zeta.trabajoDeCampo()).isEqualTo("1–6 Oct");
        assertThat(zeta.fin()).isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(zeta.muestra()).isEqualTo(1000);
        assertThat(zeta.estimaciones()).containsExactly(
                new Estimacion("PP", 34.1, "146/148"),
                new Estimacion("Vox", 18.2, "61"));
        assertThat(zeta.enlace()).isEqualTo("https://diario.example/encuesta");

        // La fila con rowspan hereda fecha y muestra; el enlace javascript: se descarta.
        Encuesta cis = e.encuestas().get(1);
        assertThat(cis.encuestadora()).isEqualTo("CIS");
        assertThat(cis.fin()).isEqualTo(LocalDate.of(2026, 1, 9));
        assertThat(cis.estimaciones()).containsExactly(
                new Estimacion("Adelante Andalucía", null, "1"),
                new Estimacion("PP", 25.5, null),
                new Estimacion("Vox", 16.6, null));
        assertThat(cis.enlace()).isNull();

        Encuesta reestimacion = e.encuestas().get(2);
        assertThat(reestimacion.encuestadora()).isEqualTo("CIS (Re-estimación)");
        assertThat(reestimacion.muestra()).isEqualTo(4000);
        assertThat(reestimacion.estimaciones()).containsExactly(new Estimacion("PP", null, "127"));
    }

    @Test
    void siCambiaLaCabeceraFallaEnLugarDeCruzarColumnas() {
        String cambiada = HTML.replace("Fieldwork date", "Dates");
        assertThatThrownBy(() -> EncuestasClient.parse(cambiada, 2026, FUENTE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cabecera");
    }

    @Test
    void siNoHaySeccionDelAnioFalla() {
        assertThatThrownBy(() -> EncuestasClient.parse(HTML, 2027, FUENTE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2027");
    }

    @Test
    void finDelTrabajoDeCampo() {
        assertThat(EncuestasClient.finTrabajoDeCampo("5 Oct", 2026)).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(EncuestasClient.finTrabajoDeCampo("29 Sep–3 Oct", 2026)).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(EncuestasClient.finTrabajoDeCampo("28 Dec 2025–3 Jan", 2026)).isEqualTo(LocalDate.of(2026, 1, 3));
        assertThat(EncuestasClient.finTrabajoDeCampo("1–30 Sept", 2026)).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(EncuestasClient.finTrabajoDeCampo("?", 2026)).isNull();
        assertThat(EncuestasClient.finTrabajoDeCampo("31 Feb", 2026)).isNull();
    }

    @Test
    void pideLaPaginaALaApiDeMediaWikiYEnlazaLaRevision() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var props = new ElectionProperties("test-agent/1.0", null, null, null, null, List.of(), List.of(),
                new ElectionProperties.Encuestas("https://en.wikipedia.test/w/api.php", "Opinion polling", 2026, Duration.ofHours(1)));
        var respuesta = new ObjectMapper().createObjectNode();
        respuesta.putObject("parse").put("revid", 1379160151L).put("text", HTML);

        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://en.wikipedia.test/w/api.php?action=parse")))
                .andExpect(queryParam("page", "Opinion%20polling"))
                .andExpect(header(HttpHeaders.USER_AGENT, "test-agent/1.0"))
                .andRespond(withSuccess(respuesta.toString(), MediaType.APPLICATION_JSON));

        Encuestas e = new EncuestasClient(builder, props).encuestas();

        assertThat(e.fuente()).isEqualTo("https://en.wikipedia.test/w/index.php?oldid=1379160151");
        assertThat(e.encuestas()).hasSize(3);
        server.verify();
    }
}
