package es.elecciones.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Composicion;
import es.elecciones.model.Composicion.Escanos;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CongresoClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LocalDate DISOLUCION = LocalDate.of(2026, 10, 6);

    private static final String INDICE = """
            <a href="/webpublica/opendata/diputados/DiputadosActivos__20261007050006.csv">CSV</a>
            <a href="/webpublica/opendata/diputados/DiputadosActivos__20261007050007.json">JSON</a>
            <a href="/webpublica/opendata/diputados/DiputadosDeBaja__20261007050010.json">JSON</a>
            """;

    /** n diputados con la misma formación y grupo; con fecha de baja si se indica. */
    private static ArrayNode diputados(int n, String formacion, String grupo, String fechaBaja) {
        ArrayNode lista = JSON.createArrayNode();
        for (int i = 0; i < n; i++) {
            var d = lista.addObject()
                    .put("NOMBRE", formacion + " " + i)
                    .put("FORMACIONELECTORAL", formacion)
                    .put("GRUPOPARLAMENTARIO", grupo);
            if (fechaBaja != null) {
                d.put("FECHABAJA", fechaBaja);
            }
        }
        return lista;
    }

    @Test
    void sumaActivosYBajasPorDisolucionYOrdenaAlfabeticamente() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var props = new ElectionProperties("test-agent/1.0", null,
                new ElectionProperties.Congreso("https://congreso.test", "/es/opendata/diputados", DISOLUCION, Duration.ofHours(1)),
                null, null, List.of(), List.of());
        CongresoClient client = new CongresoClient(builder, props);

        ArrayNode activos = diputados(100, "Zeta", "Grupo Zeta", null);
        ArrayNode bajas = diputados(200, "Álamo", "Grupo Mixto", "06/10/2026");
        bajas.addAll(diputados(50, "Zeta", "Grupo Zeta", "06/10/2026"));
        bajas.addAll(diputados(3, "Zeta", "Grupo Zeta", "01/12/2023")); // bajas antiguas: no cuentan

        server.expect(requestTo("https://congreso.test/es/opendata/diputados"))
                .andExpect(header(HttpHeaders.USER_AGENT, "test-agent/1.0"))
                .andRespond(withSuccess(INDICE, MediaType.TEXT_HTML));
        server.expect(requestTo("https://congreso.test/webpublica/opendata/diputados/DiputadosActivos__20261007050007.json"))
                .andRespond(withSuccess(activos.toString(), MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://congreso.test/webpublica/opendata/diputados/DiputadosDeBaja__20261007050010.json"))
                .andRespond(withSuccess(bajas.toString(), MediaType.APPLICATION_JSON));

        Composicion c = client.composicion();

        assertThat(c.total()).isEqualTo(350);
        assertThat(c.porFormacion()).containsExactly(new Escanos("Álamo", 200), new Escanos("Zeta", 150));
        assertThat(c.porGrupo()).containsExactly(new Escanos("Grupo Mixto", 200), new Escanos("Grupo Zeta", 150));
        assertThat(c.fuente()).isEqualTo("https://congreso.test/es/opendata/diputados");
        server.verify();
    }

    @Test
    void unRepartoQueNoSumaTrescientosCincuentaLanzaExcepcion() {
        JsonNode activos = diputados(349, "Zeta", "Grupo Zeta", null);
        assertThatThrownBy(() -> CongresoClient.calcular(activos, JSON.createArrayNode(), DISOLUCION, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("349");
    }

    @Test
    void siCambiaLaPaginaIndiceLanzaExcepcion() {
        assertThatThrownBy(() -> CongresoClient.enlace("<html>rediseño</html>", java.util.regex.Pattern.compile("x__\\d+")))
                .isInstanceOf(IllegalStateException.class);
    }
}
