package es.elecciones.client;

import static org.assertj.core.api.Assertions.assertThat;

import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Partido;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Test de CONTRATO: llama a Wikidata de verdad. No se ejecuta con {@code ./gradlew test};
 * se lanza con {@code ./gradlew contractTest} desde el workflow programado, para enterarse
 * de que la fuente ha cambiado antes que los usuarios.
 */
@Tag("contract")
class WikidataContractTest {

    @Test
    void todosLosQidDeLaConfiguracionTienenFichaConNombre() {
        ElectionProperties props = ConfiguracionReal.cargar();
        List<String> qids = props.formaciones().stream().map(ElectionProperties.Formacion::wikidata).distinct().toList();

        List<Partido> partidos = new WikidataClient(RestClient.builder(), props).partidos(qids);

        // Si un QID se fusiona o se borra en Wikidata, deja de aparecer aquí.
        assertThat(partidos).extracting(Partido::id).containsExactlyInAnyOrderElementsOf(qids);
        assertThat(partidos).allSatisfy(p -> assertThat(p.nombre()).isNotBlank().isNotEqualTo(p.id()));
        assertThat(partidos).anyMatch(p -> p.web() != null);
        assertThat(partidos).anyMatch(p -> p.color() != null);
    }
}
