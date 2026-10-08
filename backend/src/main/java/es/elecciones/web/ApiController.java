package es.elecciones.web;

import es.elecciones.model.Calendario;
import es.elecciones.model.Composicion;
import es.elecciones.model.Partido;
import es.elecciones.model.Respuesta;
import es.elecciones.service.CalendarioService;
import es.elecciones.service.CongresoService;
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

    public ApiController(PartidoService partidos, CalendarioService calendario, CongresoService congreso) {
        this.partidos = partidos;
        this.calendario = calendario;
        this.congreso = congreso;
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

    @GetMapping("/calendario")
    public ResponseEntity<Calendario> calendario() {
        return ResponseEntity.ok().cacheControl(CDN).body(calendario.calendario());
    }
}
