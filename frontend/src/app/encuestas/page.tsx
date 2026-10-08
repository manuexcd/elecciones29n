import type { Metadata } from "next";
import { FuenteInfo } from "@/components/FuenteInfo";
import { Aviso, BotonEnlace, Cifra, Tarjeta } from "@/components/ui";
import { listarPartidos, obtenerCalendario, obtenerEncuestas } from "@/lib/api";
import type { Encuesta, Partido } from "@/lib/types";

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
const fmtEntero = new Intl.NumberFormat("es-ES", { useGrouping: "always" });
const dia = (iso: string) => new Date(`${iso}T12:00:00Z`);

/**
 * Escala secuencial de UN solo tono (ámbar) para el % de voto, igual en todas las columnas: el
 * fondo indica magnitud, no partido. Tramos fijos para que el mismo valor tenga siempre el mismo
 * color. El texto va siempre en tinta, nunca en el color del fondo.
 */
const TRAMOS = [
  { desde: 30, etiqueta: "≥ 30", clase: "bg-amber-300 dark:bg-amber-400/40" },
  { desde: 20, etiqueta: "20–30", clase: "bg-amber-200 dark:bg-amber-400/28" },
  { desde: 10, etiqueta: "10–20", clase: "bg-amber-100 dark:bg-amber-400/18" },
  { desde: 5, etiqueta: "5–10", clase: "bg-amber-50 dark:bg-amber-400/10" },
  { desde: 1, etiqueta: "1–5", clase: "bg-stone-50 dark:bg-stone-800/60" },
  { desde: 0, etiqueta: "< 1", clase: "" },
];
const tramo = (voto: number) => TRAMOS.find((t) => voto >= t.desde) ?? TRAMOS[TRAMOS.length - 1];

function Celda({ e, partido }: { e: Encuesta; partido: string }) {
  const x = e.estimaciones.find((est) => est.partido === partido);
  if (!x)
    return (
      <td className="p-0.5">
        <div className="px-1.5 py-2 text-center text-stone-300 dark:text-stone-600">–</div>
      </td>
    );
  const voto = x.voto != null ? `${fmtVoto.format(x.voto)} %` : "voto no publicado";
  const escanos = x.escanos ? ` · ${x.escanos} escaños` : "";
  return (
    // Baldosa con 2 px de hueco entre celdas (el padding del td): sin bloques macizos de color.
    <td className="p-0.5" title={`${partido}: ${voto}${escanos} (${e.encuestadora}, ${fmtDia.format(dia(e.fin))})`}>
      <div
        className={`whitespace-nowrap rounded-md px-1.5 py-1.5 text-center tabular-nums transition hover:ring-2 hover:ring-stone-900/20 dark:hover:ring-white/30 ${
          x.voto != null ? tramo(x.voto).clase : ""
        }`}
      >
        <span className="font-semibold">{x.voto != null ? fmtVoto.format(x.voto) : "?"}</span>
        {x.escanos && <span className="block text-[11px] text-stone-600 dark:text-stone-300">{x.escanos}</span>}
      </div>
    </td>
  );
}

/** "NC Report/La Razón" → empresa "NC Report", encargo "La Razón". */
function separar(encuestadora: string): { empresa: string; encargo: string | null } {
  const i = encuestadora.indexOf("/");
  return i < 0
    ? { empresa: encuestadora, encargo: null }
    : { empresa: encuestadora.slice(0, i).trim(), encargo: encuestadora.slice(i + 1).trim() };
}

/** Busca el color del partido (Wikidata) por el nombre de columna de Wikipedia: "Junts" ↔ "JxCAT-JUNTS". */
const normalizar = (t: string) =>
  t.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().replace(/[^a-z0-9]/g, "");
function colorDe(columna: string, partidos: Partido[]): string | null {
  const n = normalizar(columna);
  const p = partidos.find((p) => {
    const siglas = normalizar(p.siglas ?? "");
    return siglas === n || normalizar(p.nombre) === n || (n.length >= 3 && siglas.includes(n));
  });
  return p?.color ?? null;
}

const fmtMes = new Intl.DateTimeFormat("es-ES", { month: "long", year: "numeric", timeZone: "UTC" });

