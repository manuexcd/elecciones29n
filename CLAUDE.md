# CLAUDE.md

Contexto del proyecto para retomar el trabajo. Idioma de trabajo y de la interfaz: **español**.

## Qué es

Web personal que muestra la información relevante de los partidos políticos de cara a las **elecciones
generales del domingo 29 de noviembre de 2026** (convocadas por el Real Decreto 806/2026, BOE del 6 de
octubre de 2026).

Requisitos fijados por el usuario:
- **Sin base de datos.** Todo se recupera en tiempo real de fuentes consultables (APIs, ficheros
  abiertos, scraping como último recurso). Solo caché en memoria.
- El usuario tiene mucha experiencia en **Java** y **devops**; no le importa usar tecnología moderna
  en el frontend.

## Arquitectura

```
navegador ─► Caddy ─► frontend (Next.js SSR) ─► backend (Spring Boot) ─► fuentes externas
                └──► /api/*  ────────────────────────┘
```

- `backend/` — Java 21, Spring Boot 4.1, Gradle 9 (wrapper, Kotlin DSL), Jackson 3 (`tools.jackson`). API REST de solo lectura + OpenAPI
  (`/v3/api-docs`, `/swagger-ui.html`) + Prometheus (`/actuator/prometheus`).
- `frontend/` — Next.js 15 + TypeScript + Tailwind v4. Solo componentes de servidor; las llamadas al
  backend son server-side (sin CORS).
- `infra/` — `docker-compose.yml` y `Caddyfile`. Cloudflare delante en producción (plan gratuito).
- `.github/workflows/` — `ci.yml` (tests, typecheck, build, imágenes) y `contract.yml` (tests de
  contrato contra las fuentes reales, programado).

Piezas clave del backend:
- `client/` — un cliente por fuente: `WikidataClient` (SPARQL), `CongresoClient` (Open Data del
  Congreso → `model/Composicion`, servido en `/api/congreso/composicion`) y `EncuestasClient`
  (tabla de sondeos de Wikipedia con Jsoup → `model/Encuestas`, en `/api/encuestas`). Devuelven
  datos ya normalizados.
- `model/Partido` — modelo común. La interfaz solo conoce este record. `PartidoService` lo construye
  cruzando fuentes: **qué** partidos aparecen lo decide el Congreso (formaciones con escaños);
  Wikidata solo aporta la ficha (nombre, logo, web, fundación) mediante la tabla
  `elecciones.formaciones` (siglas del Congreso → QID) de `application.yml`. Si Wikidata cae, la
  lista se sirve igual (sin logos) marcada como desactualizada.
- `cache/CachedSource` — caché en memoria con stale-while-revalidate y stale-if-error. Primera carga
  síncrona (si falla → `FuenteNoDisponibleException` → HTTP 503); después sirve el dato anterior y
  refresca en segundo plano; si el refresco falla, conserva el último dato bueno, lo marca
  `desactualizado` y no reintenta hasta pasado `retryDelay` (1 min).
- `model/Respuesta<T>` — envoltorio de todo dato externo: `datos`, `actualizado`, `desactualizado`,
  `fuente`. La UI siempre muestra procedencia y fecha.
- `ApiController` — envía `Cache-Control` pensado para la CDN (s-maxage, stale-while-revalidate,
  stale-if-error).

## Versiones (migrado el 8-oct-2026)

Spring Boot 3.5 dejó de tener soporte el 30-jun-2026; se migró a **Spring Boot 4.1.1** (soporte hasta
jul-2027) y **Gradle 9.8.1**. Cosas de Boot 4 que afectan al código:
- Starters: `spring-boot-starter-webmvc` (antes `-web`), `spring-boot-starter-restclient` (sin él no
  hay `RestClient.Builder`); en test, `-webmvc-test` y `-restclient-test`.
