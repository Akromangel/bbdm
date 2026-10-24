import { cleanup, fireEvent, render, screen } from "@testing-library/svelte";
import { afterEach, describe, expect, it, vi } from "vitest";
import Toaster, { type ToastMessage } from "./Toaster.svelte";

describe("toaster", () => {
    afterEach(cleanup);

    it("displays informational toasts as status messages", () => {
        const toasts: ToastMessage[] = [
            { id: 1, type: "info", message: "Quest accepted." },
        ];

        render(Toaster, { props: { toasts, onDismiss: vi.fn() } });

        expect(screen.getByRole("status").textContent).toContain(
            "Quest accepted.",
        );
        expect(
            screen.getByRole("status").classList.contains("toast-error"),
        ).toBe(false);
        expect(
            screen.getByRole("status").querySelector(".toast-actions")
                ?.textContent,
        ).toContain("✦");
    });

    it("displays error toasts as alerts with error styling", () => {
        const toasts: ToastMessage[] = [
            { id: 2, type: "error", message: "The action failed." },
        ];

        render(Toaster, { props: { toasts, onDismiss: vi.fn() } });

        const toast = screen.getByRole("alert");
        expect(toast.textContent).toContain("The action failed.");
        expect(toast.classList.contains("toast-error")).toBe(true);
    });

    it("displays warning toasts as status messages with warning styling", () => {
        const toasts: ToastMessage[] = [
            { id: 4, type: "warning", message: "You have 1 life left." },
        ];

        render(Toaster, { props: { toasts, onDismiss: vi.fn() } });

        const toast = screen.getByRole("status");
        expect(toast.textContent).toContain("You have 1 life left.");
        expect(toast.classList.contains("toast-warning")).toBe(true);
        expect(toast.querySelector(".toast-actions")?.textContent).toContain(
            "⚠",
        );
    });

    it("calls the dismiss handler with the selected toast id", async () => {
        const onDismiss = vi.fn();
        const toasts: ToastMessage[] = [
            { id: 3, type: "info", message: "Purchase complete." },
        ];

        render(Toaster, { props: { toasts, onDismiss } });
        await fireEvent.click(
            screen.getByRole("button", { name: "Dismiss notification" }),
        );

        expect(onDismiss).toHaveBeenCalledWith(3);
    });
});