function Cabecera({ children }: { children?: React.ReactNode }) {
  return (
    <div>
      <p className="text-sm font-medium text-amber-700 dark:text-amber-400">Sondeos de intención de voto</p>
      <h1 className="mt-1 text-3xl font-bold tracking-tight sm:text-4xl">Encuestas</h1>
      {children}
    </div>
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
      <section className="space-y-6">
        <Cabecera />
        <Aviso>
          Desde el {fmtLargo.format(dia(veda))} la ley electoral (LOREG, art. 69.7) prohíbe publicar encuestas
          electorales, por lo que esta web no las muestra.
        </Aviso>
      </section>
    );
  }

  const [r, listaPartidos] = await Promise.all([obtenerEncuestas(), listarPartidos()]);
  if (!r) {
    return (
      <section className="space-y-6">
        <Cabecera />
        <Tarjeta>Las encuestas no están disponibles en este momento.</Tarjeta>
      </section>
    );
  }
  const { partidos, encuestas, fuente } = r.datos;
  const filas = encuestas.slice(0, MAX_FILAS);
  const colores = new Map(partidos.map((p) => [p, colorDe(p, listaPartidos?.datos ?? [])]));
  const empresas = new Set(encuestas.map((e) => separar(e.encuestadora).empresa)).size;
  const diasVeda = Math.round((dia(veda).getTime() - dia(hoy).getTime()) / 86_400_000);
  const ultima = encuestas[0];
  const columnas = 2 + partidos.length;

  // Filas con un separador cada vez que cambia el mes del fin del trabajo de campo.
  const filasConMes: ({ tipo: "mes"; mes: string } | { tipo: "encuesta"; e: Encuesta; i: number })[] = [];
  filas.forEach((e, i) => {
    const mes = e.fin.slice(0, 7);
    if (i === 0 || filas[i - 1].fin.slice(0, 7) !== mes) filasConMes.push({ tipo: "mes", mes });
    filasConMes.push({ tipo: "encuesta", e, i });
  });

  return (
    <section className="space-y-6">
      <Cabecera>
        <p className="mt-2 max-w-3xl text-stone-600 dark:text-stone-400">
          Sondeos publicados en 2026, tal como los recoge Wikipedia. Se muestran todos, sin filtrar por empresa ni por
          quién los encarga (algunos los encargan medios y otros los propios partidos).
        </p>
      </Cabecera>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Cifra
          principal
          etiqueta="Hasta la veda"
          valor={diasVeda}
          detalle={`días: desde el ${fmtLargo.format(dia(veda))} no se pueden publicar`}
        />
        <Cifra retraso={60} etiqueta="Encuestas en 2026" valor={encuestas.length} detalle="publicadas y recogidas en la tabla" />
        <Cifra retraso={120} etiqueta="Encuestadoras" valor={empresas} detalle="empresas distintas" />
        <Cifra
          retraso={180}
          etiqueta="La más reciente"
          valor={ultima ? fmtDia.format(dia(ultima.fin)) : "–"}
          detalle={ultima ? `fin del trabajo de campo · ${separar(ultima.encuestadora).empresa}` : undefined}
        />
      </div>

      <div
        className="animate-aparecer overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-stone-200/70 dark:bg-stone-900 dark:ring-stone-800"
        style={{ animationDelay: "200ms" }}
      >
        <div className="flex flex-col gap-4 border-b border-stone-200 px-5 py-4 lg:flex-row lg:items-center lg:justify-between dark:border-stone-800">
          <div className="space-y-2">
            <p className="text-sm text-stone-500 dark:text-stone-400">
              Partidos en orden alfabético. En cada celda, el porcentaje de voto y, debajo, los escaños (un número o
              una horquilla); «?» si la encuesta no da ese dato.
            </p>
            <div className="flex flex-wrap items-center gap-1.5 text-xs text-stone-500 dark:text-stone-400" aria-hidden>
              <span className="mr-1">% de voto:</span>
              {[...TRAMOS].reverse().map((t) => (
                <span key={t.etiqueta} className={`rounded px-2 py-0.5 tabular-nums text-stone-700 ring-1 ring-stone-200 dark:text-stone-200 dark:ring-stone-700 ${t.clase}`}>
                  {t.etiqueta}
                </span>
              ))}
            </div>
          </div>
          <div className="shrink-0">
            <BotonEnlace href={fuente}>Tabla completa en Wikipedia</BotonEnlace>
          </div>
        </div>
        <p className="px-5 pt-3 text-xs text-stone-500 lg:hidden dark:text-stone-400">Desliza la tabla para ver todos los partidos →</p>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <caption className="sr-only">
              Estimación de voto por encuesta. Columnas: encuestadora (con quién la encarga y la muestra), fin del
              trabajo de campo y un partido por columna.
            </caption>
            <thead>
              <tr className="border-b border-stone-200 bg-stone-50 dark:border-stone-800 dark:bg-stone-800/60">
                <th
                  scope="col"
                  className="sticky left-0 z-10 bg-stone-50 px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-stone-500 dark:bg-stone-800 dark:text-stone-400"
                >
                  Encuestadora
                </th>
                <th scope="col" className="whitespace-nowrap px-2 py-3 text-left text-xs font-medium uppercase tracking-wide text-stone-500 dark:text-stone-400">
                  Fin
                </th>
                {partidos.map((p) => {
                  const color = colores.get(p);
                  return (
                    <th key={p} scope="col" className="min-w-14 px-1 py-3 text-center align-bottom font-semibold text-stone-800 dark:text-stone-100">
                      <span
                        aria-hidden
                        className={`mx-auto mb-1.5 block h-2.5 w-2.5 rounded-full ${color ? "" : "ring-1 ring-inset ring-stone-300 dark:ring-stone-600"}`}
                        style={color ? { backgroundColor: color } : undefined}
                      />
                      <span className="block text-xs leading-tight">{p}</span>
                    </th>
                  );
                })}
              </tr>
            </thead>
            <tbody>
              {filasConMes.map((f) =>
                f.tipo === "mes" ? (
                  <tr key={`mes-${f.mes}`} className="bg-stone-50/70 dark:bg-stone-800/30">
                    <th colSpan={columnas} scope="colgroup" className="px-4 pb-1.5 pt-4 text-left">
                      <span className="sticky left-4 text-xs font-semibold uppercase tracking-wider text-amber-700 dark:text-amber-400">
                        {fmtMes.format(dia(`${f.mes}-15`))}
                      </span>
                    </th>
                  </tr>
                ) : (
                  <FilaEncuesta key={`${f.e.encuestadora}-${f.e.fin}-${f.i}`} e={f.e} partidos={partidos} />
                ),
              )}
            </tbody>
          </table>
        </div>
        {encuestas.length > filas.length && (
          <p className="border-t border-stone-200 px-5 py-3 text-sm text-stone-500 dark:border-stone-800 dark:text-stone-400">
            Se muestran las {filas.length} más recientes de {encuestas.length}. Pasa el ratón por una celda para ver el
            detalle.
          </p>
        )}
      </div>

      <FuenteInfo r={r} nombre="Wikipedia en inglés («Opinion polling for the 2026 Spanish general election»)" />
    </section>
  );
}

