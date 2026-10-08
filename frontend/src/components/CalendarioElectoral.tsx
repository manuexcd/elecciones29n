import { Pastilla, Tarjeta, TituloSeccion } from "@/components/ui";
import type { Calendario } from "@/lib/types";

// "yyyy-MM-dd" -> Date a mediodía UTC, para que la zona horaria no mueva el día.
const dia = (iso: string) => new Date(`${iso}T12:00:00Z`);
const fmtDia = new Intl.DateTimeFormat("es-ES", { day: "numeric", month: "short", timeZone: "UTC" });
const fmtSemana = new Intl.DateTimeFormat("es-ES", { weekday: "short", timeZone: "UTC" });
const fmtLargo = new Intl.DateTimeFormat("es-ES", { day: "numeric", month: "long", timeZone: "UTC" });

/** Línea de tiempo: hitos pasados atenuados, el próximo destacado. */
export function CalendarioElectoral({ c, hoy }: { c: Calendario; hoy: string }) {
  const proximo = c.hitos.find((h) => h.fecha >= hoy);
  return (
    <Tarjeta etiqueta="calendario">
      <TituloSeccion id="calendario">Calendario electoral</TituloSeccion>
      <ol className="relative ml-1.5 border-l border-stone-200 dark:border-stone-700">
        {c.hitos.map((h) => {
          const pasado = h.fecha < hoy;
          const esProximo = h === proximo;
          const votacion = h.fecha === c.fechaElecciones;
          return (
            <li key={`${h.fecha}-${h.titulo}`} className="relative pb-5 pl-5 last:pb-0">
              <span
                aria-hidden
                className={`absolute -left-[5px] top-1.5 h-2.5 w-2.5 rounded-full ring-4 ring-white dark:ring-stone-900 ${
                  esProximo ? "bg-amber-500" : votacion ? "bg-stone-900 dark:bg-stone-100" : pasado ? "bg-stone-300 dark:bg-stone-600" : "border-2 border-stone-300 bg-white dark:border-stone-500 dark:bg-stone-900"
                }`}
              />
              <div className={pasado ? "text-stone-400 dark:text-stone-500" : ""}>
                <p className="flex flex-wrap items-center gap-2 text-sm">
                  <time dateTime={h.fecha} className="font-semibold tabular-nums">
                    {fmtDia.format(dia(h.fecha))}
                  </time>
                  <span className="text-xs uppercase tracking-wide text-stone-400">{fmtSemana.format(dia(h.fecha))}</span>
                  {esProximo && <Pastilla tono="acento">Próximo</Pastilla>}
                </p>
                <p className={`mt-0.5 text-sm ${votacion ? "font-semibold" : ""}`}>{h.titulo}</p>
                <p className="mt-0.5 text-xs text-stone-400">{h.fuente}</p>
              </div>
            </li>
          );
        })}
      </ol>
      {!c.encuestasPublicables && (
        <p className="mt-5 rounded-lg bg-stone-100 p-3 text-sm dark:bg-stone-800">
          Desde el {fmtLargo.format(dia(c.vedaEncuestasDesde))} no se pueden publicar encuestas electorales, por lo
          que esta web no las muestra.
        </p>
      )}
    </Tarjeta>
  );
}
