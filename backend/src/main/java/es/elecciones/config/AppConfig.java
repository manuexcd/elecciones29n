package es.elecciones.config;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class AppConfig {

    public static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

    /** Reloj inyectable: los tests lo sustituyen para simular fechas de campaña. */
    @Bean
    Clock clock() {
        return Clock.system(ZONA);
    }

    /** Timeouts para todas las llamadas salientes. Respeta el proxy del sistema si existe. */
    @Bean
    RestClientCustomizer timeouts() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .proxy(ProxySelector.getDefault())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(20));
        return builder -> builder.requestFactory(factory);
    }
}
