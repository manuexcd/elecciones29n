import type { Metadata } from "next";
import { Plus_Jakarta_Sans } from "next/font/google";
import "./globals.css";

// next/font descarga la fuente en el build y la sirve desde el propio dominio (sin peticiones a Google).
const jakarta = Plus_Jakarta_Sans({ subsets: ["latin"], variable: "--font-jakarta", display: "swap" });

export const metadata: Metadata = {
  title: "Elecciones generales 29-N",
  description:
    "Información de los partidos políticos de cara a las elecciones generales del 29 de noviembre de 2026, obtenida en tiempo real de fuentes abiertas.",
};

const enlaces = [
  { href: "/", texto: "Partidos" },
  { href: "/encuestas", texto: "Encuestas" },
];

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es" className={jakarta.variable}>
      <body className="font-sans">
        <header className="sticky top-0 z-20 border-b border-stone-200/70 bg-stone-100/85 backdrop-blur dark:border-stone-800 dark:bg-stone-950/85">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
            <a href="/" className="flex items-center gap-2.5" aria-label="Elecciones generales 29-N: inicio">
              <span className="grid h-9 w-9 place-items-center rounded-xl bg-stone-900 text-xs font-bold text-amber-400 dark:bg-stone-800">
                29N
              </span>
              {/* En móvil solo la insignia: el nombre no cabe junto al menú. */}
              <span className="hidden leading-tight sm:block">
                <span className="block text-sm font-semibold">Elecciones generales</span>
                <span className="block text-xs text-stone-500 dark:text-stone-400">29 de noviembre de 2026</span>
              </span>
            </a>
            <nav aria-label="Secciones" className="flex gap-1 text-sm font-medium">
              {enlaces.map((e) => (
                <a
                  key={e.href}
                  href={e.href}
                  className="rounded-lg px-3 py-1.5 text-stone-600 transition hover:bg-white hover:text-stone-900 hover:shadow-sm dark:text-stone-300 dark:hover:bg-stone-800 dark:hover:text-white"
                >
                  {e.texto}
                </a>
              ))}
            </nav>
          </div>
        </header>

        <main className="mx-auto max-w-6xl px-4 py-8 sm:py-10">{children}</main>

        <footer className="mx-auto max-w-6xl px-4 pb-10">
          <div className="border-t border-stone-200 pt-6 text-sm text-stone-500 dark:border-stone-800 dark:text-stone-400">
            <p>
              Proyecto personal e independiente. Los partidos se muestran en orden alfabético y con los mismos
              campos para todos. Cada dato indica su fuente y cuándo se obtuvo.
            </p>
          </div>
        </footer>
      </body>
    </html>
  );
}
