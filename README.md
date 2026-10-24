# Visual Adventure

A Svelte adventure dashboard for Dragons of Mugloar, backed by a Java and Spring Boot 4 API proxy.

## Run with Docker

Build and start the full application from the repository root:

```shell
docker build -t bbdm .
docker run --rm -p 4200:4200 bbdm
```

Open <http://localhost:4200>. The container serves the web app and its `/api` endpoints from the same port.

## Local development

The backend requires Java 25. The repository includes a `.sdkmanrc` for SDKMAN; enable automatic environment switching
by
setting `sdkman_auto_env=true` in `~/.sdkman/etc/config`, then run `sdk env` from the repository root to install or
select
the required Java version.

Start the backend from the repository root with the `local` Spring profile. This loads `application-local.yaml`, which
enables debug logging:

```shell
./gradlew :app:bootRun --args='--spring.profiles.active=local'
```

When launching the backend from an IDE, set the active Spring profile to `local` in the run configuration. Without this
profile, the application uses the default `INFO` log level.

In a second terminal, start the web application:

```shell
cd web
npm install
npm run dev
```

Open the Vite URL shown in the terminal and choose **Visual adventure**. The web app proxies `/api` requests to
`http://localhost:8080`; set `dragonsofmugloar.api.base-url` to override the upstream API origin.

## API proxy

The backend exposes game endpoints under `/api`, versioned with the `API-Version: 1.0` request header:

| Method | Endpoint                             | Purpose                  |
|--------|--------------------------------------|--------------------------|
| `POST` | `/api/game`                          | Start a game             |
| `GET`  | `/api/{gameId}/messages`             | Fetch available messages |
| `POST` | `/api/{gameId}/messages/{messageId}` | Accept or pass a message |
| `GET`  | `/api/{gameId}/shop`                 | Fetch shop inventory     |
| `POST` | `/api/{gameId}/shop/{itemId}`        | Purchase an item         |

Actions return the updated upstream game data directly, so the dashboard refreshes its statistics after each completed
action.

The Java backend follows an onion architecture: presentation depends on application, application depends on domain, and
infrastructure implements the domain gateway. ArchUnit tests enforce these layer boundaries.

## Frontend checks

```shell
cd web
npm run build
npm run check
npm run update:dependencies
```

Biome runs as the Husky pre-commit hook.

Check Gradle dependency updates from the repository root:

```shell
./gradlew :app:dependencyUpdates
```
