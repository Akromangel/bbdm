import { cleanup, render, screen } from "@testing-library/svelte";
import { afterEach, describe, expect, it } from "vitest";
import HomePage from "./HomePage.svelte";

describe("home page", () => {
    afterEach(cleanup);

    it("offers both adventures as page actions", () => {
        render(HomePage);

        expect(
            screen.getByRole("button", { name: /visual adventure/i }),
        ).toBeTruthy();
        expect(
            screen.getByRole("button", { name: /scripting adventure/i }),
        ).toBeTruthy();
        expect(screen.getAllByRole("button")).toHaveLength(2);
        expect(screen.queryByRole("link")).toBeNull();
    });
});
