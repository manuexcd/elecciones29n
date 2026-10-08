package es.elecciones.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Modelo común de partido. La interfaz solo conoce este record: cada fuente aporta sus campos y se
 * fusionan aquí.
 *
 * @param id        Identificador estable (QID de Wikidata). Null si la formación no está en
 *                  {@code elecciones.formaciones}: se muestra igual, pero sin ficha.
 * @param nombre    Nombre completo (Wikidata; si no hay ficha, las siglas).
 * @param siglas    Siglas, tal como las nombra el Congreso. Si el partido agrupa varias candidaturas
 *                  (PSOE y sus federaciones), las de la que tiene más escaños.
 * @param web       Web oficial (solo http/https).
 * @param logo      URL de la imagen del logo, si existe.
 * @param fundacion Año de fundación, si se conoce.
 * @param color     Color del partido (Wikidata P465), "#RRGGBB"; null si no consta o no es válido.
 * @param escanos   Escaños en el Congreso al final de la legislatura saliente (Open Data del Congreso).
 *                  Null solo en las fichas de Wikidata antes de cruzarlas con el Congreso.
 * @param candidaturas Desglose de {@code escanos} por candidatura del Congreso (una sola si no agrupa).
 *                  Null en el mismo caso que {@code escanos}.
 *
 * <p>Los componentes {@code @Nullable} salen en el OpenAPI como {@code type: [X, "null"]} (ver
 * {@code NulabilidadRecords}); el resto, como obligatorios y no nulos.
 * @param fuente    URL de la ficha de Wikidata, para que el usuario pueda comprobar el dato.
 */
public record Partido(
        @Nullable String id,
        String nombre,
        @Nullable String siglas,
        @Nullable String web,
        @Nullable String logo,
        @Nullable Integer fundacion,
        @Nullable String color,
        @Nullable Integer escanos,
        @Nullable List<Composicion.Escanos> candidaturas,
        @Nullable String fuente) {

    /** Completa los campos vacíos de este partido con los del otro (misma entidad, filas distintas). */
    public Partido fusionar(Partido otro) {
        return new Partido(
                id,
                nombre,
                siglas != null ? siglas : otro.siglas,
                web != null ? web : otro.web,
                logo != null ? logo : otro.logo,
                fundacion != null ? fundacion : otro.fundacion,
                color != null ? color : otro.color,
                escanos != null ? escanos : otro.escanos,
                candidaturas != null ? candidaturas : otro.candidaturas,
                fuente);
    }
}
