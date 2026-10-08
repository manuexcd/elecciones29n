package es.elecciones.web;

import es.elecciones.cache.FuenteNoDisponibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
}
