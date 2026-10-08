import type { Metadata } from "next";
import { FuenteInfo } from "@/components/FuenteInfo";
import { obtenerCalendario, obtenerEncuestas } from "@/lib/api";
import type { Encuesta } from "@/lib/types";

// Nunca en caché (ni aquí ni en Caddy/Cloudflare, ver Caddyfile): al empezar la veda debe dejar de
// mostrar encuestas en el acto.
export const dynamic = "force-dynamic";

export const metadata: Metadata = { title: "Encuestas · Elecciones generales 29-N" };

// Si el backend no responde, se usa esta fecha: ante la duda, no se publica.
const VEDA_POR_DEFECTO = "2026-11-24";
const MAX_FILAS = 30;

const fmtDia = new Intl.DateTimeFormat("es-ES", { day: "numeric", month: "short", timeZone: "UTC" });
const fmtLargo = new Intl.DateTimeFormat("es-ES", { day: "numeric", month: "long", timeZone: "UTC" });
const fmtVoto = new Intl.NumberFormat("es-ES", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const fmtEntero = new Intl.NumberFormat("es-ES");
const dia = (iso: string) => new Date(`${iso}T12:00:00Z`);

function Celda({ e, partido }: { e: Encuesta; partido: string }) {
  const x = e.estimaciones.find((est) => est.partido === partido);
  if (!x) return <td className="px-2 py-1 text-center text-slate-400">–</td>;
  return (
    <td className="whitespace-nowrap px-2 py-1 text-center tabular-nums">
      {x.voto != null ? fmtVoto.format(x.voto) : "?"}
      {x.escanos && <span className="block text-xs text-slate-500">{x.escanos}</span>}
    </td>
  );
}

export default async function PaginaEncuestas() {
  const hoy = new Date().toLocaleDateString("sv-SE", { timeZone: "Europe/Madrid" }); // yyyy-MM-dd
  const calendario = await obtenerCalendario();
  const veda = calendario?.vedaEncuestasDesde ?? VEDA_POR_DEFECTO;
  // Doble comprobación (fecha local y la del backend) para no depender solo de uno de los dos.
  const enVeda = hoy >= veda || calendario?.encuestasPublicables === false;

  if (enVeda) {
    return (
      <section className="space-y-4">
        <h1 className="text-2xl font-bold">Encuestas</h1>
        <p className="rounded bg-slate-100 p-3 dark:bg-slate-800">
          Desde el {fmtLargo.format(dia(veda))} la ley electoral (LOREG, art. 69.7) prohíbe publicar
          encuestas electorales, por lo que esta web no las muestra.
        </p>
      </section>
    );
  }

  const r = await obtenerEncuestas();
  if (!r) {
    return (
      <section className="space-y-4">
        <h1 className="text-2xl font-bold">Encuestas</h1>
        <p>Las encuestas no están disponibles en este momento.</p>
      </section>
    );
  }
  const { partidos, encuestas, fuente } = r.datos;
  const filas = encuestas.slice(0, MAX_FILAS);

  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-bold">Encuestas</h1>
      <div className="space-y-2 text-sm text-slate-600 dark:text-slate-400">
        <p>
          Sondeos de intención de voto publicados en 2026, tal como los recoge Wikipedia. Se muestran todos, sin filtrar por empresa ni por quién los encarga
          (algunos los encargan medios y otros los propios partidos). Los partidos van en orden
          alfabético. En cada celda, el porcentaje de voto estimado y, debajo, los escaños (un número o
          una horquilla); «?» si la encuesta no da ese dato.
        </p>
        <p>
          Desde el {fmtLargo.format(dia(veda))} la ley electoral prohíbe publicar encuestas: a partir de
          ese día esta página dejará de mostrarlas.
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <caption className="sr-only">
            Estimación de voto por encuesta. Columnas: encuestadora, fin del trabajo de campo, muestra y un
            partido por columna.
          </caption>
          <thead>
            <tr className="border-b border-slate-200 dark:border-slate-800">
              <th scope="col" className="sticky left-0 bg-white px-2 py-1 text-left font-medium dark:bg-slate-950">
                Encuestadora / encargo
              </th>
              <th scope="col" className="px-2 py-1 text-left font-medium">
                Fin del campo
              </th>
              <th scope="col" className="px-2 py-1 text-right font-medium">
                Muestra
              </th>
              {partidos.map((p) => (
                <th key={p} scope="col" className="px-2 py-1 text-center font-medium">
                  {p}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filas.map((e, i) => (
              <tr key={`${e.encuestadora}-${e.fin}-${i}`} className="border-b border-slate-100 dark:border-slate-900">
                <th scope="row" className="sticky left-0 bg-white px-2 py-1 text-left font-normal dark:bg-slate-950">
                  {e.enlace ? (
                    <a href={e.enlace} target="_blank" rel="noopener noreferrer nofollow" className="underline">
                      {e.encuestadora}
                    </a>
                  ) : (
                    e.encuestadora
                  )}
                </th>
                <td className="whitespace-nowrap px-2 py-1" title={`Trabajo de campo: ${e.trabajoDeCampo}`}>
                  <time dateTime={e.fin}>{fmtDia.format(dia(e.fin))}</time>
                </td>
                <td className="px-2 py-1 text-right tabular-nums">
                  {e.muestra != null ? fmtEntero.format(e.muestra) : "?"}
                </td>
                {partidos.map((p) => (
                  <Celda key={p} e={e} partido={p} />
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="text-sm text-slate-600 dark:text-slate-400">
        {encuestas.length > filas.length && `Se muestran las ${filas.length} más recientes de ${encuestas.length}. `}
        <a href={fuente} target="_blank" rel="noopener noreferrer" className="underline">
          Tabla completa en la revisión de Wikipedia consultada
        </a>
        .
      </p>
      <FuenteInfo r={r} nombre="Wikipedia en inglés («Opinion polling for the 2026 Spanish general election»)" />
    </section>
  );
}
