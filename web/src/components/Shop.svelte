<script lang="ts">
    import {
        gameSession,
        loadShop as fetchShopInventory,
        purchaseShopItem,
    } from "../lib/game-session";
    import type { ShopItem } from "../lib/game-api";
    import type { ToastMessage } from "./Toaster.svelte";

    export let onToast: (type: ToastMessage["type"], message: string) => void;

    let showShop = false;

    function errorMessage(cause: unknown): string {
        return cause instanceof Error ? cause.message : String(cause);
    }

    async function loadShop() {
        showShop = true;
        try {
            await fetchShopInventory();
        } catch (cause) {
            onToast("error", `The shop could not be loaded. ${errorMessage(cause)}`);
        }
    }

    async function buyItem(item: ShopItem) {
        if ($gameSession.busy) {
            return;
        }
        try {
            const result = await purchaseShopItem(item);
            if (!result) {
                return;
            }
            onToast("info", result.outcome.message ?? "Purchase complete.");
            if (result.refreshError !== undefined) {
                onToast(
                    "error",
                    `Your purchase was recorded, but the shop could not be refreshed. ${errorMessage(result.refreshError)}`,
                );
            }
        } catch (cause) {
            onToast("error", `The purchase could not be completed. ${errorMessage(cause)}`);
        }
    }
</script>

<button class="shop-button" disabled={$gameSession.gameOver} on:click={loadShop}><span>◈</span> Visit the shop</button>

{#if showShop}
    <div class="modal-backdrop" role="presentation"
         on:click={(event) => event.target === event.currentTarget && (showShop = false)}>
        <div class="shop-modal" role="dialog" aria-modal="true" aria-labelledby="shop-heading" tabindex="-1">
            <button class="close-button" aria-label="Close shop" on:click={() => (showShop = false)}>×</button>
            <p class="eyebrow">A well-stocked merchant</p>
            <h2 id="shop-heading">The shop <span class="gold-symbol">◈</span></h2>
            {#if $gameSession.shopItems.length}
                <div class="shop-grid">
                    {#each $gameSession.shopItems as item}
                        <article class="shop-card">
                            <h3>{item.name ?? item.id}</h3>
                            <p class="item-price">{item.cost != null ? `${item.cost} gold` : "Price unavailable"}</p>
                            <button
                                class="buy-button"
                                disabled={$gameSession.gameOver || $gameSession.busy || item.cost == null || item.cost > ($gameSession.game?.gold ?? 0)}
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
