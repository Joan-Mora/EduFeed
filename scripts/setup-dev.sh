#!/usr/bin/env bash
set -euo pipefail

# Preparación de entorno Unix (Linux/macOS)
# Requisitos: bash, docker, java 21, maven, code (opcional)

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"
ENV_EXAMPLE="$ROOT_DIR/.env.example"

log()  { echo -e "\033[1;36m[EduFeed]\033[0m $*"; }
ok()   { echo -e "\033[1;32m✔\033[0m $*"; }
warn() { echo -e "\033[1;33m⚠\033[0m $*"; }

# Copiar .env si no existe
if [[ ! -f "$ENV_FILE" && -f "$ENV_EXAMPLE" ]]; then
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  ok "Creado .env desde .env.example"
fi

# Chequeos básicos
command -v java >/dev/null 2>&1 || warn "Java no encontrado. Instala JDK 21."
command -v mvn  >/dev/null 2>&1 || warn "Maven no encontrado. Instálalo o usa Maven Wrapper."
command -v docker >/dev/null 2>&1 || warn "Docker no encontrado. Instálalo y arráncalo."

# Compilar (sin tests)
log "Compilando proyecto (sin tests)..."
( cd "$ROOT_DIR" && mvn -T1C -DskipTests package ) || warn "Compilación con errores"

log "Setup de entorno Unix finalizado. Para levantar DB: docker compose up -d"
