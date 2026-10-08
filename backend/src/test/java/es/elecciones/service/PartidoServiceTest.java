package es.elecciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import es.elecciones.model.Composicion;
import es.elecciones.model.Composicion.Escanos;
import es.elecciones.model.Partido;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PartidoServiceTest {

    private static final Composicion COMPOSICION = new Composicion(
            List.of(new Escanos("AU", 180), new Escanos("AU-CAT", 20), new Escanos("NUEVO", 1), new Escanos("ZP", 149)),
            List.of(), 350, "https://congreso.test");

    // AU-CAT es una federación de AU: mismo QID → se suman.
    private static final Map<String, String> QIDS = Map.of("AU", "Q1", "AU-CAT", "Q1", "ZP", "Q2");

    private static final List<Partido> FICHAS = List.of(
            new Partido("Q1", "Zeta Álamo Unido", "siglas-wikidata", "https://au.example", "https://logo", 1982, "#1D84CE", null, null, "x"),
            new Partido("Q2", "Ánade Popular", null, null, null, null, null, null, null, "x"));

    @Test
    void cruzaEscanosConFichasYOrdenaPorNombre() {
        List<Partido> lista = PartidoService.combinar(COMPOSICION, QIDS, FICHAS);

        assertThat(lista).extracting(Partido::nombre).containsExactly("Ánade Popular", "NUEVO", "Zeta Álamo Unido");
        Partido au = lista.get(2);
        assertThat(au.web()).isEqualTo("https://au.example");
        assertThat(au.color()).isEqualTo("#1D84CE");
        assertThat(au.fuente()).isEqualTo("https://www.wikidata.org/wiki/Q1");
    }

    @Test
    void lasFormacionesConElMismoQidSeSumanYConservanElDesglose() {
        Partido au = PartidoService.combinar(COMPOSICION, QIDS, FICHAS).get(2);

        assertThat(au.escanos()).isEqualTo(200);
        assertThat(au.siglas()).isEqualTo("AU"); // la candidatura con más escaños, no las siglas de Wikidata
        assertThat(au.candidaturas()).containsExactly(new Escanos("AU", 180), new Escanos("AU-CAT", 20));
    }

    @Test
    void unaFormacionSinQidApareceIgualPeroSinFicha() {
        Partido nuevo = PartidoService.combinar(COMPOSICION, QIDS, List.of()).stream()
                .filter(p -> p.siglas().equals("NUEVO")).findFirst().orElseThrow();
        assertThat(nuevo.id()).isNull();
        assertThat(nuevo.fuente()).isNull();
        assertThat(nuevo.escanos()).isEqualTo(1);
        assertThat(nuevo.candidaturas()).containsExactly(new Escanos("NUEVO", 1));
    }

    @Test
    void sinWikidataSeMuestranLasSiglasComoNombre() {
        assertThat(PartidoService.combinar(COMPOSICION, QIDS, List.of()))
                .extracting(Partido::nombre).containsExactly("AU", "NUEVO", "ZP");
    }
}
