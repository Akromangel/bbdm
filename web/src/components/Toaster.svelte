<script context="module" lang="ts">
    export type ToastMessage = {
        id: number;
        type: "info" | "warning" | "error";
        message: string;
    };
</script>

<script lang="ts">
    export let toasts: ToastMessage[] = [];
    export let onDismiss: (id: number) => void;
</script>

<div class="toast-stack" aria-live="polite" aria-relevant="additions">
    {#each toasts as toast (toast.id)}
        <div
            class="toast"
            class:toast-error={toast.type === "error"}
            class:toast-warning={toast.type === "warning"}
            role={toast.type === "error" ? "alert" : "status"}
        >
            <p>{toast.message}</p>
            <div class="toast-actions">
                {#if toast.type === "info"}<span aria-hidden="true">✦</span>{/if}
                {#if toast.type === "warning"}<span aria-hidden="true">⚠</span>{/if}
                <button aria-label="Dismiss notification" on:click={() => onDismiss(toast.id)}>×</button>
            </div>
        </div>
    {/each}
</div>

<style>
    .toast-stack {
        position: fixed;
        z-index: 4;
        top: 20px;
        right: 20px;
        display: grid;
        width: min(420px, calc(100vw - 40px));
        gap: 10px;
    }

    .toast {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 12px 15px;
        border: 1px solid #536e57;
        background: #203329;
        box-shadow: 0 8px 24px rgb(0 0 0 / 30%);
        color: #c9ddc6;
        font-size: 13px;
    }

    .toast p {
        min-width: 0;
        flex: 1;
        margin: 0;
    }

    .toast-actions {
        display: flex;
        flex: 0 0 auto;
        align-items: center;
        gap: 10px;
    }

    .toast-actions > span {
        display: inline-flex;
        flex: 0 0 auto;
        align-items: center;
        justify-content: center;
        font-family: "DM Sans", sans-serif;
        font-size: 13px;
        line-height: 1;
        color: var(--gold);
    }

    .toast-actions button {
        border: 0;
        background: transparent;
        color: inherit;
        font: inherit;
        font-size: 16px;
        line-height: 1;
    }

    .toast-error {
        border-color: #9e594e;
        background: #3d2422;
        color: #f0b3a8;
    }

    .toast-warning {
        border-color: #b8792d;
        background: #3d3021;
        color: #f2c477;
    }
</style>
