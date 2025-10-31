# syntax=docker/dockerfile:1.7

# --- Build stage --------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-24 AS build
WORKDIR /workspace
# Copiamos todo el monorepo para poder compilar el módulo backend con -am
COPY . .
# Construir solo el backend y sus dependencias (-am) sin tests
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests -pl edufeed-backend -am package

# --- Runtime stage ------------------------------------------------------------
FROM eclipse-temurin:24-jre
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=prod \
    TZ=America/Bogota

RUN useradd -r -u 1001 spring && \
    mkdir -p /app && chown -R spring:spring /app
WORKDIR /app

# Copiar jar del backend
COPY --from=build /workspace/edufeed-backend/target/edufeed-backend-*.jar /app/app.jar

EXPOSE 8080
USER spring
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
