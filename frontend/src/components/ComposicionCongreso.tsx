import { FuenteInfo } from "@/components/FuenteInfo";
import { Hemiciclo } from "@/components/Hemiciclo";
import type { Composicion, Partido, Respuesta } from "@/lib/types";

/** Escaños de la legislatura saliente: hemiciclo por partido y tabla por grupo parlamentario. */
export function ComposicionCongreso({ r, partidos }: { r: Respuesta<Composicion>; partidos: Partido[] | null }) {
  const c = r.datos;
  return (
    <section aria-labelledby="congreso">
      <h2 id="congreso" className="text-xl font-semibold">
        Congreso saliente (XV legislatura)
      </h2>
      <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
        Reparto de escaños en el momento de la disolución de las Cortes ({c.total} diputados).
      </p>
      {partidos && (
        <div className="mt-4">
          <Hemiciclo partidos={partidos} />
        </div>
      )}
      <h3 className="mt-8 font-medium">Por grupo parlamentario</h3>
      <table className="mt-3 w-full max-w-xl text-sm">
        <thead>
          <tr className="border-b border-slate-200 text-left dark:border-slate-800">
            <th scope="col" className="py-1 font-medium">
              Grupo parlamentario
            </th>
            <th scope="col" className="py-1 text-right font-medium">
              Escaños
            </th>
          </tr>
        </thead>
        <tbody>
          {c.porGrupo.map((g) => (
            <tr key={g.nombre} className="border-b border-slate-100 dark:border-slate-900">
              <td className="py-1">{g.nombre}</td>
              <td className="py-1 text-right tabular-nums">{g.escanos}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="mt-3">
        <FuenteInfo r={r} nombre="Datos abiertos del Congreso de los Diputados" />
      </div>
    </section>
  );
}
