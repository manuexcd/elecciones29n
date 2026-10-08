package es.elecciones.web;

import es.elecciones.model.Calendario;
import es.elecciones.model.Composicion;
import es.elecciones.model.Encuestas;
import es.elecciones.model.Partido;
import es.elecciones.model.Respuesta;
import es.elecciones.service.CalendarioService;
import es.elecciones.service.CongresoService;
import es.elecciones.service.EncuestasService;
import es.elecciones.service.PartidoService;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {

    /**
     * Cabeceras pensadas para la CDN (Cloudflare): el navegador cachea 1 min, la CDN 5 min y puede
     * servir contenido caducado hasta 1 h mientras revalida, o 1 día si el origen está caído.
     */
    private static final CacheControl CDN = CacheControl.maxAge(Duration.ofMinutes(1))
            .cachePublic()
            .sMaxAge(Duration.ofMinutes(5))
            .staleWhileRevalidate(Duration.ofHours(1))
            .staleIfError(Duration.ofDays(1));

    private final PartidoService partidos;
    private final CalendarioService calendario;
    private final CongresoService congreso;
    private final EncuestasService encuestas;

    public ApiController(PartidoService partidos, CalendarioService calendario, CongresoService congreso,
                         EncuestasService encuestas) {
        this.partidos = partidos;
        this.calendario = calendario;
        this.congreso = congreso;
        this.encuestas = encuestas;
    }

    @GetMapping("/partidos")
    public ResponseEntity<Respuesta<List<Partido>>> partidos() {
        return ResponseEntity.ok().cacheControl(CDN).body(partidos.partidos());
    }

    @GetMapping("/partidos/{id}")
    public ResponseEntity<Respuesta<Partido>> partido(@PathVariable String id) {
        Respuesta<Partido> respuesta = partidos.partido(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partido no encontrado"));
        return ResponseEntity.ok().cacheControl(CDN).body(respuesta);
    }

    /** Reparto de escaños de la legislatura saliente (XV), según el Open Data del Congreso. */
    @GetMapping("/congreso/composicion")
    public ResponseEntity<Respuesta<Composicion>> composicion() {
        return ResponseEntity.ok().cacheControl(CDN).body(congreso.composicion());
    }

    /**
     * Sondeos publicados (Wikipedia). Durante la veda (LOREG art. 69.7) responde 451 sin cuerpo de
     * datos, y antes de ella ningún caché puede guardar la respuesta más allá del inicio de la veda.
     */
    @GetMapping("/encuestas")
    public ResponseEntity<Respuesta<Encuestas>> encuestas() {
        Respuesta<Encuestas> respuesta = encuestas.encuestas().orElseThrow(VedaEncuestasException::new);
        return ResponseEntity.ok().cacheControl(hastaLaVeda(calendario.hastaLaVeda())).body(respuesta);
    }

    /**
     * Como {@link #CDN}, pero sin que ningún caché pueda servir la respuesta después de {@code restante}:
     * a lo sumo s-maxage + stale-* desde que se generó.
     */
    static CacheControl hastaLaVeda(Duration restante) {
        Duration sMaxAge = min(Duration.ofMinutes(5), restante);
        Duration margen = restante.minus(sMaxAge);
        if (sMaxAge.toSeconds() <= 0) {
            return CacheControl.noStore();
        }
        CacheControl cc = CacheControl.maxAge(min(Duration.ofMinutes(1), sMaxAge)).cachePublic().sMaxAge(sMaxAge);
        return margen.toSeconds() > 0
                ? cc.staleWhileRevalidate(min(Duration.ofHours(1), margen)).staleIfError(min(Duration.ofDays(1), margen))
                : cc;
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    @GetMapping("/calendario")
    public ResponseEntity<Calendario> calendario() {
        return ResponseEntity.ok().cacheControl(CDN).body(calendario.calendario());
    }
}
