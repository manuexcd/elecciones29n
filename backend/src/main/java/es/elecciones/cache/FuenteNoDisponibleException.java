package es.elecciones.cache;

/** La fuente falló y no hay ningún dato anterior que servir. */
public class FuenteNoDisponibleException extends RuntimeException {

    public FuenteNoDisponibleException(String fuente, Throwable causa) {
        super("La fuente '" + fuente + "' no está disponible y no hay datos en caché", causa);
    }
}
