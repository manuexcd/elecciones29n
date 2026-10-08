import type { Partido } from "@/lib/types";

/**
 * Hemiciclo del Congreso: un punto por escaño, en SVG renderizado en el servidor (sin JS de cliente).
 *
 * Los partidos se colocan de izquierda a derecha en ORDEN ALFABÉTICO (criterio de neutralidad del
 * proyecto), no por ideología: la posición en el dibujo no significa nada político.
 *
 * Colores: los de cada partido según Wikidata (P465), porque es lo que el lector reconoce. Algunos
 * se parecen mucho entre sí (EH Bildu y Junts, por ejemplo), así que la identidad nunca depende solo
 * del color: la leyenda lleva nombre y escaños, y cada punto muestra su partido al pasar el ratón.
 */

const ANCHO = 640;
const ALTO = 340;
const CX = ANCHO / 2;
const CY = ALTO - 10;
const R_EXT = 310;
const R_INT = 120;
const FILAS = 10;
const SIN_COLOR = "#94A3B8"; // partido sin color en Wikidata

type Escano = { x: number; y: number; angulo: number };

/** Reparte n escaños en FILAS arcos concéntricos, con más escaños cuanto más exterior es el arco. */
function posiciones(n: number): { escanos: Escano[]; radio: number } {
  const radios = Array.from({ length: FILAS }, (_, i) => R_INT + ((R_EXT - R_INT) * i) / (FILAS - 1));
  const suma = radios.reduce((a, b) => a + b, 0);
  const porFila = radios.map((r) => Math.round((n * r) / suma));
  porFila[FILAS - 1] += n - porFila.reduce((a, b) => a + b, 0); // cuadrar el redondeo

  const escanos: Escano[] = [];
  radios.forEach((r, i) => {
    const k = porFila[i];
    for (let j = 0; j < k; j++) {
      const angulo = k === 1 ? Math.PI / 2 : Math.PI - (Math.PI * j) / (k - 1); // de izquierda a derecha
      escanos.push({ x: CX + r * Math.cos(angulo), y: CY - r * Math.sin(angulo), angulo });
    }
  });
  // Ordenar por ángulo (izquierda → derecha) para que cada partido ocupe una cuña continua.
  escanos.sort((a, b) => b.angulo - a.angulo || Math.hypot(a.x - CX, a.y - CY) - Math.hypot(b.x - CX, b.y - CY));

  const separacionRadial = (R_EXT - R_INT) / (FILAS - 1);
  const separacionArco = (Math.PI * R_INT) / Math.max(1, porFila[0] - 1);
  return { escanos, radio: 0.42 * Math.min(separacionRadial, separacionArco) };
}

const plural = (n: number) => `${n} ${n === 1 ? "escaño" : "escaños"}`;

export function Hemiciclo({ partidos }: { partidos: Partido[] }) {
  const conEscanos = partidos.filter((p) => (p.escanos ?? 0) > 0);
  const total = conEscanos.reduce((a, p) => a + (p.escanos ?? 0), 0);
  if (total === 0) return null;
  const mayoria = Math.floor(total / 2) + 1;
  const { escanos, radio } = posiciones(total);

  let inicio = 0;
  const bloques = conEscanos.map((p) => {
    const bloque = { p, puntos: escanos.slice(inicio, inicio + (p.escanos ?? 0)) };
    inicio += p.escanos ?? 0;
    return bloque;
  });

  return (
    <figure>
      <svg
        viewBox={`0 0 ${ANCHO} ${ALTO}`}
        className="w-full max-w-2xl"
        role="img"
        aria-labelledby="hemiciclo-titulo"
      >
        <title id="hemiciclo-titulo">
          {`Reparto de los ${total} escaños: ${conEscanos.map((p) => `${p.nombre}, ${p.escanos}`).join("; ")}`}
        </title>
        {bloques.map(({ p, puntos }) => (
          <g key={p.siglas ?? p.nombre} fill={p.color ?? SIN_COLOR}>
            <title>{`${p.nombre} (${p.siglas}): ${plural(p.escanos ?? 0)}`}</title>
            {puntos.map((e, i) => (
              <circle key={i} cx={e.x.toFixed(1)} cy={e.y.toFixed(1)} r={radio.toFixed(1)} />
            ))}
          </g>
        ))}
        <text x={CX} y={CY - 38} textAnchor="middle" className="fill-slate-900 text-[40px] font-semibold dark:fill-slate-100">
          {total}
        </text>
        <text x={CX} y={CY - 14} textAnchor="middle" className="fill-slate-600 text-[15px] dark:fill-slate-400">
          escaños
        </text>
      </svg>

      <figcaption className="mt-4">
        <p className="mb-3 text-sm text-slate-600 dark:text-slate-400">
          Mayoría absoluta: <span className="font-medium text-slate-900 dark:text-slate-100">{mayoria} escaños</span>
        </p>
        <ul className="grid gap-x-6 gap-y-1 text-sm sm:grid-cols-2 lg:grid-cols-3">
          {conEscanos.map((p) => (
            <li key={p.siglas ?? p.nombre} className="flex items-center gap-2">
              <span
                className="inline-block h-3 w-3 shrink-0 rounded-full"
                style={{ backgroundColor: p.color ?? SIN_COLOR }}
                aria-hidden
              />
              <span className="min-w-0 flex-1 truncate">{p.nombre}</span>
              <span className="tabular-nums font-medium">{p.escanos}</span>
            </li>
          ))}
        </ul>
        <p className="mt-2 text-xs text-slate-500">
          Orden alfabético de izquierda a derecha; la posición no indica ideología. Colores de Wikidata.
        </p>
      </figcaption>
    </figure>
  );
}
