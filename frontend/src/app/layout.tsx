import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Elecciones generales 29-N",
  description:
    "Información de los partidos políticos de cara a las elecciones generales del 29 de noviembre de 2026, obtenida en tiempo real de fuentes abiertas.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body>
        <div className="mx-auto max-w-5xl px-4 py-8">
          <header className="mb-8 flex flex-wrap items-baseline justify-between gap-2 border-b border-slate-200 pb-4 dark:border-slate-800">
            <a href="/" className="text-2xl font-bold">
              Elecciones generales 29-N
            </a>
            <nav aria-label="Secciones" className="flex gap-4 text-sm">
              <a href="/" className="underline-offset-4 hover:underline">
                Partidos
              </a>
              <a href="/encuestas" className="underline-offset-4 hover:underline">
                Encuestas
              </a>
            </nav>
          </header>
          <main>{children}</main>
          <footer className="mt-12 border-t border-slate-200 pt-4 text-sm text-slate-600 dark:border-slate-800 dark:text-slate-400">
            Proyecto personal e independiente. Los partidos se muestran en orden alfabético y con los
            mismos campos para todos. Cada dato indica su fuente y cuándo se obtuvo.
          </footer>
        </div>
      </body>
    </html>
  );
}
