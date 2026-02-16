import { dirname } from "path";
import { fileURLToPath } from "url";
import { FlatCompat } from "@eslint/eslintrc";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const compat = new FlatCompat({
  baseDirectory: __dirname,
});

const eslintConfig = [
  ...compat.extends("next/core-web-vitals", "next/typescript", "prettier"),
  {
    ignores: [
      "node_modules/**",
      ".next/**",
      "out/**",
      "build/**",
      "next-env.d.ts",
    ],
  },
  {
    rules: {
      // Pas de console.log en prod (warn autorise console.warn/error)
      "no-console": ["warn", { allow: ["warn", "error"] }],
      // Variables inutilisées = erreur (ignore les prefixés par _)
      "@typescript-eslint/no-unused-vars": [
        "error",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
      ],
      // Pas de any explicite
      "@typescript-eslint/no-explicit-any": "warn",
      // Préférer const
      "prefer-const": "error",
      // Pas de var
      "no-var": "error",
      // Pas de conditions toujours vraies/fausses
      "no-constant-condition": "error",
      // Pas de code après return/throw
      "no-unreachable": "error",
      // Hooks React bien utilisés
      "react-hooks/exhaustive-deps": "warn",
      "react-hooks/rules-of-hooks": "error",
    },
  },
];

export default eslintConfig;
