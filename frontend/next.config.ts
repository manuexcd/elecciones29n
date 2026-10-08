import type { NextConfig } from "next";

const config: NextConfig = {
  // Genera .next/standalone: imagen Docker pequeña, sin node_modules completo.
  output: "standalone",
  poweredByHeader: false,
};

export default config;
