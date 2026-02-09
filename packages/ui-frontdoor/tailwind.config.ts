import type { Config } from "tailwindcss";
import sharedConfig from "../ui-shared/tailwind.config";

export default {
  ...(sharedConfig as Config),
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
} satisfies Config;
