/**
 * Tipos del contrato de la API, sacados de src/lib/api-types.ts (generado desde el OpenAPI del
 * backend con `npm run gen:api`, con el backend en marcha). Si cambias un record en Java, regenera
 * ese fichero: el frontend deja de compilar donde no cuadre.
 *
 * Los campos que pueden ser null se marcan en Java con `@Nullable` (JSpecify); el resto llegan
 * siempre rellenos (ver NulabilidadRecords en el backend).
 */
import type { components } from "./api-types";

type Schemas = components["schemas"];

export type Partido = Schemas["Partido"];
export type Escanos = Schemas["Escanos"];
export type Composicion = Schemas["Composicion"];
export type Calendario = Schemas["Calendario"];
export type Hito = Schemas["Hito"];
export type Encuestas = Schemas["Encuestas"];
export type Encuesta = Schemas["Encuesta"];

/**
 * springdoc genera un esquema por cada uso del genérico (RespuestaPartido, RespuestaComposicion...).
 * Este alias recupera el genérico para los componentes; `api.ts` usa los esquemas concretos, y si
 * alguno deja de encajar con el genérico, falla la compilación donde se pasan a los componentes.
 */
export type Respuesta<T> = Omit<Schemas["RespuestaPartido"], "datos"> & { datos: T };
