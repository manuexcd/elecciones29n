import { FuenteInfo } from "@/components/FuenteInfo";
import { Hemiciclo } from "@/components/Hemiciclo";
import { Tarjeta, TituloSeccion } from "@/components/ui";
import type { Composicion, Partido, Respuesta } from "@/lib/types";

/** Escaños de la legislatura saliente por partido (hemiciclo). */
export function ComposicionCongreso({ r, partidos }: { r: Respuesta<Composicion>; partidos: Partido[] | null }) {
  return (
    <Tarjeta etiqueta="congreso">
      <TituloSeccion
        id="congreso"
        subtitulo={`Reparto de escaños en el momento de la disolución de las Cortes (${r.datos.total} diputados).`}
      >
        Congreso saliente · XV legislatura
      </TituloSeccion>
      {partidos && <Hemiciclo partidos={partidos} />}
      <div className="mt-5">
        <FuenteInfo r={r} nombre="Datos abiertos del Congreso de los Diputados" />
      </div>
    </Tarjeta>
  );
}

/**
 * Escaños por grupo parlamentario, como barras. Orden alfabético (el que da el backend), no por
 * tamaño; todas las barras del mismo color neutro.
 */
export function GruposParlamentarios({ r }: { r: Respuesta<Composicion> }) {
  const max = Math.max(...r.datos.porGrupo.map((g) => g.escanos), 1);
  return (
    <Tarjeta etiqueta="grupos">
      <TituloSeccion id="grupos" subtitulo="Orden alfabético. Los grupos no coinciden siempre con los partidos: el Mixto reúne a varios.">
        Grupos parlamentarios
      </TituloSeccion>
      <ul className="grid gap-x-10 gap-y-3 md:grid-cols-2">
        {r.datos.porGrupo.map((g) => (
          <li key={g.nombre}>
            <div className="flex items-baseline justify-between gap-3 text-sm">
              <span className="truncate">{g.nombre.replace(/^Grupo Parlamentario /, "")}</span>
              <span className="font-semibold tabular-nums">{g.escanos}</span>
            </div>
            <div className="mt-1 h-1.5 rounded-full bg-stone-100 dark:bg-stone-800" aria-hidden>
              <div className="h-full rounded-full bg-stone-400 dark:bg-stone-500" style={{ width: `${(g.escanos / max) * 100}%` }} />
            </div>
          </li>
        ))}
      </ul>
    </Tarjeta>
  );
}
