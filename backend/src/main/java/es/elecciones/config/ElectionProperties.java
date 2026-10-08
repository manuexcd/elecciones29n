package es.elecciones.config;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de la aplicación (ver application.yml).
 *
 * @param userAgent         User-Agent que se envía a las fuentes. Wikimedia exige que identifique
 *                          el proyecto y un contacto; cámbialo antes de desplegar.
 * @param fechaElecciones   Día de la votación.
 * @param vedaEncuestasDesde Primer día en que NO se pueden publicar encuestas (LOREG art. 69.7).
 * @param hitos             Calendario electoral (BOE, RD 806/2026 y derivados de la LOREG).
 * @param formaciones       Correspondencia formación del Congreso → QID de Wikidata.
 */
@ConfigurationProperties(prefix = "elecciones")
public record ElectionProperties(
        String userAgent,
        Wikidata wikidata,
        Congreso congreso,
        LocalDate fechaElecciones,
        LocalDate vedaEncuestasDesde,
        List<Hito> hitos,
        List<Formacion> formaciones) {

    public record Wikidata(String sparqlUrl, Duration ttl) {}

    /**
     * @param baseUrl         Raíz del sitio (los enlaces de la página índice son relativos).
     * @param indice          Ruta de la página de datos abiertos de diputados.
     * @param fechaDisolucion Fecha de baja que reciben los diputados al disolverse las Cortes.
     */
    public record Congreso(String baseUrl, String indice, LocalDate fechaDisolucion, Duration ttl) {}

    public record Hito(LocalDate fecha, String titulo, String fuente) {}

    /**
     * @param congreso Nombre exacto en el campo FORMACIONELECTORAL del Congreso.
     * @param wikidata QID de su ficha en Wikidata.
     */
    public record Formacion(String congreso, String wikidata) {}
}
