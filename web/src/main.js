import { mount } from "svelte";
import App from "./App.svelte";
import { resolvePageRoute } from "./lib/page-route";
import "./style.css";

mount(App, {
    target: document.getElementById("app"),
    props: { page: resolvePageRoute(window.location.pathname) },
});
