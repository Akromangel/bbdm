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
{:else if window.location.pathname === "/scripting-adventure"}
    {#await import("./pages/ScriptingAdventurePage.svelte")}
        <main class="loading"><span class="spinner"></span><p>Opening the scripting adventure…</p></main>
    {:then module}
        {@const ScriptingAdventurePage = module.default}
        <ScriptingAdventurePage />
    {:catch}
        <main class="loading">
            <p>The scripting adventure page could not be loaded.</p>
            <a class="primary-button" href="/scripting-adventure">Try again</a>
        </main>
    {/await}
{:else}
    <HomePage />
{/if}
