package es.elecciones.client;

import com.fasterxml.jackson.databind.JsonNode;
import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Partido;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Obtiene de Wikidata (SPARQL) la ficha de los partidos indicados: nombre, siglas, web, logo y año
 * de fundación.
 *
 * <p>Wikidata NO decide qué partidos se muestran (una consulta genérica devuelve ~960 entidades:
 * municipales, juveniles, históricos...); solo enriquece los QID de {@code elecciones.formaciones}.
 */
@Component
public class WikidataClient {

    // %s = lista de "wd:Qnnn". Idiomas de reserva: el BNG, por ejemplo, no tiene etiqueta en español.
    private static final String SPARQL = """
            SELECT ?p ?pLabel ?siglas ?web ?logo ?inicio ?color WHERE {
              VALUES ?p { %s }
              OPTIONAL { ?p wdt:P1813 ?siglas }
              OPTIONAL { ?p wdt:P856  ?web }
              OPTIONAL { ?p wdt:P154  ?logo }
              OPTIONAL { ?p wdt:P571  ?inicio }
              OPTIONAL { ?p wdt:P465  ?color }
              SERVICE wikibase:label { bd:serviceParam wikibase:language "es,gl,ca,eu,en". }
            }
            """;

    private static final Pattern QID = Pattern.compile("Q\\d+");
    private static final Pattern COLOR = Pattern.compile("[0-9A-Fa-f]{6}");

    private static final String ENTITY_PREFIX = "http://www.wikidata.org/entity/";

    private final RestClient http;
    private final String sparqlUrl;

    public WikidataClient(RestClient.Builder builder, ElectionProperties props) {
        this.http = builder.defaultHeader(HttpHeaders.USER_AGENT, props.userAgent()).build();
        this.sparqlUrl = props.wikidata().sparqlUrl();
    }

    public List<Partido> partidos(Collection<String> qids) {
        // Los QID vienen de la configuración, pero se validan igual: van dentro de la consulta.
        String values = qids.stream()
                .peek(q -> {
                    if (!QID.matcher(q).matches()) {
                        throw new IllegalArgumentException("QID no válido: " + q);
                    }
                })
                .map(q -> "wd:" + q)
                .collect(Collectors.joining(" "));
        JsonNode root = http.get()
                .uri(sparqlUrl + "?format=json&query={query}", SPARQL.formatted(values))
                .accept(MediaType.valueOf("application/sparql-results+json"))
                .retrieve()
                .body(JsonNode.class);
        return parse(root);
    }

    /** Visible para tests. Una fila por combinación de valores: se fusionan por QID. */
    static List<Partido> parse(JsonNode root) {
        if (root == null || !root.path("results").path("bindings").isArray()) {
            throw new IllegalStateException("Respuesta SPARQL con formato inesperado");
        }
        Map<String, Partido> porId = new LinkedHashMap<>();
        for (JsonNode row : root.path("results").path("bindings")) {
            String uri = value(row, "p");
            if (uri == null || !uri.startsWith(ENTITY_PREFIX)) {
                continue;
            }
            String id = uri.substring(ENTITY_PREFIX.length());
            String nombre = value(row, "pLabel");
            if (nombre == null || nombre.equals(id)) {
                continue; // sin etiqueta en es/en: Wikidata devuelve el propio QID
            }
            Partido fila = new Partido(
                    id,
                    nombre,
                    value(row, "siglas"),
                    soloHttp(value(row, "web")),
                    logo(value(row, "logo")),
                    year(value(row, "inicio")),
                    color(value(row, "color")),
                    null,
                    null,
                    "https://www.wikidata.org/wiki/" + id);
            porId.merge(id, fila, Partido::fusionar);
        }
        Collator collator = Collator.getInstance(Locale.of("es"));
        List<Partido> partidos = new ArrayList<>(porId.values());
        partidos.sort((a, b) -> collator.compare(a.nombre(), b.nombre()));
        return List.copyOf(partidos);
    }

    private static String value(JsonNode row, String field) {
        JsonNode v = row.path(field).path("value");
        return v.isTextual() && !v.asText().isBlank() ? v.asText() : null;
    }

    /** Wikidata es editable por cualquiera: nunca pasamos al frontend un esquema que no sea http(s). */
    private static String soloHttp(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://")) ? url : null;
    }

    /** P465 es "RRGGBB" sin almohadilla. Va a un atributo SVG: solo se acepta hexadecimal estricto. */
    private static String color(String hex) {
        return hex != null && COLOR.matcher(hex).matches() ? "#" + hex.toUpperCase(Locale.ROOT) : null;
    }

    private static String logo(String url) {
        String https = soloHttp(url);
        if (https == null) {
            return null;
        }
        return https.replaceFirst("^http://", "https://") + "?width=200";
    }

    private static Integer year(String date) {
        if (date == null || date.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
