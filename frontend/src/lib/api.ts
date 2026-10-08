import type { components } from "./api-types";

type Schemas = components["schemas"];

// Solo se usa en el servidor (componentes de servidor), así que no hay CORS ni se expone al navegador.
const API_URL = process.env.API_URL ?? "http://localhost:8080";

class NotFoundError extends Error {}

async function get<T>(path: string, cache: RequestInit = { next: { revalidate: 60 } }): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    ...cache,
    signal: AbortSignal.timeout(10_000),
  });
  if (res.status === 404) throw new NotFoundError(path);
  if (!res.ok) throw new Error(`API ${res.status} en ${path}`);
  return (await res.json()) as T;
}

/** Devuelve null si la API no responde: la página decide cómo degradar en lugar de romperse. */
async function orNull<T>(p: Promise<T>): Promise<T | null> {
  try {
    return await p;
  } catch (e) {
    if (e instanceof NotFoundError) return null;
    console.error(e);
    return null;
  }
}

export const listarPartidos = () => orNull(get<Schemas["RespuestaListPartido"]>("/api/partidos"));

export const obtenerPartido = (id: string) =>
  orNull(get<Schemas["RespuestaPartido"]>(`/api/partidos/${encodeURIComponent(id)}`));

export const obtenerCalendario = () => orNull(get<Schemas["Calendario"]>("/api/calendario"));

export const obtenerComposicion = () =>
  orNull(get<Schemas["RespuestaComposicion"]>("/api/congreso/composicion"));

// Sin caché de Next: un dato guardado podría servirse ya dentro de la veda. El backend tiene su
// propia caché en memoria, así que la llamada es barata. Durante la veda responde 451 → null.
export const obtenerEncuestas = () =>
  orNull(get<Schemas["RespuestaEncuestas"]>("/api/encuestas", { cache: "no-store" }));
