package es.elecciones.client;

import es.elecciones.config.ElectionProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

/** Lee el application.yml real, para que los tests de contrato comprueben la configuración desplegada. */
final class ConfiguracionReal {

    private ConfiguracionReal() {}

    static ElectionProperties cargar() {
        try {
            var fuentes = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
            return new Binder(ConfigurationPropertySources.from(fuentes))
                    .bind("elecciones", ElectionProperties.class)
                    .get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