- Paquetes nuevos: `org.springframework.boot.restclient.RestClientCustomizer`,
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`.
- Jackson 3: `tools.jackson.databind.*`, `asString()`/`isString()` en vez de `asText()`/`isTextual()`.
  Jackson 2 sigue en el classpath solo porque lo arrastra springdoc/swagger: no usarlo.
- springdoc 3.1.x (compilado contra Boot 4.1); el OpenAPI generado pasa a 3.1.0.
- El Dockerfile del backend compila con `./gradlew` sobre `eclipse-temurin:21-jdk` (misma versión
  de Gradle en local, CI y Docker; la imagen oficial `gradle` va por detrás).

## Convenciones

- **Neutralidad (importante):** orden alfabético, mismos campos para todos los partidos, cada dato con
  su fuente y fecha, y separar lo que dice un partido de sí mismo (programa) de los datos objetivos
  (escaños, votaciones). No ordenar por tamaño ni destacar partidos.
- Programas electorales y noticias: **enlazar, no copiar** (derechos de autor).
- Todo dato externo editable por terceros (p. ej. Wikidata) se trata como no fiable: solo se aceptan
  URLs `http(s)` (ver `WikidataClient.soloHttp`) y los ids se validan antes de usarse
  (`/^Q\d+$/` en la ficha).
- Una fuente nueva = un cliente + un test unitario con respuesta simulada + un test de contrato
  (`@Tag("contract")`, excluido de `gradle test`, se lanza con `gradle contractTest`).
- Cada fuente expone dos métricas Micrometer: última actualización correcta y fallos consecutivos.
- Los tests unitarios nunca llaman a la red.
- Tipos del frontend generados desde el OpenAPI: `npm run gen:api` (backend en marcha) escribe
  `src/lib/api-types.ts`, que **se commitea**; `src/lib/types.ts` solo tiene alias. El job `api-types`
  del CI falla si el fichero no coincide con lo que genera el backend. Al cambiar un record: regenerar.
- Nulabilidad en los records de `model/`: `@Nullable` de JSpecify en los componentes que pueden ser
  null; el resto se consideran no nulos. `config/NulabilidadRecords` (un `ModelConverter` de
  swagger) marca en el OpenAPI todos los componentes como `required` (Jackson serializa también los
  null) y los `@Nullable` como `type: [X, "null"]`.
- Variables de entorno de Spring con *relaxed binding*: `elecciones.user-agent` →
  `ELECCIONES_USERAGENT` (sin guion bajo entre palabras).

## Comandos

```bash
# Backend
cd backend
./gradlew test            # unitarios, sin red
./gradlew contractTest    # llama de verdad a Wikidata
./gradlew bootRun         # http://localhost:8080

# Frontend
cd frontend
npm install
npm run gen:api           # con el backend en marcha: genera src/lib/api-types.ts
npm run dev               # http://localhost:3000
npm run typecheck && npm run build

