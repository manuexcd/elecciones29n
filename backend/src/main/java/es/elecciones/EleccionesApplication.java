package es.elecciones;

import es.elecciones.config.ElectionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ElectionProperties.class)
public class EleccionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(EleccionesApplication.class, args);
    }
}
