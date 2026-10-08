import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { FuenteInfo } from "@/components/FuenteInfo";
import { BotonEnlace, Cifra, IconoFlecha, Pastilla, Tarjeta, TituloSeccion } from "@/components/ui";
import { obtenerPartido } from "@/lib/api";

export const dynamic = "force-dynamic";

type Props = { params: Promise<{ id: string }> };

// Los ids son QIDs de Wikidata. Rechazar cualquier otra cosa antes de llamar al backend.
const ID_VALIDO = /^Q\d+$/;
const ESCANOS_CONGRESO = 350;
const SIN_COLOR = "#D6D3D1";
/** "https://www.psoe.es/x" → "psoe.es". La URL viene de Wikidata: si no se puede leer, null. */
function dominio(url: string | null): string | null {
  if (!url) return null;
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return null;
  }
}

const fmtPorcentaje = new Intl.NumberFormat("es-ES", { style: "percent", maximumFractionDigits: 1 });

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  if (!ID_VALIDO.test(id)) return { title: "Partido no encontrado" };
  const r = await obtenerPartido(id);
  return { title: r ? `${r.datos.nombre} · Elecciones generales 29-N` : "Partido no encontrado" };
}

export default async function FichaPartido({ params }: Props) {
  const { id } = await params;
  if (!ID_VALIDO.test(id)) notFound();

  const r = await obtenerPartido(id);
  if (!r) notFound();
  const p = r.datos;

  const anios = p.fundacion != null ? new Date().getFullYear() - p.fundacion : null;
  const candidaturas = p.candidaturas ?? [];
  const color = p.color ?? SIN_COLOR;

  return (
    <article className="space-y-6">
      <a
        href="/"
        className="inline-flex items-center gap-1.5 text-sm font-medium text-stone-600 hover:text-stone-900 dark:text-stone-400 dark:hover:text-white"
      >
        <IconoFlecha className="h-4 w-4 rotate-180" />
        Todos los partidos
      </a>

      <div className="relative overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-stone-200/70 dark:bg-stone-900 dark:ring-stone-800">
        {/* Franja con el color del partido (Wikidata), igual en todas las fichas. */}
        <div className="h-2" style={{ backgroundColor: color }} aria-hidden />
        <div className="flex flex-col gap-6 p-6 sm:flex-row sm:items-center sm:p-8">
          <div className="grid h-24 w-24 shrink-0 place-items-center rounded-2xl bg-white p-3 ring-1 ring-stone-200 dark:ring-stone-700">
            {p.logo ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={p.logo} alt={`Logo de ${p.nombre}`} width={80} height={80} className="max-h-full max-w-full object-contain" />
            ) : (
              <span className="text-lg font-bold text-stone-400">{p.siglas?.slice(0, 5)}</span>
            )}
          </div>
          <div className="min-w-0 flex-1">
            {p.siglas && <Pastilla>{p.siglas}</Pastilla>}
            <h1 className="mt-2 text-2xl font-bold tracking-tight sm:text-3xl">{p.nombre}</h1>
            <p className="mt-1 text-sm text-stone-500 dark:text-stone-400">
              Formación con escaños en el Congreso al disolverse las Cortes
            </p>
          </div>
          <div className="flex flex-wrap gap-2 sm:flex-col">
            {p.web && (
              <BotonEnlace href={p.web} rel="noopener noreferrer nofollow">
                Web oficial
              </BotonEnlace>
            )}
            {p.fuente && <BotonEnlace href={p.fuente}>Ficha en Wikidata</BotonEnlace>}
          </div>
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <Cifra
          etiqueta="Escaños en el Congreso saliente"
          valor={p.escanos ?? "–"}
          detalle={p.escanos != null ? `${fmtPorcentaje.format(p.escanos / ESCANOS_CONGRESO)} de la cámara` : "No consta"}
        />
        <Cifra
          etiqueta="Año de fundación"
          valor={p.fundacion ?? "–"}
          detalle={anios != null ? `Hace ${anios} ${anios === 1 ? "año" : "años"}` : "No consta"}
        />
        <Cifra
          etiqueta="Web oficial"
          valor={<span className="block truncate text-lg">{dominio(p.web) ?? "No consta"}</span>}
          detalle="Según Wikidata"
        />
      </div>

      {candidaturas.length > 1 && (
        <Tarjeta etiqueta="candidaturas">
          <TituloSeccion
            id="candidaturas"
            subtitulo="Se presentó con varias candidaturas (federaciones o marcas territoriales); aquí se suman."
          >
            Escaños por candidatura
          </TituloSeccion>
          <ul className="space-y-3">
            {candidaturas.map((c) => (
              <li key={c.nombre}>
                <div className="flex items-baseline justify-between gap-3 text-sm">
                  <span>{c.nombre}</span>
                  <span className="font-semibold tabular-nums">{c.escanos}</span>
                </div>
                <div className="mt-1 h-1.5 rounded-full bg-stone-100 dark:bg-stone-800" aria-hidden>
                  <div
                    className="h-full rounded-full"
                    style={{ width: `${(c.escanos / Math.max(1, p.escanos ?? 1)) * 100}%`, backgroundColor: color }}
                  />
                </div>
              </li>
            ))}
          </ul>
        </Tarjeta>
      )}

      <FuenteInfo r={r} nombre="Congreso de los Diputados (escaños) y Wikidata (ficha)" />
    </article>
  );
}
