import type { Calendario } from "@/lib/types";

const fmt = new Intl.DateTimeFormat("es-ES", {
  weekday: "short",
  day: "numeric",
  month: "long",
  timeZone: "Europe/Madrid",
});

// "yyyy-MM-dd" -> Date a mediodía UTC, para que la zona horaria no mueva el día.
const dia = (iso: string) => new Date(`${iso}T12:00:00Z`);

export function CalendarioElectoral({ c }: { c: Calendario }) {
  const hoy = new Date().toLocaleDateString("sv-SE", { timeZone: "Europe/Madrid" }); // yyyy-MM-dd
  return (
    <section aria-labelledby="calendario">
      <h2 id="calendario" className="text-xl font-semibold">
        Calendario electoral
      </h2>
      <ol className="mt-3 space-y-2">
        {c.hitos.map((h) => {
          const pasado = h.fecha < hoy;
          return (
            <li
              key={`${h.fecha}-${h.titulo}`}
              className={`flex gap-3 ${pasado ? "text-slate-500 dark:text-slate-500" : ""}`}
            >
              <time dateTime={h.fecha} className="w-36 shrink-0 font-medium capitalize">
                {fmt.format(dia(h.fecha))}
              </time>
              <span>
                {h.titulo}
                <span className="block text-xs text-slate-500">{h.fuente}</span>
              </span>
            </li>
          );
        })}
      </ol>
      {!c.encuestasPublicables && (
        <p className="mt-4 rounded bg-slate-100 p-3 text-sm dark:bg-slate-800">
          Desde el {fmt.format(dia(c.vedaEncuestasDesde))} no se pueden publicar encuestas electorales,
          por lo que esta web no las muestra.
        </p>
      )}
    </section>
  );
}
