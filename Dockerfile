FROM node:24-alpine AS frontend-build
WORKDIR /workspace/web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

FROM eclipse-temurin:25-jdk-alpine-3.24 AS backend-build
WORKDIR /workspace
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY app/ app/
COPY --from=frontend-build /workspace/web/dist/ app/src/main/resources/static/
RUN chmod +x gradlew && ./gradlew :app:bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine-3.24
WORKDIR /app
ENV SERVER_PORT=4200
EXPOSE 4200
COPY --from=backend-build --chown=10001:10001 /workspace/app/build/libs/app-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
