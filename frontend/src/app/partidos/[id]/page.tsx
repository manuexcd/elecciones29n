import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { FuenteInfo } from "@/components/FuenteInfo";
import { obtenerPartido } from "@/lib/api";

export const dynamic = "force-dynamic";

type Props = { params: Promise<{ id: string }> };

// Los ids son QIDs de Wikidata. Rechazar cualquier otra cosa antes de llamar al backend.
const ID_VALIDO = /^Q\d+$/;

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

  return (
    <article className="space-y-6">
      <a href="/" className="text-sm underline">
        ← Todos los partidos
      </a>

      <div className="flex items-center gap-4">
        {p.logo && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={p.logo} alt={`Logo de ${p.nombre}`} width={80} height={80} className="h-20 w-20 object-contain" />
        )}
        <div>
          <h1 className="text-3xl font-bold">{p.nombre}</h1>
          {p.siglas && <p className="text-slate-600 dark:text-slate-400">{p.siglas}</p>}
        </div>
      </div>

      <dl className="grid grid-cols-[10rem_1fr] gap-y-2">
        <dt className="font-medium">Escaños en el Congreso saliente</dt>
        <dd>
          {p.escanos ?? "No consta"}
          {p.candidaturas && p.candidaturas.length > 1 && (
            <span className="block text-sm text-slate-600 dark:text-slate-400">
              {p.candidaturas.map((c) => `${c.nombre}: ${c.escanos}`).join(" · ")}
            </span>
          )}
        </dd>

        <dt className="font-medium">Año de fundación</dt>
        <dd>{p.fundacion ?? "No consta"}</dd>

        <dt className="font-medium">Web oficial</dt>
        <dd>
          {p.web ? (
            <a href={p.web} target="_blank" rel="noopener noreferrer nofollow" className="underline break-all">
              {p.web}
            </a>
          ) : (
            "No consta"
          )}
        </dd>

        <dt className="font-medium">Ficha de origen</dt>
        <dd>
          {p.fuente ? (
            <a href={p.fuente} target="_blank" rel="noopener noreferrer" className="underline break-all">
              {p.fuente}
            </a>
          ) : (
            "No consta"
          )}
        </dd>
      </dl>

      <FuenteInfo r={r} nombre="Congreso de los Diputados (escaños) y Wikidata (ficha)" />
    </article>
  );
}
