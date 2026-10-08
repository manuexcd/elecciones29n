package es.elecciones.model;

import java.time.LocalDate;
import java.util.List;

/**
 * @param fechaElecciones     Día de la votación.
 * @param hitos               Fechas clave en orden cronológico.
 * @param vedaEncuestasDesde  Primer día en que está prohibido publicar encuestas.
 * @param encuestasPublicables false a partir de la veda: el frontend debe ocultar los sondeos.
 */
public record Calendario(
        LocalDate fechaElecciones,
        List<Hito> hitos,
        LocalDate vedaEncuestasDesde,
        boolean encuestasPublicables) {

    public record Hito(LocalDate fecha, String titulo, String fuente) {}
}
