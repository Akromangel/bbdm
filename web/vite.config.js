import { svelte } from "@sveltejs/vite-plugin-svelte";
import { defineConfig } from "vitest/config";

export default defineConfig({
    plugins: [svelte()],
    resolve: {
        conditions: ["browser"],
    },
    server: {
        proxy: {
            "/api": "http://localhost:8080",
        },
    },
    test: {
        environment: "happy-dom",
        coverage: {
            provider: "v8",
            include: ["src/**/*.{js,ts,svelte}"],
            exclude: ["src/**/*.test.{js,ts,svelte}"],
            reporter: ["text", "html"],
        },
    },
});