function FilaEncuesta({ e, partidos }: { e: Encuesta; partidos: string[] }) {
  const { empresa, encargo } = separar(e.encuestadora);
  return (
    <tr className="group border-t border-stone-100 dark:border-stone-800">
      <th
        scope="row"
        className="sticky left-0 z-10 w-32 min-w-32 bg-white sm:w-44 sm:min-w-44 px-4 py-2.5 text-left font-normal transition-colors group-hover:bg-stone-50 dark:bg-stone-900 dark:group-hover:bg-stone-800"
      >
        <span className="block whitespace-nowrap font-semibold">
          {e.enlace ? (
            <a
              href={e.enlace}
              target="_blank"
              rel="noopener noreferrer nofollow"
              className="underline decoration-stone-300 underline-offset-2 hover:decoration-amber-500 dark:decoration-stone-600"
            >
              {empresa}
            </a>
          ) : (
            empresa
          )}
        </span>
        <span className="block text-xs leading-snug text-stone-500 dark:text-stone-400">
          {[encargo && `para ${encargo}`, e.muestra != null ? `${fmtEntero.format(e.muestra)} entrevistas` : "muestra no publicada"]
            .filter(Boolean)
            .join(" · ")}
        </span>
      </th>
      <td className="whitespace-nowrap px-2 py-2.5 text-stone-600 dark:text-stone-400" title={`Trabajo de campo: ${e.trabajoDeCampo}`}>
        <time dateTime={e.fin}>{fmtDia.format(dia(e.fin))}</time>
      </td>
      {partidos.map((p) => (
        <Celda key={p} e={e} partido={p} />
      ))}
    </tr>
  );
}
