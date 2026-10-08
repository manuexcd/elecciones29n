package es.elecciones.web;

import es.elecciones.cache.FuenteNoDisponibleException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    /** Fuente caída y sin dato previo: 503 en lugar de un 500 genérico. */
    @ExceptionHandler(FuenteNoDisponibleException.class)
    ProblemDetail fuenteNoDisponible(FuenteNoDisponibleException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        problem.setTitle("Fuente de datos no disponible");
        return problem;
    }

    /** 451 Unavailable For Legal Reasons, y que nadie lo guarde: al acabar la veda debe poder cambiar. */
    @ExceptionHandler(VedaEncuestasException.class)
    ResponseEntity<ProblemDetail> vedaEncuestas() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS,
                "La ley electoral (LOREG art. 69.7) prohíbe publicar encuestas los cinco días anteriores a la votación.");
        problem.setTitle("Periodo de veda de encuestas");
        return ResponseEntity.status(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS).cacheControl(CacheControl.noStore()).body(problem);
    }
}
