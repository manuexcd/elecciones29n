package es.elecciones.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import tools.jackson.databind.ObjectMapper;
import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Partido;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WikidataClientTest {

    private static final String SPARQL_URL = "https://query.wikidata.org/sparql";

    private static final String RESPUESTA = """
            {"head":{"vars":["p","pLabel","siglas","web","logo","inicio"]},
             "results":{"bindings":[
               {"p":{"value":"http://www.wikidata.org/entity/Q2"},"pLabel":{"value":"Zeta Partido"},
                "web":{"value":"https://zeta.example"},"color":{"value":"red\\" onload=\\"alert(1)"}},
               {"p":{"value":"http://www.wikidata.org/entity/Q1"},"pLabel":{"value":"Álamo Unido"},
                "siglas":{"value":"AU"},"inicio":{"value":"1982-10-02T00:00:00Z"},"color":{"value":"1d84ce"}},
               {"p":{"value":"http://www.wikidata.org/entity/Q1"},"pLabel":{"value":"Álamo Unido"},
                "logo":{"value":"http://commons.wikimedia.org/wiki/Special:FilePath/AU.svg"},
                "web":{"value":"javascript:alert(1)"}},
               {"p":{"value":"http://www.wikidata.org/entity/Q3"},"pLabel":{"value":"Q3"}}
             ]}}
            """;

    private WikidataClient client(MockRestServiceServer[] serverOut) {
        RestClient.Builder builder = RestClient.builder();
        serverOut[0] = MockRestServiceServer.bindTo(builder).build();
        var props = new ElectionProperties("test-agent/1.0", new ElectionProperties.Wikidata(SPARQL_URL, Duration.ofHours(1)),
                null, null, null, List.of(), List.of());
        return new WikidataClient(builder, props);
    }

    @Test
    void fusionaFilasDuplicadasOrdenaYDescartaEntradasSinEtiqueta() {
        MockRestServiceServer[] server = new MockRestServiceServer[1];
        WikidataClient client = client(server);
        server[0].expect(requestTo(org.hamcrest.Matchers.startsWith(SPARQL_URL)))
                .andExpect(header(HttpHeaders.USER_AGENT, "test-agent/1.0"))
                .andRespond(withSuccess(RESPUESTA, MediaType.valueOf("application/sparql-results+json")));

        List<Partido> partidos = client.partidos(List.of("Q1", "Q2", "Q3"));

        assertThat(partidos).extracting(Partido::id).containsExactly("Q1", "Q2"); // Álamo antes que Zeta; Q3 descartado
        Partido alamo = partidos.get(0);
        assertThat(alamo.siglas()).isEqualTo("AU");
        assertThat(alamo.fundacion()).isEqualTo(1982);
        assertThat(alamo.logo()).isEqualTo("https://commons.wikimedia.org/wiki/Special:FilePath/AU.svg?width=200");
        assertThat(alamo.web()).isNull(); // "javascript:" descartado
        assertThat(alamo.fuente()).isEqualTo("https://www.wikidata.org/wiki/Q1");
        assertThat(alamo.color()).isEqualTo("#1D84CE");
        assertThat(partidos.get(1).web()).isEqualTo("https://zeta.example");
        assertThat(partidos.get(1).color()).isNull(); // no es hexadecimal: descartado
        server[0].verify();
    }

    @Test
    void rechazaIdsQueNoSonQidAntesDeMeterlosEnLaConsulta() {
        MockRestServiceServer[] server = new MockRestServiceServer[1];
        WikidataClient client = client(server);
        assertThatThrownBy(() -> client.partidos(List.of("Q1 } ?x ?y ?z . {")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaRespuestaConOtroFormatoLanzaExcepcion() throws Exception {
        var raiz = new ObjectMapper().readTree("{\"error\":\"algo cambió\"}");
        assertThatThrownBy(() -> WikidataClient.parse(raiz)).isInstanceOf(IllegalStateException.class);
    }
}
