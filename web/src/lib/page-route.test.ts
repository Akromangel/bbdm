import { describe, expect, it } from "vitest";
import { resolvePageRoute } from "./page-route";

describe("page route", () => {
    it("resolves supported paths and falls back to home", () => {
        expect([
            resolvePageRoute("/"),
            resolvePageRoute("/game"),
            resolvePageRoute("/scripting-adventure"),
            resolvePageRoute("/unknown"),
        ]).toEqual(["home", "game", "scripting-adventure", "home"]);
    });
});
