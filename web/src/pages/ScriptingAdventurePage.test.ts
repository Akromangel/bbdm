import { cleanup, render, screen } from "@testing-library/svelte";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ScriptingAdventurePage from "./ScriptingAdventurePage.svelte";

class MockEventSource {
    static instances: MockEventSource[] = [];

    onopen: (() => void) | null = null;
    onerror: (() => void) | null = null;
    listeners = new Map<string, (event: Event) => void>();
    closed = false;

    constructor(readonly url: string) {
        MockEventSource.instances.push(this);
    }

    addEventListener(type: string, listener: (event: Event) => void) {
        this.listeners.set(type, listener);
    }

    open() {
        this.onopen?.();
    }

    emit(type: string, data: object) {
        this.listeners.get(type)?.(
            new MessageEvent(type, { data: JSON.stringify(data) }),
        );
    }

    close() {
        this.closed = true;
    }
}

describe("scripting adventure page", () => {
    beforeEach(() => {
        MockEventSource.instances = [];
        vi.stubGlobal("EventSource", MockEventSource);
        vi.stubGlobal("crypto", { randomUUID: () => "session-1" });
        vi.stubGlobal(
            "fetch",
            vi.fn().mockResolvedValue({
                ok: true,
                json: async () => ({ sessionId: "session-1" }),
            }),
        );
    });

    afterEach(() => {
        cleanup();
        vi.useRealTimers();
        vi.unstubAllGlobals();
    });

    it("shows the six-second countdown when the page opens", () => {
        render(ScriptingAdventurePage);

        expect(screen.getByText("6")).toBeTruthy();
    });

    it("connects before starting the session and displays newest SSE updates first", async () => {
        vi.useFakeTimers();
        render(ScriptingAdventurePage);

        await vi.advanceTimersByTimeAsync(6000);
        const eventSource = MockEventSource.instances[0];
        expect(eventSource.url).toBe(
            "/api/scripting-adventure/sessions/session-1/events?apiVersion=1.0",
        );
        expect(fetch).not.toHaveBeenCalled();

        eventSource.open();
        expect(fetch).not.toHaveBeenCalled();
        eventSource.emit("connected", { sessionId: "session-1" });
        expect(fetch).toHaveBeenCalledOnce();

        eventSource.emit("state", {
            sessionId: "session-1",
            gold: 12,
            score: 42,
            lives: 3,
            level: 1,
            turn: 1,
            hpotCount: 0,
            task: "Find the map",
            message: "The trail is clear.",
            complete: false,
            error: "",
        });
        eventSource.emit("state", {
            sessionId: "session-1",
            gold: 7,
            score: 78,
            lives: 1,
            level: 2,
            turn: 2,
            hpotCount: 1,
            task: "Buy Healing pot",
            message: "Bought Healing pot. The dragon is defeated.",
            complete: true,
            error: "",
        });

        expect(
            await screen.findByText(
                "Bought Healing pot. The dragon is defeated.",
            ),
        ).toBeTruthy();
        expect(
            Array.from(
                document.querySelectorAll(".event-heading strong"),
                (element) => element.textContent,
            ),
        ).toEqual(["Buy Healing pot", "Find the map"]);
        expect(eventSource.closed).toBe(true);
    });

    it("displays the score from adventure state updates", async () => {
        vi.useFakeTimers();
        render(ScriptingAdventurePage);

        await vi.advanceTimersByTimeAsync(6000);
        const eventSource = MockEventSource.instances[0];
        eventSource.emit("state", {
            sessionId: "session-1",
            gold: 12,
            score: 425,
            lives: 3,
            level: 1,
            turn: 1,
            hpotCount: 0,
            task: "Find the map",
            message: "The trail is clear.",
            complete: false,
            error: "",
        });

        expect(await screen.findByText("✦ 425 score")).toBeTruthy();
    });

    it("keeps only the 20 newest updates and emphasizes the newest message", async () => {
        vi.useFakeTimers();
        render(ScriptingAdventurePage);

        await vi.advanceTimersByTimeAsync(6000);
        const eventSource = MockEventSource.instances[0];
        for (let turn = 1; turn <= 21; turn += 1) {
            eventSource.emit("state", {
                sessionId: "session-1",
                gold: 12,
                score: turn,
                lives: 3,
                level: 1,
                turn,
                hpotCount: 0,
                task: `Task ${turn}`,
                message: `Update ${turn}`,
                complete: false,
                error: "",
            });
        }

        await screen.findByText("Update 21");
        const events = Array.from(
            document.querySelectorAll(".adventure-event"),
        );
        expect({
            count: events.length,
            newestMessage: events[0]?.querySelector("p")?.textContent,
            newestEmphasized: events[0]?.classList.contains(
                "adventure-event-newest",
            ),
            oldestVisibleMessage:
                events[events.length - 1]?.querySelector("p")?.textContent,
        }).toEqual({
            count: 20,
            newestMessage: "Update 21",
            newestEmphasized: true,
            oldestVisibleMessage: "Update 2",
        });
    });
});
