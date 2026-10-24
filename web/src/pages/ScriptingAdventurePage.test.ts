import { cleanup, render, screen } from "@testing-library/svelte";
import { afterEach, describe, expect, it } from "vitest";
import ScriptingAdventurePage from "./ScriptingAdventurePage.svelte";

describe("scripting adventure page", () => {
    afterEach(cleanup);

    it("shows the development status using the adventure page layout", () => {
        render(ScriptingAdventurePage);

        expect(
            screen.getByRole("heading", { name: "Under development" }),
        ).toBeTruthy();
    });
});
