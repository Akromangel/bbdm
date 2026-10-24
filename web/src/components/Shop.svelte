<script lang="ts">
    import {
        fetchShop,
        purchaseItem,
        type ActionOutcome,
        type GameState,
        type ShopItem,
    } from "../lib/game-api";
    import type { ToastMessage } from "./Toaster.svelte";

    export let game: GameState;
    export let busy = false;
    export let onGameUpdate: (outcome: ActionOutcome) => void;
    export let onToast: (type: ToastMessage["type"], message: string) => void;

    let shopItems: ShopItem[] = [];
    let showShop = false;

    function errorMessage(cause: unknown): string {
        return cause instanceof Error ? cause.message : String(cause);
    }

    async function loadShop() {
        showShop = true;
        try {
            shopItems = await fetchShop(game.gameId);
        } catch (cause) {
            onToast("error", `The shop could not be loaded. ${errorMessage(cause)}`);
        }
    }

    async function buyItem(item: ShopItem) {
        if (busy) {
            return;
        }
        busy = true;
        try {
            const outcome = await purchaseItem(game.gameId, item);
            onGameUpdate(outcome);
            onToast("info", outcome.message ?? "Purchase complete.");
            try {
                shopItems = await fetchShop(game.gameId);
            } catch (cause) {
                onToast(
                    "error",
                    `Your purchase was recorded, but the shop could not be refreshed. ${errorMessage(cause)}`,
                );
            }
        } catch (cause) {
            onToast("error", `The purchase could not be completed. ${errorMessage(cause)}`);
        } finally {
            busy = false;
        }
    }
</script>

<button class="shop-button" on:click={loadShop}><span>◈</span> Visit the shop</button>

{#if showShop}
    <div class="modal-backdrop" role="presentation"
         on:click={(event) => event.target === event.currentTarget && (showShop = false)}>
        <div class="shop-modal" role="dialog" aria-modal="true" aria-labelledby="shop-heading" tabindex="-1">
            <button class="close-button" aria-label="Close shop" on:click={() => (showShop = false)}>×</button>
            <p class="eyebrow">A well-stocked merchant</p>
            <h2 id="shop-heading">The shop <span class="gold-symbol">◈</span></h2>
            {#if shopItems.length}
                <div class="shop-list">
                    {#each shopItems as item}
                        <div class="shop-item">
                            <div>
                                <strong>{item.name ?? item.id}</strong><span>{item.cost ? `${item.cost} gold` : item.description ?? "Useful for the journey"}</span>
                            </div>
                            <button class="buy-button" disabled={busy} on:click={() => buyItem(item)}>Buy</button>
                        </div>
                    {/each}
                </div>
            {:else}
                <p class="shop-empty">No wares are on display at the moment.</p>
            {/if}
        </div>
    </div>
{/if}

<style>
    .shop-button {
        padding: 11px 15px;
        border: 1px solid #526457;
        background: transparent;
        color: var(--paper);
        font-size: 12px;
    }

    .shop-button span {
        margin-right: 8px;
        color: var(--gold);
    }

    .shop-button:hover {
        border-color: var(--gold);
    }

    .shop-modal h2 {
        margin-bottom: 24px;
    }

    .shop-list {
        display: grid;
        gap: 9px;
    }

    .shop-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 15px;
        padding: 13px;
        border: 1px solid #d4cbb6;
    }

    .shop-item div {
        display: grid;
        gap: 5px;
    }

    .shop-item strong {
        font:
            500 16px "Playfair Display",
            serif;
    }

    .shop-item span,
    .shop-empty {
        color: #6f796d;
        font-size: 11px;
    }

    .buy-button {
        padding: 8px 13px;
        border: 0;
        background: #253a2d;
        color: var(--paper);
        font-size: 11px;
    }

    .shop-empty {
        margin-bottom: 0;
    }
</style>
