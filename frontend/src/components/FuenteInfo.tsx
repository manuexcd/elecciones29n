import type { Respuesta } from "@/lib/types";

const fmt = new Intl.DateTimeFormat("es-ES", {
  dateStyle: "medium",
  timeStyle: "short",
  timeZone: "Europe/Madrid",
});

/** Procedencia y frescura de un dato. Si la fuente está caída, lo dice en lugar de callarlo. */
export function FuenteInfo({ r, nombre }: { r: Respuesta<unknown>; nombre: string }) {
  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-stone-500 dark:text-stone-400">
      <span>
        <span className="font-medium text-stone-600 dark:text-stone-300">Fuente:</span> {nombre}
      </span>
      <span className="flex items-center gap-1.5">
        <span
          className={`h-1.5 w-1.5 rounded-full ${r.desactualizado ? "bg-amber-500" : "bg-stone-400"}`}
          aria-hidden
        />
        Actualizado: {fmt.format(new Date(r.actualizado))}
      </span>
      {r.desactualizado && (
        <span className="w-full rounded-lg bg-amber-50 px-2.5 py-1.5 text-amber-900 ring-1 ring-amber-200 dark:bg-amber-400/10 dark:text-amber-200 dark:ring-amber-400/20">
          La fuente no responde ahora mismo: se muestra el último dato disponible.
        </span>
      )}
    </div>
  );
}
