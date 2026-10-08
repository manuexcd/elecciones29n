/**
 * Tipos del contrato de la API.
 *
 * Hoy están escritos a mano para que el proyecto compile sin el backend en marcha. Cuando lo
 * tengas levantado, genera los tipos reales con `npm run gen:api` (crea src/lib/api-types.ts desde
 * /v3/api-docs) y sustituye este fichero por alias, por ejemplo:
 *
 *   import type { components } from "./api-types";
 *   export type Partido = components["schemas"]["Partido"];
 *
 * A partir de ahí, si cambias un record en Java, el frontend deja de compilar.
 *
 * Ojo: springdoc genera hoy TODOS los campos como opcionales (`id?: string`). Antes de cambiar,
 * hay que marcar en Java los obligatorios (p. ej. `@Schema(requiredMode = REQUIRED)` o un
 * PropertyCustomizer que lo haga para los componentes de los records).
 */

export interface Partido {
  id: string | null; // QID de Wikidata; null si la formación aún no tiene ficha asociada
  nombre: string;
  siglas: string | null;
  web: string | null;
  logo: string | null;
  fundacion: number | null;
  color: string | null; // "#RRGGBB" (Wikidata P465, validado en el backend)
  escanos: number | null; // en el Congreso, al final de la legislatura saliente
  candidaturas: Escanos[]; // desglose de escanos (p. ej. PSOE + sus federaciones)
  fuente: string | null; // URL de la ficha de Wikidata
}

export interface Escanos {
  nombre: string;
  escanos: number;
}

export interface Composicion {
  porFormacion: Escanos[];
  porGrupo: Escanos[];
  total: number;
  fuente: string;
}

export interface Respuesta<T> {
  datos: T;
  actualizado: string; // ISO-8601
  desactualizado: boolean;
  fuente: string;
}

export interface Hito {
  fecha: string; // yyyy-MM-dd
  titulo: string;
  fuente: string;
}

export interface Calendario {
  fechaElecciones: string;
  hitos: Hito[];
  vedaEncuestasDesde: string;
  encuestasPublicables: boolean;
}
