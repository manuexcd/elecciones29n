import { CalendarioElectoral } from "@/components/CalendarioElectoral";
import { ComposicionCongreso, GruposParlamentarios } from "@/components/ComposicionCongreso";
import { FuenteInfo } from "@/components/FuenteInfo";
import { Aviso, Cifra, IconoFlecha, Pastilla, Tarjeta, TituloSeccion } from "@/components/ui";
import { listarPartidos, obtenerCalendario, obtenerComposicion } from "@/lib/api";
import type { Partido } from "@/lib/types";

// Se renderiza en cada petición (no en el build): el build no necesita el backend en marcha.
// El caché real lo hacen el fetch (60 s) y la CDN (cabeceras de Caddy/Cloudflare).
export const dynamic = "force-dynamic";

const DIA_ELECCIONES = "2026-11-29";
const DIA_PROCLAMACION = "2026-10-28";
const DIA_CONVOCATORIA = "2026-10-06";
const SIN_COLOR = "#D6D3D1";

const fmtDia = new Intl.DateTimeFormat("es-ES", { day: "numeric", month: "short", timeZone: "UTC" });

function diasHasta(iso: string, hoy: string): number {
  const ms = new Date(`${iso}T00:00:00Z`).getTime() - new Date(`${hoy}T00:00:00Z`).getTime();
  return Math.round(ms / 86_400_000);
}

function TarjetaPartido({ p, retraso }: { p: Partido; retraso: number }) {
  const contenido = (
    <>
      {/* Franja con el color del partido (Wikidata): identifica, no ordena ni destaca. */}
      <span className="absolute inset-x-0 top-0 h-1" style={{ backgroundColor: p.color ?? SIN_COLOR }} aria-hidden />
      <span className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-white p-1.5 ring-1 ring-stone-200 transition group-hover:scale-105 dark:ring-stone-700">
        {p.logo ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={p.logo} alt="" width={40} height={40} loading="lazy" className="max-h-full max-w-full object-contain" />
        ) : (
          <span className="text-xs font-bold text-stone-400">{p.siglas?.slice(0, 4)}</span>
        )}
      </span>
      <span className="min-w-0 flex-1">
        <span className="block font-semibold leading-snug">{p.nombre}</span>
        <span className="mt-1 flex items-center gap-2">
          {p.siglas && <Pastilla>{p.siglas}</Pastilla>}
        </span>
      </span>
      <span className="text-right">
        <span className="block text-2xl font-bold tabular-nums">{p.escanos ?? "–"}</span>
        <span className="block text-xs text-stone-500 dark:text-stone-400">{p.escanos === 1 ? "escaño" : "escaños"}</span>
      </span>
    </>
  );
  const clases =
    "animate-aparecer relative flex h-full items-center gap-4 overflow-hidden rounded-2xl bg-white p-4 pt-5 shadow-sm ring-1 ring-stone-200/70 dark:bg-stone-900 dark:ring-stone-800";
  const estilo = { animationDelay: `${retraso}ms` };
  // Sin QID no hay ficha a la que enlazar, pero el partido se muestra igual (mismos campos para todos).
  return p.id ? (
    <a
      href={`/partidos/${p.id}`}
      style={estilo}
      className={`${clases} group transition hover:-translate-y-0.5 hover:shadow-md hover:ring-stone-300 dark:hover:ring-stone-700`}
    >
      {contenido}
      <IconoFlecha className="h-4 w-4 shrink-0 text-stone-300 transition group-hover:translate-x-0.5 group-hover:text-stone-600 dark:text-stone-600 dark:group-hover:text-stone-300" />
    </a>
  ) : (
    <div className={clases} style={estilo}>
      {contenido}
    </div>
  );
}

