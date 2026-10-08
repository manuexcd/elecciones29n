package es.elecciones.client;

import static org.assertj.core.api.Assertions.assertThat;

import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Composicion;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Test de CONTRATO: llama al Open Data del Congreso de verdad (ver {@link WikidataContractTest}). */
@Tag("contract")
class CongresoContractTest {

    @Test
    void elCongresoDevuelve350DiputadosYTodasSusFormacionesTienenQid() {
        ElectionProperties props = ConfiguracionReal.cargar();
        Composicion c = new CongresoClient(RestClient.builder(), props).composicion();

        assertThat(c.total()).isEqualTo(350);
        assertThat(c.porGrupo()).anyMatch(g -> g.nombre().contains("Mixto"));

        // Si falla, añade la formación a elecciones.formaciones (application.yml).
        List<String> mapeadas = props.formaciones().stream().map(ElectionProperties.Formacion::congreso).toList();
        assertThat(c.porFormacion()).extracting(Composicion.Escanos::nombre).allMatch(mapeadas::contains);
    }
}
