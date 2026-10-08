import type { Calendario, Composicion, Partido, Respuesta } from "./types";

// Solo se usa en el servidor (componentes de servidor), así que no hay CORS ni se expone al navegador.
const API_URL = process.env.API_URL ?? "http://localhost:8080";

class NotFoundError extends Error {}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    next: { revalidate: 60 },
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

export const listarPartidos = () => orNull(get<Respuesta<Partido[]>>("/api/partidos"));

export const obtenerPartido = (id: string) =>
  orNull(get<Respuesta<Partido>>(`/api/partidos/${encodeURIComponent(id)}`));

export const obtenerCalendario = () => orNull(get<Calendario>("/api/calendario"));

export const obtenerComposicion = () =>
  orNull(get<Respuesta<Composicion>>("/api/congreso/composicion"));
