package es.elecciones.model;

import java.time.Instant;

/**
 * Envoltorio de todos los datos que vienen de una fuente externa.
 *
 * @param datos           El dato en sí.
 * @param actualizado     Cuándo se obtuvo correctamente de la fuente.
 * @param desactualizado  true si la fuente falló y se está sirviendo el último dato bueno.
 * @param fuente          Nombre de la fuente.
 */
public record Respuesta<T>(T datos, Instant actualizado, boolean desactualizado, String fuente) {}
