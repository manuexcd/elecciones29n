package es.elecciones.model;

import java.util.List;

/**
 * Reparto de escaños del Congreso al final de la legislatura saliente (Open Data del Congreso).
 *
 * <p>Ambas listas van en orden alfabético, no por número de escaños (criterio de neutralidad).
 *
 * @param porFormacion Escaños por formación electoral, tal como la nombra el Congreso (p. ej. el
 *                     PSC-PSOE aparece aparte del PSOE: así se presentaron a las elecciones).
 * @param porGrupo     Escaños por grupo parlamentario.
 * @param total        Número de diputados contados (350 si el dato es coherente).
 * @param fuente       URL de la página de datos abiertos de origen.
 */
public record Composicion(List<Escanos> porFormacion, List<Escanos> porGrupo, int total, String fuente) {

    public record Escanos(String nombre, int escanos) {}
}
