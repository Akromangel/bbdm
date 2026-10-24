# Agent guide

- Backend: Java 25, Spring Boot 4, onion architecture (`presentation` → `application` → `domain`; `infrastructure`
  implements domain ports). Do not use Java `var`; PMD enforces explicit local variable types. Add or update tests with
  behavior changes; keep ArchUnit rules passing.
- Unit tests that use mocks must include an `@AfterEach` method that calls `verifyNoMoreInteractions` for every mock.
- Keep dependency, plugin, and tool versions in `gradle/libs.versions.toml`; use Spring Boot-managed versions without
  explicit versions, and read every non-Spring-managed dependency version from the catalog.
- API routes use `/api/...` and the `API-Version: 1.0` header. Keep upstream `/api/v2/...` paths in the infrastructure
  client.
- Logging: Use SLF4J's fluent chain (`log.atDebug().addKeyValue(...).setMessage(...).log()`) consistently throughout
  the application, prefer structured key-value fields, and use suppliers for computed or potentially expensive values.
  Upstream HTTP request and response logging is handled by `UpstreamLoggingInterceptor` on the configured `RestClient`;
  it logs method, URI, status, and body at debug level. Spring's `RestClient.Builder.bufferContent` buffers and replays
  response bodies so the client can deserialize them normally.
- Frontend: Svelte in `web/`. Use four spaces, never tabs; Biome runs in the Husky pre-commit hook.
- Verify backend with `./gradlew :app:test` (`.\gradlew.bat :app:test` on Windows). Verify frontend with `npm run check`
  and `npm run build` from `web/`.
- Write commit messages in Commitizen-style Conventional Commits format: `type(scope): description`.
