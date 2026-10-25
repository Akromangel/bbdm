export type PageRoute = "home" | "game" | "scripting-adventure";

export function resolvePageRoute(pathname: string): PageRoute {
    if (pathname === "/game") {
        return "game";
    }
    if (pathname === "/scripting-adventure") {
        return "scripting-adventure";
    }
    return "home";
}
