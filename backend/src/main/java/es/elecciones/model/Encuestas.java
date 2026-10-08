package es.elecciones.model;

import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Sondeos de intención de voto publicados, tal como los recoge la tabla de Wikipedia.
 *
 * <p>Criterio de neutralidad: los partidos van en orden alfabético (Wikipedia los ordena por
 * resultado) y no se marca quién va en cabeza. Se muestran todas las encuestas de la tabla, sin
 * filtrar por empresa ni por quién las encarga.
 *
 * @param partidos  Partidos con columna en la tabla, en orden alfabético, con el nombre que les da
 *                  Wikipedia (normalmente las siglas).
 * @param encuestas De la más reciente a la más antigua (fin del trabajo de campo).
 * @param fuente    Enlace permanente a la revisión de Wikipedia de la que se leyó la tabla.
 */
public record Encuestas(List<String> partidos, List<Encuesta> encuestas, String fuente) {

    /**
     * @param encuestadora    Empresa y, tras la barra, quién la encarga (p. ej. "NC Report/La Razón").
     * @param trabajoDeCampo  Fechas tal como aparecen en la tabla (p. ej. "29 Sep–3 Oct").
     * @param fin             Último día del trabajo de campo.
     * @param muestra         Número de entrevistas; null si no consta.
     * @param estimaciones    Solo los partidos para los que la encuesta da algún dato, en el mismo
     *                        orden que {@link Encuestas#partidos()}.
     * @param enlace          Publicación original (primera referencia de la fila), si la hay.
     */
    public record Encuesta(
            String encuestadora,
            String trabajoDeCampo,
            LocalDate fin,
            @Nullable Integer muestra,
            List<Estimacion> estimaciones,
            @Nullable String enlace) {}

    /**
     * @param partido Nombre de la columna en {@link Encuestas#partidos()}.
     * @param voto    Porcentaje de voto estimado; null si la encuesta solo da escaños.
     * @param escanos Escaños estimados tal como los publica la encuesta: un número ("6") o una
     *                horquilla ("146/148"); null si no los da.
     */
    public record Estimacion(String partido, @Nullable Double voto, @Nullable String escanos) {}
}
