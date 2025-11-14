# syntax=docker/dockerfile:1.7

# --- Build stage --------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-24 AS build
WORKDIR /workspace
# Copiar todo el monorepo para compilar desktop con dependencias
COPY . .
# Construir desktop con todas las dependencias en un único JAR
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests -pl edufeed-desktop -am package

# --- Runtime stage con GUI (VNC + noVNC) --------------------------------------
FROM ubuntu:22.04

# Evitar prompts interactivos durante instalación
ENV DEBIAN_FRONTEND=noninteractive \
    DISPLAY=:1 \
    VNC_PORT=5901 \
    NOVNC_PORT=6080 \
    VNC_PASSWORD=edufeed123

# Instalar dependencias: Java con JavaFX, X11, VNC, noVNC, gestor de ventanas
RUN apt-get update && apt-get install -y \
    # Java Runtime CON JavaFX (openjfx incluye las libs nativas)
    openjdk-21-jre openjfx \
    # X11 y librerías gráficas
    xvfb x11vnc fluxbox \
    # noVNC para acceso web
    novnc websockify \
    # Utilidades
    wget curl supervisor \
    # Librerías nativas adicionales para JavaFX
    libgtk-3-0 libgl1-mesa-glx libxrender1 libxtst6 libxi6 \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root
RUN useradd -m -s /bin/bash edufeed && \
    mkdir -p /app /home/edufeed/.vnc && \
    chown -R edufeed:edufeed /app /home/edufeed

WORKDIR /app

# Copiar SOLO el JAR shaded (con todas las dependencias)
COPY --from=build --chown=edufeed:edufeed \
    /workspace/edufeed-desktop/target/edufeed-desktop-shaded.jar /app/edufeed-desktop.jar

# Script de inicio para VNC + Desktop app
COPY --chown=edufeed:edufeed edufeed-desktop/start-desktop.sh /app/start-desktop.sh
RUN chmod +x /app/start-desktop.sh

# Configuración de supervisord para manejar múltiples procesos
COPY edufeed-desktop/supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Exponer puertos: VNC directo (5901) y noVNC web (6080)
EXPOSE 5901 6080

USER edufeed

# Iniciar supervisord que manejará Xvfb, VNC, noVNC y la app
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
