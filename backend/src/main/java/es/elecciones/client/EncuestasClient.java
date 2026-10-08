package es.elecciones.client;

import es.elecciones.config.ElectionProperties;
import es.elecciones.model.Encuestas;
import es.elecciones.model.Encuestas.Encuesta;
import es.elecciones.model.Encuestas.Estimacion;
import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Lee la tabla de sondeos de intención de voto de Wikipedia (API de MediaWiki, HTML renderizado).
 *
 * <p>Se pide la página entera y no solo la sección del año porque las referencias (el enlace a la
 * publicación original de cada encuesta) solo se renderizan con la página completa.
 *
 * <p>Wikipedia es editable por cualquiera: los textos se pasan tal cual (React los escapa), los
 * enlaces solo si son http(s), y si la tabla cambia de forma se lanza excepción para que se siga
 * sirviendo el último dato bueno en lugar de publicar columnas cruzadas.
 */
@Component
public class EncuestasClient {

    private static final Logger log = LoggerFactory.getLogger(EncuestasClient.class);

    private static final String EMPRESA = "Polling firm/Commissioner";
    private static final String FECHAS = "Fieldwork date";
    private static final String MUESTRA = "Sample size";
    /** Columnas de la tabla que no son partidos. "Lead" (ventaja del primero) se descarta a propósito. */
    private static final Set<String> NO_PARTIDOS = Set.of(EMPRESA, FECHAS, MUESTRA, "Turnout", "Lead");

