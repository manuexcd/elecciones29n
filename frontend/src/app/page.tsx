import { CalendarioElectoral } from "@/components/CalendarioElectoral";
import { ComposicionCongreso } from "@/components/ComposicionCongreso";
import { FuenteInfo } from "@/components/FuenteInfo";
import { listarPartidos, obtenerCalendario, obtenerComposicion } from "@/lib/api";
import type { Partido } from "@/lib/types";

// Se renderiza en cada petición (no en el build): el build no necesita el backend en marcha.
// El caché real lo hacen el fetch (60 s) y la CDN (cabeceras de Caddy/Cloudflare).
export const dynamic = "force-dynamic";

const DIA_ELECCIONES = "2026-11-29";
const DIA_PROCLAMACION = "2026-10-28";

function diasHasta(iso: string): number {
  const hoy = new Date().toLocaleDateString("sv-SE", { timeZone: "Europe/Madrid" });
  const ms = new Date(`${iso}T00:00:00Z`).getTime() - new Date(`${hoy}T00:00:00Z`).getTime();
  return Math.round(ms / 86_400_000);
}

function TarjetaPartido({ p }: { p: Partido }) {
  const contenido = (
    <>
      {p.logo ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={p.logo} alt="" width={40} height={40} loading="lazy" className="h-10 w-10 object-contain" />
      ) : (
        <span className="h-10 w-10 shrink-0 rounded bg-slate-200 dark:bg-slate-800" aria-hidden />
      )}
      <span>
        <span className="block font-medium">{p.nombre}</span>
        <span className="text-sm text-slate-500">
          {p.siglas}
          {p.escanos != null && ` · ${p.escanos} ${p.escanos === 1 ? "escaño" : "escaños"}`}
        </span>
      </span>
    </>
  );
  const clases = "flex h-full items-center gap-3 rounded-lg border border-slate-200 p-3 dark:border-slate-800";
  // Sin QID no hay ficha a la que enlazar, pero el partido se muestra igual (mismos campos para todos).
  return p.id ? (
    <a href={`/partidos/${p.id}`} className={`${clases} hover:bg-slate-50 dark:hover:bg-slate-900`}>
      {contenido}
    </a>
  ) : (
    <div className={clases}>{contenido}</div>
  );
}

export default async function Home() {
  const [calendario, partidos, composicion] = await Promise.all([
    obtenerCalendario(),
    listarPartidos(),
    obtenerComposicion(),
  ]);
  const dias = diasHasta(calendario?.fechaElecciones ?? DIA_ELECCIONES);
  const hoy = new Date().toLocaleDateString("sv-SE", { timeZone: "Europe/Madrid" });

  return (
    <div className="space-y-12">
      <section>
        <p className="text-slate-600 dark:text-slate-400">
          Domingo 29 de noviembre de 2026 ·{" "}
          {dias > 0 ? `faltan ${dias} días` : dias === 0 ? "hoy se vota" : "jornada electoral celebrada"}
        </p>
      </section>

      {calendario ? (
        <CalendarioElectoral c={calendario} />
      ) : (
        <p>El calendario no está disponible en este momento.</p>
      )}

      <section aria-labelledby="partidos">
        <h2 id="partidos" className="text-xl font-semibold">
          Partidos políticos
        </h2>

        {hoy < DIA_PROCLAMACION && (
          <p className="mt-3 rounded bg-slate-100 p-3 text-sm dark:bg-slate-800">
            Lista provisional: son las formaciones que tenían escaños en el Congreso al disolverse las
            Cortes, no necesariamente las que concurren el 29-N. La lista oficial de candidaturas se
            publicará en el BOE el 28 de octubre.
          </p>
        )}

        {partidos ? (
          <>
            <ul className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {partidos.datos.map((p) => (
                <li key={p.siglas ?? p.nombre}>
                  <TarjetaPartido p={p} />
                </li>
              ))}
            </ul>
            <div className="mt-4">
              <FuenteInfo r={partidos} nombre="Congreso de los Diputados (escaños) y Wikidata (fichas)" />
            </div>
          </>
        ) : (
          <p className="mt-4">Los datos de partidos no están disponibles en este momento. Inténtalo de nuevo en unos minutos.</p>
        )}
      </section>

      {composicion && <ComposicionCongreso r={composicion} partidos={partidos?.datos ?? null} />}
    </div>
  );
}