export default async function Home() {
  const [calendario, partidos, composicion] = await Promise.all([
    obtenerCalendario(),
    listarPartidos(),
    obtenerComposicion(),
  ]);
  const hoy = new Date().toLocaleDateString("sv-SE", { timeZone: "Europe/Madrid" }); // yyyy-MM-dd
  const dias = diasHasta(calendario?.fechaElecciones ?? DIA_ELECCIONES, hoy);
  const proximo = calendario?.hitos.find((h) => h.fecha >= hoy && h.fecha !== calendario.fechaElecciones);
  const total = composicion?.datos.total ?? 350;
  // Periodo electoral: de la convocatoria (primer hito) al día de la votación.
  const inicio = calendario?.hitos[0]?.fecha ?? DIA_CONVOCATORIA;
  const duracion = diasHasta(calendario?.fechaElecciones ?? DIA_ELECCIONES, inicio);
  const transcurridos = Math.min(duracion, Math.max(0, duracion - dias));

  return (
    <div className="space-y-8">
      <section className="relative space-y-6">
        {/* Brillo ámbar decorativo detrás del titular. */}
        <div
          aria-hidden
          className="pointer-events-none absolute -left-24 -top-24 -z-10 h-72 w-[36rem] max-w-full rounded-full bg-amber-300/25 blur-3xl dark:bg-amber-500/10"
        />
        <div className="animate-aparecer">
          <p className="text-sm font-medium text-amber-700 dark:text-amber-400">Domingo, 29 de noviembre de 2026</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight sm:text-4xl">Elecciones generales</h1>
          <p className="mt-2 max-w-2xl text-stone-600 dark:text-stone-400">
            Los datos de los partidos, obtenidos en tiempo real de fuentes abiertas. Cada dato indica de dónde sale y
            cuándo se consultó.
          </p>
        </div>

        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <Cifra
            principal
            etiqueta="Cuenta atrás"
            valor={dias > 0 ? dias : dias === 0 ? "Hoy" : "—"}
            detalle={dias > 1 ? "días para votar" : dias === 1 ? "día para votar" : dias === 0 ? "se vota" : "jornada celebrada"}
            progreso={
              duracion > 0
                ? { valor: transcurridos / duracion, texto: `Día ${transcurridos} de ${duracion} desde la convocatoria` }
                : undefined
            }
          />
          <Cifra
            retraso={60}
            etiqueta="Próximo hito"
            valor={proximo ? fmtDia.format(new Date(`${proximo.fecha}T12:00:00Z`)) : "—"}
            detalle={proximo?.titulo ?? "Sin hitos pendientes"}
          />
          <Cifra retraso={120} etiqueta="Escaños en juego" valor={total} detalle={`Mayoría absoluta: ${Math.floor(total / 2) + 1}`} />
          <Cifra
            retraso={180}
            etiqueta="Formaciones"
            valor={partidos?.datos.length ?? "—"}
            detalle="con escaños en la legislatura saliente"
          />
        </div>
      </section>

      <Tarjeta etiqueta="partidos" retraso={150}>
        <TituloSeccion id="partidos" subtitulo="En orden alfabético. Pulsa en un partido para ver su ficha.">
          Partidos políticos
        </TituloSeccion>

        {hoy < DIA_PROCLAMACION && (
          <div className="mb-5">
            <Aviso>
              <strong className="font-semibold">Lista provisional.</strong> Son las formaciones que tenían escaños en el
              Congreso al disolverse las Cortes, no necesariamente las que concurren el 29-N. La lista oficial de
              candidaturas se publicará en el BOE el 28 de octubre.
            </Aviso>
          </div>
        )}

        {partidos ? (
          <>
            <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {partidos.datos.map((p, i) => (
                <li key={p.siglas ?? p.nombre}>
                  <TarjetaPartido p={p} retraso={250 + i * 40} />
                </li>
              ))}
            </ul>
            <div className="mt-5">
              <FuenteInfo r={partidos} nombre="Congreso de los Diputados (escaños) y Wikidata (fichas)" />
            </div>
          </>
        ) : (
          <p>Los datos de partidos no están disponibles en este momento. Inténtalo de nuevo en unos minutos.</p>
        )}
      </Tarjeta>

      <div className="grid gap-8 lg:grid-cols-3">
        <div className="space-y-8 lg:col-span-2">
          {composicion ? (
            <>
              <ComposicionCongreso r={composicion} partidos={partidos?.datos ?? null} />
              <GruposParlamentarios r={composicion} />
            </>
          ) : (
            <Tarjeta>La composición del Congreso no está disponible en este momento.</Tarjeta>
          )}
        </div>
        {calendario ? (
          <div>
            <CalendarioElectoral c={calendario} hoy={hoy} />
          </div>
        ) : (
          <Tarjeta>El calendario no está disponible en este momento.</Tarjeta>
        )}
      </div>

    </div>
  );
}