    private static final Map<String, Integer> MESES = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("feb", 2), Map.entry("mar", 3), Map.entry("apr", 4),
            Map.entry("may", 5), Map.entry("jun", 6), Map.entry("jul", 7), Map.entry("aug", 8),
            Map.entry("sep", 9), Map.entry("oct", 10), Map.entry("nov", 11), Map.entry("dec", 12));
    /** "3 Oct", "3 Oct 2026"; en "28 Dec 2025–3 Jan" vale la última. */
    private static final Pattern DIA_MES = Pattern.compile("(\\d{1,2})\\s+([A-Za-z]{3})[a-z]*\\.?(?:\\s+(\\d{4}))?");
    private static final Pattern VOTO = Pattern.compile("\\d{1,2}(?:\\.\\d+)?");
    private static final Pattern ESCANOS = Pattern.compile("\\d{1,3}(?:[/–-]\\d{1,3})?");
    private static final Pattern PARENTESIS_FINAL = Pattern.compile("\\s*\\([^)]*\\)$");

    private final RestClient http;
    private final ElectionProperties.Encuestas config;

    public EncuestasClient(RestClient.Builder builder, ElectionProperties props) {
        this.http = builder.defaultHeader(HttpHeaders.USER_AGENT, props.userAgent()).build();
        this.config = props.encuestas();
    }

    public Encuestas encuestas() {
        JsonNode root = http.get()
                .uri(config.apiUrl() + "?action=parse&format=json&formatversion=2&redirects=1"
                                + "&disableeditsection=1&disablelimitreport=1&prop={prop}&page={page}",
                        "text|revid", config.pagina())
                .retrieve()
                .body(JsonNode.class);
        JsonNode parse = root == null ? null : root.path("parse");
        if (parse == null || !parse.path("text").isString() || !parse.path("revid").isNumber()) {
            throw new IllegalStateException("Respuesta de MediaWiki con formato inesperado: "
                    + (root == null ? "vacía" : root.path("error").path("info").asString("")));
        }
        String fuente = config.apiUrl().replaceFirst("/api\\.php$", "/index.php?oldid=") + parse.path("revid").asLong();
        return parse(parse.path("text").asString(), config.anio(), fuente);
    }

    /** Visible para tests. */
    static Encuestas parse(String html, int anio, String fuente) {
        Document doc = Jsoup.parse(html);
        Element tabla = tablaDelAnio(doc, anio);
        List<List<Element>> filas = rejilla(tabla);
        if (filas.isEmpty()) {
            throw new IllegalStateException("Tabla de sondeos vacía");
        }

        List<Element> cabecera = filas.getFirst();
        int colEmpresa = -1, colFechas = -1, colMuestra = -1;
        Map<Integer, String> partidoPorColumna = new HashMap<>();
        for (int c = 0; c < cabecera.size(); c++) {
            Element th = cabecera.get(c);
            if (c > 0 && th == cabecera.get(c - 1)) {
                continue; // misma celda con colspan
            }
            String texto = texto(th);
            switch (texto) {
                case EMPRESA -> colEmpresa = c;
                case FECHAS -> colFechas = c;
                case MUESTRA -> colMuestra = c;
                default -> {
                    if (!NO_PARTIDOS.contains(texto)) {
                        partidoPorColumna.put(c, nombrePartido(th));
                    }
                }
            }
        }
        if (colEmpresa < 0 || colFechas < 0 || colMuestra < 0 || partidoPorColumna.isEmpty()) {
            throw new IllegalStateException("La cabecera de la tabla de sondeos ha cambiado: " + cabecera.stream().map(EncuestasClient::texto).toList());
        }

        Collator collator = Collator.getInstance(Locale.of("es"));
        List<String> partidos = partidoPorColumna.values().stream().distinct().sorted(collator).toList();
        Comparator<Estimacion> alfabetico = Comparator.comparing(Estimacion::partido, collator);

        List<Encuesta> encuestas = new ArrayList<>();
        int descartadas = 0;
        for (List<Element> fila : filas.subList(1, filas.size())) {
            if (fila.size() <= colMuestra || !fila.get(colEmpresa).nameIs("td") || fila.get(colEmpresa) == fila.get(colFechas)) {
                continue; // segunda fila de cabecera (colores) o fila separadora a todo lo ancho
            }
            String encuestadora = texto(fila.get(colEmpresa));
            String fechas = texto(fila.get(colFechas));
            LocalDate fin = finTrabajoDeCampo(fechas, anio);
            if (encuestadora.isEmpty() || fin == null) {
                log.warn("Fila de sondeo sin empresa o con fecha ilegible, se descarta: '{}' '{}'", encuestadora, fechas);
                descartadas++;
                continue;
            }
            List<Estimacion> estimaciones = new ArrayList<>();
            partidoPorColumna.forEach((c, partido) -> {
                if (c < fila.size()) {
                    Estimacion e = estimacion(partido, texto(fila.get(c)));
                    if (e != null) {
                        estimaciones.add(e);
                    }
                }
            });
            estimaciones.sort(alfabetico);
            encuestas.add(new Encuesta(encuestadora, fechas, fin, muestra(texto(fila.get(colMuestra))),
                    List.copyOf(estimaciones), enlace(doc, fila.get(colEmpresa))));
        }
        if (encuestas.isEmpty() || descartadas > encuestas.size()) {
            throw new IllegalStateException("No se han podido leer las filas de la tabla de sondeos (" + descartadas + " descartadas)");
        }
        encuestas.sort(Comparator.comparing(Encuesta::fin).reversed()); // estable: respeta el orden de la tabla
        return new Encuestas(partidos, List.copyOf(encuestas), fuente);
    }

    /** Primera tabla tras el encabezado cuyo id es el año (p. ej. {@code <h5 id="2026">}). */
    private static Element tablaDelAnio(Document doc, int anio) {
        Element encabezado = doc.getElementById(String.valueOf(anio));
        if (encabezado == null) {
            throw new IllegalStateException("No hay sección " + anio + " en la página de sondeos");
        }
        // MediaWiki envuelve los encabezados en <div class="mw-heading">: los hermanos son los de ese div.
        Element desde = encabezado.parent() != null && encabezado.parent().hasClass("mw-heading") ? encabezado.parent() : encabezado;
        for (Element e = desde.nextElementSibling(); e != null; e = e.nextElementSibling()) {
            if (e.hasClass("mw-heading") || e.nameIs("h2") || e.nameIs("h3") || e.nameIs("h4") || e.nameIs("h5")) {
                break;
            }
            Element tabla = e.is("table.wikitable") ? e : e.selectFirst("table.wikitable");
            if (tabla != null) {
                return tabla;
            }
        }
        throw new IllegalStateException("No hay tabla de sondeos bajo la sección " + anio);
    }

    /** Expande rowspan/colspan: cada fila tiene una celda por columna (la misma celda repetida si abarca varias). */
    private static List<List<Element>> rejilla(Element tabla) {
        List<List<Element>> filas = new ArrayList<>();
        Map<Integer, Element> arrastradas = new HashMap<>();
        Map<Integer, Integer> restantes = new HashMap<>();
        for (Element tr : tabla.select("> tbody > tr, > thead > tr")) {
            List<Element> fila = new ArrayList<>();
            var celdas = tr.children().stream().filter(c -> c.nameIs("td") || c.nameIs("th")).iterator();
            int col = 0;
            while (celdas.hasNext() || restantes.getOrDefault(col, 0) > 0) {
                if (restantes.getOrDefault(col, 0) > 0) {
                    fila.add(arrastradas.get(col));
                    restantes.merge(col, -1, Integer::sum);
                    col++;
                    continue;
                }
                Element celda = celdas.next();
                int colspan = entero(celda.attr("colspan"));
                int rowspan = entero(celda.attr("rowspan"));
                for (int i = 0; i < colspan; i++, col++) {
                    fila.add(celda);
                    if (rowspan > 1) {
                        arrastradas.put(col, celda);
                        restantes.put(col, rowspan - 1);
                    }
                }
            }
            filas.add(fila);
        }
        return filas;
    }

    private static int entero(String atributo) {
        try {
            return Math.max(1, Integer.parseInt(atributo.strip()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** Texto visible sin las llamadas a notas y referencias ("[ a ]", "[ 12 ]"). */
    private static String texto(Element celda) {
        Element copia = celda.clone();
        copia.select("sup").remove();
        return copia.text().strip();
    }

    /**
     * El nombre que muestra la tabla: el alt del logo o el texto del enlace ("SALF"); si solo hay un
     * logo sin alt, el título del enlace. Sin "(2021)" y similares.
     */
    private static String nombrePartido(Element th) {
        Element img = th.selectFirst("img[alt]");
        String nombre = img != null && !img.attr("alt").isBlank() ? img.attr("alt") : null;
        if (nombre == null && !texto(th).isEmpty()) {
            nombre = texto(th);
        }
        if (nombre == null) {
            Element a = th.selectFirst("a[title]");
            nombre = a != null ? a.attr("title") : "";
        }
        nombre = PARENTESIS_FINAL.matcher(nombre.strip()).replaceFirst("");
        if (nombre.isEmpty()) {
            throw new IllegalStateException("Columna de partido sin nombre en la tabla de sondeos");
        }
        return nombre;
    }

    /** Visible para tests. Último día del trabajo de campo; null si no se entiende. */
    static LocalDate finTrabajoDeCampo(String fechas, int anio) {
        Matcher m = DIA_MES.matcher(fechas);
        LocalDate fin = null;
        while (m.find()) {
            Integer mes = MESES.get(m.group(2).toLowerCase(Locale.ROOT));
            if (mes == null) {
                continue;
            }
            int a = m.group(3) != null ? Integer.parseInt(m.group(3)) : anio;
            try {
                fin = LocalDate.of(a, mes, Integer.parseInt(m.group(1)));
            } catch (java.time.DateTimeException e) {
                fin = null;
            }
        }
        return fin;
    }

    private static Integer muestra(String texto) {
        String digitos = texto.replace(",", "").strip();
        return digitos.matches("\\d{1,7}") ? Integer.parseInt(digitos) : null;
    }

    /** "34.1 146/148", "? 126", "25.5", "–". Null si la encuesta no da ningún dato del partido. */
    private static Estimacion estimacion(String partido, String texto) {
        String[] partes = texto.split("\\s+");
        Double voto = partes.length > 0 && VOTO.matcher(partes[0]).matches() ? Double.valueOf(partes[0]) : null;
        String escanos = partes.length > 1 && ESCANOS.matcher(partes[1]).matches() ? partes[1] : null;
        return voto == null && escanos == null ? null : new Estimacion(partido, voto, escanos);
    }

    /** Primera referencia de la celda de la empresa → enlace externo de esa referencia. */
    private static String enlace(Document doc, Element celda) {
        Element ref = celda.selectFirst("sup.reference a[href^=#cite_note]");
        if (ref == null) {
            return null;
        }
        Element nota = doc.getElementById(ref.attr("href").substring(1));
        Element externo = nota == null ? null : nota.selectFirst("a.external[href]");
        String url = externo == null ? null : externo.attr("href");
        return url != null && (url.startsWith("https://") || url.startsWith("http://")) ? url : null;
    }
}
