package es.elecciones.web;

/** Se ha pedido una encuesta durante la veda electoral. Se responde 451 (ver {@link ApiExceptionHandler}). */
class VedaEncuestasException extends RuntimeException {

    VedaEncuestasException() {
        super("Veda de encuestas", null, false, false);
    }
}
