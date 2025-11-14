#!/bin/bash
set -e

echo "[EduFeed Desktop] Iniciando servidor VNC..."

# Configurar password VNC
mkdir -p ~/.vnc
echo "$VNC_PASSWORD" | x11vnc -storepasswd /home/edufeed/.vnc/passwd

# Iniciar Xvfb (X virtual framebuffer)
Xvfb :1 -screen 0 1920x1080x24 &
XVFB_PID=$!
echo "[EduFeed Desktop] Xvfb iniciado (PID: $XVFB_PID)"

# Esperar que X esté listo
sleep 2

# Iniciar gestor de ventanas fluxbox
fluxbox &
echo "[EduFeed Desktop] Fluxbox iniciado"

# Iniciar servidor VNC
x11vnc -display :1 -rfbport 5901 -rfbauth /home/edufeed/.vnc/passwd -forever -shared &
echo "[EduFeed Desktop] VNC servidor en puerto 5901"

# Iniciar noVNC para acceso web
websockify --web=/usr/share/novnc/ 6080 localhost:5901 &
echo "[EduFeed Desktop] noVNC disponible en puerto 6080"

# Esperar que todo esté listo
sleep 3

echo "[EduFeed Desktop] Iniciando aplicación JavaFX..."
# Iniciar aplicación desktop
java -jar /app/edufeed-desktop.jar

# Mantener contenedor vivo
wait $XVFB_PID
