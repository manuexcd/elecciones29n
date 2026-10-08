import type { Respuesta } from "@/lib/types";

const fmt = new Intl.DateTimeFormat("es-ES", {
  dateStyle: "medium",
  timeStyle: "short",
  timeZone: "Europe/Madrid",
});

/** Procedencia y frescura de un dato. Si la fuente está caída, lo dice en lugar de callarlo. */
export function FuenteInfo({ r, nombre }: { r: Respuesta<unknown>; nombre: string }) {
  return (
    <div className="text-sm text-slate-600 dark:text-slate-400">
      <p>
        Fuente: {nombre} · Actualizado: {fmt.format(new Date(r.actualizado))}
      </p>
      {r.desactualizado && (
        <p className="mt-1 rounded bg-amber-100 px-2 py-1 text-amber-900 dark:bg-amber-900/40 dark:text-amber-200">
          La fuente no responde ahora mismo: se muestra el último dato disponible.
        </p>
      )}
    </div>
  );
}
