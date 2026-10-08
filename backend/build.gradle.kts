plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "es.elecciones"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // Spring Boot 4 separa RestClient (RestClient.Builder autoconfigurado, RestClientCustomizer) en su propio starter
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    // Genera /v3/api-docs (contrato OpenAPI) y /swagger-ui.html
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")   // @WebMvcTest
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test") // MockRestServiceServer
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

springBoot {
    buildInfo()
}

// `./gradlew test` NO llama a las fuentes reales.
tasks.test {
    useJUnitPlatform {
        excludeTags("contract")
    }
}

// `./gradlew contractTest` SÍ llama a las fuentes reales (se ejecuta en un workflow programado).
tasks.register<Test>("contractTest") {
    description = "Comprueba que las fuentes externas siguen devolviendo el formato esperado."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("contract")
    }
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}
