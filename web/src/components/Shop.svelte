<script lang="ts">
    import {
        fetchShop,
        purchaseItem,
        ApiProblemError,
        type ActionOutcome,
        type GameState,
        type ShopItem,
    } from "../lib/game-api";
    import type { ToastMessage } from "./Toaster.svelte";

    export let game: GameState;
    export let busy = false;
    export let gameOver = false;
    export let onGameUpdate: (outcome: ActionOutcome) => void;
    export let onToast: (type: ToastMessage["type"], message: string) => void;
    export let onGameOver: () => void;

    let shopItems: ShopItem[] = [];
    let showShop = false;

    function errorMessage(cause: unknown): string {
        return cause instanceof Error ? cause.message : String(cause);
    }

    function markGameOver(cause: unknown) {
        if (cause instanceof ApiProblemError && cause.status === 410) {
            onGameOver();
        }
    }

    async function loadShop() {
        showShop = true;
        try {
            shopItems = await fetchShop(game.gameId);
        } catch (cause) {
            markGameOver(cause);
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
            markGameOver(cause);
            onToast("error", `The purchase could not be completed. ${errorMessage(cause)}`);
        } finally {
            busy = false;
        }
    }
</script>

<button class="shop-button" disabled={gameOver} on:click={loadShop}><span>◈</span> Visit the shop</button>

{#if showShop}
    <div class="modal-backdrop" role="presentation"
         on:click={(event) => event.target === event.currentTarget && (showShop = false)}>
        <div class="shop-modal" role="dialog" aria-modal="true" aria-labelledby="shop-heading" tabindex="-1">
            <button class="close-button" aria-label="Close shop" on:click={() => (showShop = false)}>×</button>
            <p class="eyebrow">A well-stocked merchant</p>
            <h2 id="shop-heading">The shop <span class="gold-symbol">◈</span></h2>
            {#if shopItems.length}
                <div class="shop-grid">
                    {#each shopItems as item}
                        <article class="shop-card">
                            <h3>{item.name ?? item.id}</h3>
                            <p class="item-price">{item.cost != null ? `${item.cost} gold` : "Price unavailable"}</p>
                            <button
                                class="buy-button"
                                disabled={gameOver || busy || item.cost == null || item.cost > (game.gold ?? 0)}
                                on:click={() => buyItem(item)}
                            >Buy</button>
                        </article>
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

    .shop-modal {
        width: min(900px, 100%);
    }

    .shop-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 12px;
    }

    .shop-card {
        display: flex;
        min-width: 0;
        flex-direction: column;
        align-items: flex-start;
        gap: 10px;
        padding: 14px;
        border: 1px solid #d4cbb6;
    }

    .shop-card h3 {
        margin: 0;
        overflow-wrap: anywhere;
        font:
            500 16px "Playfair Display",
            serif;
    }

    .item-price {
        margin: 0;
        color: #6f796d;
        font-size: 12px;
    }

    .shop-empty {
        color: #6f796d;
        font-size: 11px;
    }

    .buy-button {
        margin-top: auto;
        padding: 8px 13px;
        border: 0;
        background: #253a2d;
        color: var(--paper);
        font-size: 11px;
    }

    .buy-button:disabled {
        cursor: not-allowed;
    }

    .shop-empty {
        margin-bottom: 0;
    }

    @media (max-width: 640px) {
        .shop-grid {
            grid-template-columns: repeat(2, minmax(0, 1fr));
        }
    }

    @media (max-width: 420px) {
        .shop-grid {
            grid-template-columns: 1fr;
        }
    }
</style>
