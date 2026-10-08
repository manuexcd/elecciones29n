package es.elecciones.client;

import com.fasterxml.jackson.databind.JsonNode;
import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Composicion;
import es.elecciones.model.Composicion.Escanos;
import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Obtiene la composición del Congreso desde su portal de datos abiertos.
 *
 * <p>Los ficheros llevan la fecha de generación en el nombre ({@code DiputadosActivos__20261007050007.json}),
 * así que primero se lee la página índice para localizar los enlaces vigentes.
 *
 * <p>Tras la disolución de las Cortes, "DiputadosActivos" solo contiene a los miembros de la
 * Diputación Permanente; el resto pasa a "DiputadosDeBaja" con fecha de baja igual a la de
 * disolución. La composición final de la legislatura es la unión de ambos.
 */
@Component
public class CongresoClient {

    static final int ESCANOS_CONGRESO = 350;

    private static final Pattern ACTIVOS = Pattern.compile("/webpublica/opendata/diputados/DiputadosActivos__\\d+\\.json");
    private static final Pattern BAJAS = Pattern.compile("/webpublica/opendata/diputados/DiputadosDeBaja__\\d+\\.json");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RestClient http;
    private final String base;
    private final String indice;
    private final LocalDate fechaDisolucion;

    public CongresoClient(RestClient.Builder builder, ElectionProperties props) {
        this.http = builder.defaultHeader(HttpHeaders.USER_AGENT, props.userAgent()).build();
        this.base = props.congreso().baseUrl();
        this.indice = base + props.congreso().indice();
        this.fechaDisolucion = props.congreso().fechaDisolucion();
    }

    public Composicion composicion() {
        String html = http.get().uri(indice).retrieve().body(String.class);
        JsonNode activos = descargar(enlace(html, ACTIVOS));
        JsonNode bajas = descargar(enlace(html, BAJAS));
        return calcular(activos, bajas, fechaDisolucion, indice);
    }

    private JsonNode descargar(String ruta) {
        return http.get().uri(base + ruta).retrieve().body(JsonNode.class);
    }

    /** Visible para tests. */
    static String enlace(String html, Pattern patron) {
        Matcher m = patron.matcher(html == null ? "" : html);
        if (!m.find()) {
            throw new IllegalStateException("No se encuentra el enlace " + patron + " en la página de datos abiertos");
        }
        return m.group();
    }

    /** Visible para tests. Cuenta los activos más las bajas producidas por la disolución. */
    static Composicion calcular(JsonNode activos, JsonNode bajas, LocalDate fechaDisolucion, String fuente) {
        if (activos == null || !activos.isArray() || bajas == null || !bajas.isArray()) {
            throw new IllegalStateException("Respuesta del Congreso con formato inesperado");
        }
        List<JsonNode> diputados = new ArrayList<>();
        activos.forEach(diputados::add);
        if (fechaDisolucion != null) {
            String fecha = FECHA.format(fechaDisolucion);
            for (JsonNode d : bajas) {
                if (fecha.equals(d.path("FECHABAJA").asText())) {
                    diputados.add(d);
                }
            }
        }
        if (diputados.size() != ESCANOS_CONGRESO) {
            // Mejor fallar (y servir el último dato bueno) que publicar un reparto incoherente.
            throw new IllegalStateException("Se esperaban " + ESCANOS_CONGRESO + " diputados y hay " + diputados.size());
        }

        Collator collator = Collator.getInstance(Locale.of("es"));
        Map<String, Integer> porFormacion = new TreeMap<>(collator);
        Map<String, Integer> porGrupo = new TreeMap<>(collator);
        for (JsonNode d : diputados) {
            porFormacion.merge(texto(d, "FORMACIONELECTORAL"), 1, Integer::sum);
            porGrupo.merge(texto(d, "GRUPOPARLAMENTARIO"), 1, Integer::sum);
        }
        return new Composicion(lista(porFormacion), lista(porGrupo), diputados.size(), fuente);
    }

    private static String texto(JsonNode d, String campo) {
        String v = d.path(campo).asText("").strip();
        if (v.isEmpty()) {
            throw new IllegalStateException("Diputado sin " + campo + ": " + d.path("NOMBRE").asText());
        }
        return v;
    }

    private static List<Escanos> lista(Map<String, Integer> mapa) {
        return mapa.entrySet().stream().map(e -> new Escanos(e.getKey(), e.getValue())).toList();
    }
}
