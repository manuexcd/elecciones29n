# Elecciones generales 29-N

Web de consulta con la información de los partidos políticos de cara a las elecciones generales del
**29 de noviembre de 2026** (convocadas por el Real Decreto 806/2026, BOE del 6 de octubre).

Sin base de datos: todo se obtiene en tiempo real de fuentes abiertas, con caché en memoria.

## Arquitectura

```
navegador ──► Caddy (:80/:443) ──► frontend (Next.js, SSR)  ──► backend (Spring Boot) ──► Wikidata
                  │                                                    ▲
                  └──────────────► /api/*  ────────────────────────────┘
```

- `backend/` — Java 21, Spring Boot 3.5, Gradle. Consulta las fuentes, las normaliza a un modelo
  común (`Partido`) y expone una API REST de solo lectura con contrato OpenAPI (`/v3/api-docs`).
- `frontend/` — Next.js (React) + TypeScript + Tailwind. Componentes de servidor, sin JS de cliente.
- `infra/` — Docker Compose y Caddyfile.
- `.github/workflows/` — CI y tests de contrato de las fuentes.

### Cómo evita depender de la fuente (`CachedSource`)

1. Primera petición: carga síncrona. Si falla → 503.
2. Dato caducado: se sirve el anterior y se refresca en segundo plano (*stale-while-revalidate*).
3. Si el refresco falla: se conserva el último dato bueno, se marca `desactualizado: true` y no se
   reintenta hasta pasado 1 minuto, para no martillear la fuente.

Cada respuesta incluye `actualizado` (cuándo se obtuvo con éxito), `desactualizado` y `fuente`, y la
interfaz los muestra.

## Puesta en marcha

```bash
# Backend (una sola vez, para generar el wrapper):
cd backend && gradle wrapper --gradle-version 8.14.3
./gradlew test          # tests unitarios, sin red
./gradlew bootRun       # http://localhost:8080  (Swagger UI en /swagger-ui.html)
./gradlew contractTest  # llama de verdad a Wikidata

# Frontend:
cd frontend && npm install
npm run gen:api         # con el backend en marcha: genera src/lib/api-types.ts
npm run dev             # http://localhost:3000

# Todo junto con Docker:
cd infra && docker compose up --build   # http://localhost
```

## Antes de publicar (pendiente)

- [ ] Cambiar `elecciones.user-agent` en `application.yml` (o la variable `USER_AGENT`): Wikimedia
      exige un User-Agent con contacto y puede bloquear los genéricos. Ojo: el Congreso responde 403
      si el User-Agent contiene «github».
- [ ] Verificar las fechas de `elecciones.hitos` contra el texto del BOE (RD 806/2026). Las marcadas
      «Prensa» salen de medios, y las «derivado» las he deducido de la LOREG.
- [ ] Verificar el art. 69.7 LOREG (veda de encuestas: 5 días antes, desde el 24 nov).
- [ ] El 28 de octubre, sustituir la lista provisional de Wikidata por las candidaturas proclamadas.
- [ ] Commitear `package-lock.json` y el wrapper de Gradle; pasar CI a `npm ci` / `./gradlew`.

## Fuentes

Implementadas: Wikidata (datos básicos de los partidos) y Open Data del Congreso (escaños y grupos
de la legislatura saliente, `GET /api/congreso/composicion`).

Pendientes:

| Dato | Fuente |
|---|---|
| Candidaturas proclamadas | BOE (28-oct) |
| Votaciones | Open Data del Congreso |
| Resultados históricos y noche electoral | Infoelectoral (Interior) |
| Encuestas | CIS, tablas públicas de sondeos |
| Programas | Enlace a la web de cada partido (no se copian) |

## Criterios de neutralidad

Orden alfabético, mismos campos para todos, cada dato con su fuente y fecha, y separación entre lo
que dice un partido de sí mismo y los datos objetivos.
