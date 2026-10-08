/** Piezas visuales compartidas (estilo «dashboard»: tarjetas blancas sobre fondo piedra). */

/** Retraso de la animación de entrada, para que las tarjetas aparezcan escalonadas. */
const retrasar = (ms?: number) => (ms ? { animationDelay: `${ms}ms` } : undefined);

export function Tarjeta({
  children,
  className = "",
  etiqueta,
  retraso,
}: {
  children: React.ReactNode;
  className?: string;
  /** Si se indica, la tarjeta es una <section> con ese id de encabezado. */
  etiqueta?: string;
  retraso?: number;
}) {
  const clases = `animate-aparecer rounded-2xl bg-white p-5 shadow-sm ring-1 ring-stone-200/70 sm:p-6 dark:bg-stone-900 dark:ring-stone-800 ${className}`;
  return etiqueta ? (
    <section aria-labelledby={etiqueta} className={clases} style={retrasar(retraso)}>
      {children}
    </section>
  ) : (
    <div className={clases} style={retrasar(retraso)}>
      {children}
    </div>
  );
}

export function TituloSeccion({
  id,
  children,
  subtitulo,
}: {
  id: string;
  children: React.ReactNode;
  subtitulo?: React.ReactNode;
}) {
  return (
    <header className="mb-5">
      <h2 id={id} className="text-lg font-semibold tracking-tight">
        {children}
      </h2>
      {subtitulo && <p className="mt-1 text-sm text-stone-500 dark:text-stone-400">{subtitulo}</p>}
    </header>
  );
}

/** Cifra destacada. La variante "principal" (fondo oscuro, número ámbar) se usa una sola vez por página. */
export function Cifra({
  valor,
  etiqueta,
  detalle,
  principal = false,
  progreso,
  retraso,
}: {
  valor: React.ReactNode;
  etiqueta: React.ReactNode;
  detalle?: React.ReactNode;
  principal?: boolean;
  /** 0..1: barra fina bajo la cifra (p. ej. cuánto ha avanzado el periodo electoral). */
  progreso?: { valor: number; texto: string };
  retraso?: number;
}) {
  return (
    <div
      style={retrasar(retraso)}
      className={`animate-aparecer relative overflow-hidden rounded-2xl p-5 shadow-sm ${
        principal
          ? "bg-stone-900 text-white dark:bg-amber-400/10 dark:ring-1 dark:ring-amber-400/30"
          : "bg-white ring-1 ring-stone-200/70 dark:bg-stone-900 dark:ring-stone-800"
      }`}
    >
      {principal && (
        // Brillo ámbar decorativo en la esquina.
        <span aria-hidden className="pointer-events-none absolute -right-10 -top-10 h-32 w-32 rounded-full bg-amber-400/20 blur-2xl" />
      )}
      <p className={`text-xs font-medium uppercase tracking-wider ${principal ? "text-stone-400" : "text-stone-500 dark:text-stone-400"}`}>
        {etiqueta}
      </p>
      <p
        className={`mt-2 text-4xl font-bold tracking-tight tabular-nums ${principal ? "text-amber-400" : ""}`}
      >
        {valor}
      </p>
      {detalle && (
        <p className={`mt-1 text-sm ${principal ? "text-stone-300" : "text-stone-500 dark:text-stone-400"}`}>{detalle}</p>
      )}
      {progreso && (
        <div className="mt-4">
          <div
            className="h-1.5 overflow-hidden rounded-full bg-white/15"
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.round(progreso.valor * 100)}
            aria-label={progreso.texto}
          >
            <div className="h-full rounded-full bg-amber-400" style={{ width: `${Math.min(1, Math.max(0, progreso.valor)) * 100}%` }} />
          </div>
          <p className="mt-1.5 text-xs text-stone-400">{progreso.texto}</p>
        </div>
      )}
    </div>
  );
}

export function Pastilla({ children, tono = "neutro" }: { children: React.ReactNode; tono?: "neutro" | "acento" }) {
  const colores =
    tono === "acento"
      ? "bg-amber-100 text-amber-900 dark:bg-amber-400/15 dark:text-amber-300"
      : "bg-stone-100 text-stone-700 dark:bg-stone-800 dark:text-stone-300";
  return <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${colores}`}>{children}</span>;
}

/** Aviso destacado (lista provisional, veda...). */
export function Aviso({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex gap-3 rounded-xl bg-amber-50 p-4 text-sm text-amber-950 ring-1 ring-amber-200 dark:bg-amber-400/10 dark:text-amber-100 dark:ring-amber-400/20">
      <IconoInfo className="mt-0.5 h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" />
      <div>{children}</div>
    </div>
  );
}

/** Enlace externo con aspecto de botón. */
export function BotonEnlace({ href, children, rel = "noopener noreferrer" }: { href: string; children: React.ReactNode; rel?: string }) {
  return (
    <a
      href={href}
      target="_blank"
      rel={rel}
      className="inline-flex items-center gap-1.5 rounded-lg bg-white px-3 py-2 text-sm font-medium shadow-sm ring-1 ring-stone-200 transition hover:bg-stone-50 dark:bg-stone-800 dark:ring-stone-700 dark:hover:bg-stone-700"
    >
      {children}
      <IconoExterno className="h-3.5 w-3.5 text-stone-400" />
    </a>
  );
}

export function IconoInfo({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 20 20" fill="currentColor" className={className} aria-hidden>
      <path
        fillRule="evenodd"
        d="M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Zm-7-4a1 1 0 1 1-2 0 1 1 0 0 1 2 0ZM9 9a.75.75 0 0 0 0 1.5h.25v2.75a.75.75 0 0 0 1.5 0V9.75A.75.75 0 0 0 10 9H9Z"
        clipRule="evenodd"
      />
    </svg>
  );
}

export function IconoExterno({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 20 20" fill="currentColor" className={className} aria-hidden>
      <path
        fillRule="evenodd"
        d="M4.25 5.5a.75.75 0 0 0-.75.75v8.5c0 .414.336.75.75.75h8.5a.75.75 0 0 0 .75-.75v-4a.75.75 0 0 1 1.5 0v4A2.25 2.25 0 0 1 12.75 17h-8.5A2.25 2.25 0 0 1 2 14.75v-8.5A2.25 2.25 0 0 1 4.25 4h5a.75.75 0 0 1 0 1.5h-5Zm7.25-.75a.75.75 0 0 1 .75-.75h3.5a.75.75 0 0 1 .75.75v3.5a.75.75 0 0 1-1.5 0V6.56l-5.22 5.22a.75.75 0 1 1-1.06-1.06l5.22-5.22h-1.69a.75.75 0 0 1-.75-.75Z"
        clipRule="evenodd"
      />
    </svg>
  );
}

export function IconoFlecha({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 20 20" fill="currentColor" className={className} aria-hidden>
      <path
        fillRule="evenodd"
        d="M3 10a.75.75 0 0 1 .75-.75h10.64l-4.22-4.22a.75.75 0 1 1 1.06-1.06l5.5 5.5a.75.75 0 0 1 0 1.06l-5.5 5.5a.75.75 0 1 1-1.06-1.06l4.22-4.22H3.75A.75.75 0 0 1 3 10Z"
        clipRule="evenodd"
      />
    </svg>
  );
}
