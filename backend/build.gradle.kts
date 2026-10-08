plugins {
    java
    id("org.springframework.boot") version "3.5.0"
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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    // Genera /v3/api-docs (contrato OpenAPI) y /swagger-ui.html
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
