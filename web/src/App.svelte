<script>
    import HomePage from "./pages/HomePage.svelte";
</script>

{#if window.location.pathname === "/game"}
    {#await import("./pages/VisualAdventurePage.svelte")}
        <main class="loading"><span class="spinner"></span><p>Opening the field journal…</p></main>
    {:then module}
        {@const VisualAdventurePage = module.default}
        <VisualAdventurePage />
    {:catch}
        <main class="loading">
            <p>The adventure page could not be loaded.</p>
            <a class="primary-button" href="/game">Try again</a>
        </main>
    {/await}
{:else}
    <HomePage />
{/if}