# Todo junto
cd infra && docker compose up --build   # http://localhost
```

## Estado actual

El esqueleto se creó en una sesión en la nube **sin acceso a Maven ni a npm**, así que en esa sesión
solo se verificó: la lógica de `CachedSource` (arnés con 16 comprobaciones), la sintaxis del Java y
la validez de los YAML/JSON. Después el usuario ejecutó en local `gradle wrapper` + `./gradlew test`
y `npm install` y **fueron bien**.

**Sesión del 7-oct-2026 (en local, con red):**
- La consulta SPARQL de `WikidataClient` **no sirve como lista de partidos**: devuelve ~960
  entidades (partidos municipales, juventudes, históricos sin fecha de disolución, muchas sin
  etiqueta). Filtrar por escaños en el Congreso (`P1410` + cualificador `P194 = Q539149`, mejor
  rango) baja a 17, pero **faltan PSOE y Sumar** por cómo están modelados. Conclusión: Wikidata solo
  vale para enriquecer fichas (logo, web, fundación), no para decidir qué partidos aparecen.
- Implementado `CongresoClient` + `CongresoService` + `GET /api/congreso/composicion`, con test
  unitario y de contrato (ambos en verde). Da 350 escaños que cuadran con el 23-J.
- Detalles de la fuente del Congreso:
  - Los ficheros llevan fecha en el nombre (`DiputadosActivos__20261007050007.json`); el cliente lee
    la página índice `/es/opendata/diputados` para encontrar los enlaces.
  - Tras la disolución, `DiputadosActivos` solo tiene 137 (Diputación Permanente); el resto está en
    `DiputadosDeBaja` con `FECHABAJA = 06/10/2026` (`elecciones.congreso.fecha-disolucion`). Si la
    suma no da 350 el cliente lanza excepción (se sirve el último dato bueno).
  - `FORMACIONELECTORAL` es la candidatura con la que se presentaron (PSC-PSOE, PsdeG-PSOE... salen
    aparte del PSOE). No se agrupan: sería una decisión editorial.
  - El histórico `Diput__*.json` no incluye la formación.
  - **El WAF del Congreso devuelve 403 si el User-Agent contiene «github»**. Tenerlo en cuenta al
    poner el User-Agent definitivo (vale una URL propia o un email).
- Decidido con el usuario: hasta el 28-oct la home muestra las 16 formaciones con escaños en la
  legislatura saliente, enriquecidas con Wikidata. Hecho: `WikidataClient.partidos(qids)` (consulta
  con `VALUES`, ya no la genérica), tabla `elecciones.formaciones` (16 QID revisados a mano),
  `Partido.escanos`, frontend con escaños en tarjetas y ficha y tabla por grupo parlamentario
  (`ComposicionCongreso`). Los tests de contrato leen el `application.yml` real y fallan si una
  formación del Congreso no tiene QID o si un QID deja de existir en Wikidata. Verificado:
  21 tests unitarios, 2 de contrato, `npm run typecheck`, `npm run build` y la home renderizada con
  datos reales.
- Decidido con el usuario: **el PSOE y sus federaciones (PSC, PSdeG, PSE-EE, PSIB, PSN) se
  muestran como un solo partido (121 escaños)**. Se hace en configuración: las federaciones apuntan
  al QID del PSOE y `PartidoService.combinar` agrupa por QID; el desglose queda en
  `Partido.candidaturas` y se muestra en la ficha. `/api/congreso/composicion` sigue dando el dato
  bruto por candidatura.
- Hemiciclo (`frontend/src/components/Hemiciclo.tsx`): SVG de servidor, un punto por escaño, partidos
  en orden alfabético de izquierda a derecha (no ideológico), colores de Wikidata P465 (validados
  como hex en `WikidataClient`). Esos colores NO pasan el validador de paleta (EH Bildu/Junts casi
  iguales, amarillos con poco contraste): se compensa con leyenda con nombre y escaños y `<title>`
  por partido al pasar el ratón. Revisado con capturas de Chrome headless en escritorio y móvil.

**Sesión del 8-oct-2026:**
- `docker compose up --build` probado por el usuario: funciona.
- **Encuestas** (decidido con el usuario: fuente Wikipedia en inglés, «Opinion polling for the 2026
  Spanish general election»; la española no tiene artículo de sondeos para 2026):
  - Se pide la página entera (`action=parse`, ~1,6 MB, TTL 1 h) porque las referencias (enlace a
    la publicación original de cada encuesta) no salen al renderizar solo la sección. Se lee la
    primera tabla bajo el encabezado con id = `elecciones.encuestas.anio`; se expanden
    rowspan/colspan (las filas del CIS comparten fecha y muestra). Fuente = enlace `oldid` a la
    revisión leída.
  - Neutralidad: partidos en orden alfabético, sin columna «Lead» ni sombreado del ganador, todas
    las encuestas sin filtrar. Si la cabecera cambia, excepción → se sirve el último dato bueno.
  - Veda: `CalendarioService.encuestasPublicables()` es el único que decide. Durante la veda la
    API responde **451** con `no-store` sin consultar la caché; antes, `ApiController.hastaLaVeda`
    limita el Cache-Control para que s-maxage + stale-* no pasen de las 00:00 del día de veda.
    La página `/encuestas` sale con `no-store` en Caddy, pide los datos sin caché de Next y
    comprueba también la fecha local (si el backend falla, usa 2026-11-24: ante la duda, no publica).
  - Verificado: test de contrato contra Wikipedia (142 encuestas, ninguna descartada), unitarios,
    y la veda de extremo a extremo con `ELECCIONES_VEDAENCUESTASDESDE=2026-10-01` (451 en la API y
    aviso en la página). La regla nueva del Caddyfile no se ha probado con `docker compose`.
- OpenAPI con campos obligatorios y nulables (ver Convenciones) y frontend con tipos generados.
  Verificado: tests unitarios, `npm run typecheck`, `npm run build`, y que el typecheck falla si se
  usa sin comprobar un campo nulable. El job `api-types` del CI aún no se ha ejecutado en GitHub.

## Pendiente antes de publicar

- [ ] Cambiar `elecciones.user-agent` en `application.yml` (o la variable `USER_AGENT`): Wikimedia
      exige un User-Agent que identifique el proyecto y un contacto.
- [ ] Verificar las fechas de `elecciones.hitos` (en `application.yml`) contra el texto del BOE
      (RD 806/2026). Las marcadas «Prensa» salen de medios; las «derivado» (fin de campaña 27-nov y
      jornada de reflexión 28-nov) las dedujo Claude de la LOREG, no se leyeron del decreto.
- [ ] Verificar el art. 69.7 LOREG: veda de encuestas los 5 días previos a la votación
      (`veda-encuestas-desde: 2026-11-24`). A partir de esa fecha la web debe ocultar encuestas y
      cualquier contenido de campaña el día de reflexión.
- [x] Commitear `package-lock.json` y el wrapper de Gradle; pasar el CI a `npm ci` / `./gradlew`
      (y activar `cache: npm` en `setup-node`).
- [ ] Tras el primer push: comprobar que `contract.yml` funciona desde GitHub (el CDN del Congreso,
      Akamai, podría bloquear las IP de los runners).
- [ ] En `contract.yml`, subir la frecuencia a cada hora durante la campaña (13–27 nov).

## Fechas clave (de la prensa que cita el RD 806/2026; verificar en BOE)

| Fecha | Hito |
|---|---|
| 6 oct | BOE: disolución de las Cortes y convocatoria |
| 26 oct | Fin del plazo de presentación de candidaturas |
| **28 oct** | **Publicación de candidaturas** → sustituir la lista provisional de Wikidata |
| 13 nov | Comienza la campaña |
| 24 nov | Empieza la veda de encuestas (verificar) |
| 25 nov | Fecha límite ordinaria del voto por correo |
| **29 nov** | **Elecciones generales** |
| 24 dic | Fecha límite constitucional para reunirse el nuevo Congreso |

## Próximos pasos (orden recomendado)

1. **Noticias** (decidido: RSS de Europa Press, canal 00066, y RTVE `rss/temas_politica.xml`;
   ambos comprobados el 8-oct; EFE da 403). Titular + enlace + fecha, sin copiar texto.
2. **Programas** (decidido: URLs a mano en `application.yml`, una por partido, que el usuario rellena
   cuando se publiquen; «No publicado todavía» mientras tanto; comprobar que el enlace responde).
3. **Conector del BOE** para las candidaturas proclamadas (a partir del 28 de octubre). Hasta
   entonces la home muestra un aviso de «lista provisional» (se oculta sola después de esa fecha).
4. Resultados de la noche electoral si Interior (Infoelectoral) ofrece datos consumibles.
5. Añadir Caffeine/Resilience4j **solo cuando hagan falta** (Jsoup ya está, para Wikipedia).

## Fuentes previstas

| Dato | Fuente |
|---|---|
| Datos básicos, logo, web, fundación | Wikidata (SPARQL) — **implementado** |
| Candidaturas proclamadas | BOE (API de datos abiertos) |
| Escaños y grupos de la legislatura saliente | Open Data del Congreso — **implementado** |
| Votaciones, Senado | Open Data del Congreso / Senado |
| Resultados históricos y noche electoral | Infoelectoral (Ministerio del Interior) |
| Partidos registrados | Registro de Partidos Políticos (Interior) |
| Encuestas | Wikipedia (en), tabla de sondeos — **implementado** |
| Noticias | RSS de Europa Press y RTVE |

Salvo las marcadas como implementadas, los endpoints concretos **no se han comprobado**: validar cada URL y formato antes
de implementar el cliente.
