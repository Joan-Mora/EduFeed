# Fase 8.1: Dockerización

## Índice

1. [Introducción](#introducción)
2. [Arquitectura de contenedores](#arquitectura-de-contenedores)
3. [Componentes](#componentes)
4. [Configuración de entornos](#configuración-de-entornos)
5. [Construcción de imágenes](#construcción-de-imágenes)
6. [Despliegue](#despliegue)
7. [Resolución de problemas](#resolución-de-problemas)
8. [Mejores prácticas](#mejores-prácticas)

---

## Introducción

La fase de dockerización transforma la aplicación EduFeed de un sistema multi-módulo Maven ejecutado localmente en un conjunto de servicios containerizados listos para despliegue en cualquier entorno (desarrollo, staging, producción).

### Objetivos

- ✅ Containerizar el backend Spring Boot con Dockerfile multi-stage
- ✅ Orquestar servicios (PostgreSQL, pgAdmin, backend) con Docker Compose
- ✅ Establecer configuración por entornos (.env.dev, .env.stage, .env.prod)
- ✅ Garantizar portabilidad y consistencia entre dev/stage/prod
- ✅ Optimizar tamaño de imagen y tiempos de build

### Beneficios

- **Portabilidad**: "funciona en mi máquina" → "funciona en todas las máquinas"
- **Aislamiento**: dependencias y configuración encapsuladas
- **Escalabilidad**: base para orquestación (Kubernetes, Swarm, ECS)
- **CI/CD**: integración directa con pipelines de despliegue
- **Consistencia**: mismo runtime Java/DB en dev, stage y prod

---

## Arquitectura de contenedores

```
┌─────────────────────────────────────────────────────────┐
│                     Docker Host                         │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   pgAdmin    │  │   Backend    │  │  PostgreSQL  │  │
│  │ dpage/pgadmin│  │ edufeed:prod │  │postgres:16.4 │  │
│  │  Port: 5052  │  │  Port: 8080  │  │  Port: 5432  │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                 │          │
│         └─────────────────┴─────────────────┘          │
│                    Docker Network                      │
│                  (bridge por defecto)                  │
└─────────────────────────────────────────────────────────┘
```

### Red y comunicación

- **Red bridge por defecto**: contenedores se resuelven por nombre de servicio
- Backend conecta a PostgreSQL vía `jdbc:postgresql://db:5432/edufeed`
- pgAdmin accede a DB usando hostname `db` (nombre del servicio en compose)

### Volúmenes persistentes

- `db_data_prod`: datos de PostgreSQL (/var/lib/postgresql/data)
- `pgadmin_data_prod`: configuración y conexiones de pgAdmin

---

## Componentes

### 1. Dockerfile Backend (multi-stage)

**Ubicación**: `edufeed-backend/Dockerfile`

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:24-jdk AS builder
WORKDIR /workspace
COPY pom.xml .
COPY edufeed-backend/pom.xml edufeed-backend/
COPY edufeed-common/pom.xml edufeed-common/
COPY edufeed-biometric/pom.xml edufeed-biometric/
COPY edufeed-desktop/pom.xml edufeed-desktop/
COPY edufeed-common/target edufeed-common/target
COPY edufeed-biometric/target edufeed-biometric/target
COPY edufeed-backend/src edufeed-backend/src
RUN apt-get update && apt-get install -y maven && \
    mvn -f edufeed-backend/pom.xml clean package -DskipTests && \
    mkdir -p /app && \
    mv edufeed-backend/target/*.jar /app/app.jar

# Stage 2: Runtime
FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=builder /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Características**:
- **Multi-stage**: build en JDK 24, runtime en JRE 24 (imagen más pequeña)
- **Cache de dependencias**: copia pom.xml primero para aprovechar layer caching
- **Módulos comunes**: incluye edufeed-common y edufeed-biometric compilados
- **Puerto expuesto**: 8080 (configurable con env var PORT)

### 2. Docker Compose Producción

**Ubicación**: `docker-compose.prod.yml`

```yaml
services:
  db:
    image: postgres:16.4-alpine
    container_name: edufeed-db-prod
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - db_data_prod:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER}"]
      interval: 10s
      timeout: 5s
      retries: 10
    env_file:
      - .env.prod

  pgadmin:
    image: dpage/pgadmin4:8.12
    container_name: edufeed-pgadmin-prod
    depends_on:
      - db
    ports:
      - "${PGADMIN_PORT:-5052}:80"
    environment:
      PGADMIN_DEFAULT_EMAIL: ${PGADMIN_DEFAULT_EMAIL}
      PGADMIN_DEFAULT_PASSWORD: ${PGADMIN_DEFAULT_PASSWORD}
    volumes:
      - pgadmin_data_prod:/var/lib/pgadmin
    env_file:
      - .env.prod

  backend:
    build:
      context: .
      dockerfile: edufeed-backend/Dockerfile
    image: edufeed-backend:latest
    container_name: edufeed-backend-prod
    depends_on:
      db:
        condition: service_healthy
    ports:
      - "${PORT:-8080}:8080"
    environment:
      DB_URL: ${DB_URL}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      PORT: ${PORT}
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      JWT_ISSUER: ${JWT_ISSUER:-EduFeed}
      JWT_SECRET: ${JWT_SECRET}
      APP_TIMEZONE: ${APP_TIMEZONE:-America/Bogota}
      BIOMETRIC_ENCRYPTION_KEY: ${BIOMETRIC_ENCRYPTION_KEY}
      SEED_OPERATOR_ENABLED: ${SEED_OPERATOR_ENABLED:-false}
      SPRING_FLYWAY_BASELINE_ON_MIGRATE: ${SPRING_FLYWAY_BASELINE_ON_MIGRATE:-true}
      SPRING_FLYWAY_BASELINE_VERSION: ${SPRING_FLYWAY_BASELINE_VERSION:-0}
    env_file:
      - .env.prod

volumes:
  db_data_prod:
  pgadmin_data_prod:
```

**Características**:
- **Healthcheck**: backend espera a que DB esté `healthy` antes de iniciar
- **env_file**: carga variables desde `.env.prod` (secretos fuera del repo)
- **depends_on con condition**: garantiza orden de arranque correcto
- **Nombres explícitos**: container_name para referencia en scripts/logs

---

## Configuración de entornos

### Archivo .env.dev (desarrollo local)

**Ubicación**: `.env.dev` (versionado en Git)

```bash
# Database
POSTGRES_DB=edufeed
POSTGRES_USER=edufeed
POSTGRES_PASSWORD=edufeed
POSTGRES_PORT=5432

# pgAdmin
PGADMIN_PORT=5050
PGADMIN_DEFAULT_EMAIL=joanmora07@hotmail.com
PGADMIN_DEFAULT_PASSWORD=admin123

# Backend
PORT=8080
DB_URL=jdbc:postgresql://db:5432/edufeed
DB_USER=edufeed
DB_PASSWORD=edufeed
SPRING_PROFILES_ACTIVE=dev

# Security (dev - valores de ejemplo)
JWT_SECRET=dev-secret-key-not-for-production-use-only
BIOMETRIC_ENCRYPTION_KEY=dev-biometric-key-32-chars-min

# App
APP_TIMEZONE=America/Bogota
SEED_OPERATOR_ENABLED=true
```

### Archivo .env.stage (staging)

**Ubicación**: `.env.stage` (versionado en Git, credenciales no sensibles)

```bash
POSTGRES_DB=edufeed_stage
POSTGRES_USER=edufeed_stage
POSTGRES_PASSWORD=CHANGE_ME_STAGING
POSTGRES_PORT=5433

PGADMIN_PORT=5051
PGADMIN_DEFAULT_EMAIL=admin@edufeed.stage
PGADMIN_DEFAULT_PASSWORD=CHANGE_ME

PORT=8081
DB_URL=jdbc:postgresql://db:5432/edufeed_stage
DB_USER=edufeed_stage
DB_PASSWORD=CHANGE_ME_STAGING
SPRING_PROFILES_ACTIVE=staging

JWT_SECRET=CHANGE_ME_JWT_STAGING
BIOMETRIC_ENCRYPTION_KEY=CHANGE_ME_BIO_STAGING

APP_TIMEZONE=America/Bogota
SEED_OPERATOR_ENABLED=true
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
SPRING_FLYWAY_BASELINE_VERSION=0
```

### Archivo .env.prod.example (plantilla producción)

**Ubicación**: `.env.prod.example` (versionado), `.env.prod` (NO versionado)

```bash
# IMPORTANTE: Copiar a .env.prod y llenar con valores reales
# NO SUBIR .env.prod al repositorio (ya está en .gitignore)

# Database
POSTGRES_DB=edufeed_prod
POSTGRES_USER=edufeed_prod
POSTGRES_PASSWORD=
POSTGRES_PORT=5432

# pgAdmin
PGADMIN_PORT=5052
PGADMIN_DEFAULT_EMAIL=
PGADMIN_DEFAULT_PASSWORD=

# Backend
PORT=8080
DB_URL=jdbc:postgresql://db:5432/edufeed_prod
DB_USER=edufeed_prod
DB_PASSWORD=
SPRING_PROFILES_ACTIVE=prod

# Security (GENERAR VALORES SEGUROS)
# openssl rand -base64 32
JWT_SECRET=
BIOMETRIC_ENCRYPTION_KEY=

# App
APP_TIMEZONE=America/Bogota
SEED_OPERATOR_ENABLED=false

# Flyway
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
SPRING_FLYWAY_BASELINE_VERSION=0
```

**Generar secretos seguros**:

```powershell
# PowerShell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object {[char]$_})
```

```bash
# Linux/Mac
openssl rand -base64 32
```

### .gitignore

Asegurar que `.env.prod` NO se versione:

```gitignore
.env.prod
*.env.local
```

---

## Construcción de imágenes

### Build local (desarrollo)

```powershell
# Desde raíz del proyecto
docker compose --env-file .env.dev -f docker-compose.yml build
```

### Build producción

```powershell
# 1. Compilar módulos comunes primero (edufeed-common, edufeed-biometric)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
& "$env:USERPROFILE\tools\maven\apache-maven-3.9.9\bin\mvn.cmd" clean install -DskipTests

# 2. Build imagen backend
docker compose --env-file .env.prod -f docker-compose.prod.yml build backend

# 3. Tag para registry (opcional)
docker tag edufeed-backend:latest ghcr.io/joan-mora/edufeed-backend:latest
docker tag edufeed-backend:latest ghcr.io/joan-mora/edufeed-backend:v1.0.0
```

### Optimización de build

**Cache de Maven**: montar .m2 como volumen para acelerar builds repetidos:

```yaml
# docker-compose.build.yml (opcional)
services:
  builder:
    image: eclipse-temurin:24-jdk
    volumes:
      - .:/workspace
      - maven-cache:/root/.m2
    working_dir: /workspace
    command: mvn clean package -DskipTests

volumes:
  maven-cache:
```

**BuildKit**: habilitar para builds paralelos y mejor cache:

```powershell
$env:DOCKER_BUILDKIT=1
docker build -f edufeed-backend/Dockerfile -t edufeed-backend:latest .
```

---

## Despliegue

### Desarrollo local

```powershell
# Levantar stack completo
docker compose --env-file .env.dev -f docker-compose.yml up -d

# Ver logs
docker compose logs -f backend

# Verificar salud
docker ps
docker exec edufeed-db pg_isready -U edufeed

# Acceso
# Backend: http://localhost:8080
# pgAdmin: http://localhost:5050
```

### Staging

```powershell
# En servidor staging
cd /opt/edufeed-staging

# Copiar .env.stage a .env.prod (o editar directamente)
cp .env.stage .env.prod

# Pull código
git pull origin main

# Build y deploy
docker compose --env-file .env.prod -f docker-compose.prod.yml build
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d

# Verificar migraciones Flyway
docker compose logs backend | grep -i flyway

# Backend: http://staging.edufeed.com:8081
# pgAdmin: http://staging.edufeed.com:5051
```

### Producción

```powershell
# En servidor producción
cd /opt/edufeed

# IMPORTANTE: .env.prod debe existir con secretos reales
# Ver sección "Configuración de entornos"

# Pull última versión
git pull origin main

# Build (o pull desde GHCR si usas CI/CD)
docker compose --env-file .env.prod -f docker-compose.prod.yml pull backend
# O: docker compose --env-file .env.prod -f docker-compose.prod.yml build backend

# Deploy con zero-downtime (recrear solo servicios cambiados)
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps backend

# Verificar
curl http://localhost:8080/actuator/health

# Backend: http://edufeed.com:8080
# pgAdmin: http://edufeed.com:5052
```

### Comandos útiles

```powershell
# Detener servicios
docker compose --env-file .env.prod -f docker-compose.prod.yml down

# Detener y eliminar volúmenes (CUIDADO: borra datos)
docker compose --env-file .env.prod -f docker-compose.prod.yml down -v

# Reiniciar solo backend
docker compose --env-file .env.prod -f docker-compose.prod.yml restart backend

# Ver logs últimas 100 líneas
docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=100 -f

# Ejecutar comando en contenedor
docker exec -it edufeed-backend-prod /bin/sh
docker exec -it edufeed-db-prod psql -U edufeed_prod -d edufeed_prod

# Inspeccionar red
docker network inspect edufeed_default
```

---

## Resolución de problemas

### Error: "Cannot connect to Docker daemon"

**Causa**: Docker Desktop no está corriendo o permisos insuficientes.

**Solución**:
```powershell
# Verificar estado Docker
docker info

# Windows: abrir Docker Desktop
# Linux: iniciar servicio
sudo systemctl start docker
```

### Error: "port is already allocated"

**Causa**: Puerto (5432, 8080, 5052) ocupado por otra aplicación.

**Solución**:
```powershell
# Ver qué proceso usa el puerto
netstat -ano | findstr :8080

# Cambiar puerto en .env
# PORT=8081

# O detener proceso conflictivo
taskkill /PID <PID> /F
```

### Error: "backend exited with code 1"

**Causa**: Backend no puede conectar a DB, configuración incorrecta o migraciones Flyway fallan.

**Solución**:
```powershell
# Ver logs detallados
docker compose logs backend

# Verificar DB está healthy
docker ps
# STATUS debe ser "healthy" para edufeed-db-prod

# Verificar variables de entorno
docker exec edufeed-backend-prod env | grep DB_

# Probar conexión manual a DB
docker exec -it edufeed-db-prod psql -U edufeed_prod -d edufeed_prod -c "SELECT 1;"

# Revisar migraciones Flyway
docker exec edufeed-backend-prod cat /app/classes/db/migration/V1__init.sql
```

### Error: "Flyway baseline required"

**Causa**: DB existente sin metadata Flyway.

**Solución**:
```bash
# Opción 1: Baselining en .env
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
SPRING_FLYWAY_BASELINE_VERSION=0

# Opción 2: Baseline manual
docker exec edufeed-db-prod psql -U edufeed_prod -d edufeed_prod
# En psql:
# CREATE TABLE IF NOT EXISTS flyway_schema_history (...);
```

### Error: "OOM (Out of Memory)"

**Causa**: JVM usa demasiada memoria.

**Solución**:
```yaml
# En docker-compose.prod.yml, agregar límites
services:
  backend:
    environment:
      JAVA_OPTS: "-Xms512m -Xmx1g -XX:MaxMetaspaceSize=256m"
    deploy:
      resources:
        limits:
          memory: 1.5G
        reservations:
          memory: 512M
```

### Build falla con "Maven dependency not found"

**Causa**: edufeed-common/edufeed-biometric no compilados.

**Solución**:
```powershell
# Compilar módulos comunes primero
mvn clean install -DskipTests

# Luego build Docker
docker compose build backend
```

---

## Mejores prácticas

### Seguridad

- ✅ **NO** versionar `.env.prod` con secretos reales
- ✅ Usar secretos fuertes (min 32 caracteres aleatorios)
- ✅ Rotar JWT_SECRET y BIOMETRIC_ENCRYPTION_KEY periódicamente
- ✅ Limitar exposición de puertos (firewall, security groups)
- ✅ Usar HTTPS en producción (nginx reverse proxy + Let's Encrypt)
- ✅ Ejecutar contenedores como usuario no-root (agregar `USER` en Dockerfile)

### Performance

- ✅ Usar multi-stage builds para imágenes pequeñas (JRE vs JDK)
- ✅ Aprovechar layer caching (COPY pom.xml antes de src)
- ✅ Montar volumen .m2 para cache de Maven en builds frecuentes
- ✅ Configurar health checks para restart automático
- ✅ Ajustar recursos (CPU, memoria) según carga real

### Operaciones

- ✅ Usar `docker-compose.yml` para dev, `docker-compose.prod.yml` para prod
- ✅ Nombrar contenedores explícitamente (edufeed-db-prod vs db_1)
- ✅ Versionar imágenes con tags (v1.0.0, v1.1.0) además de `latest`
- ✅ Implementar backups automáticos de volúmenes (ver Fase 8.4)
- ✅ Monitorear uso de disco (docker system df)
- ✅ Limpiar imágenes huérfanas (`docker image prune`)

### CI/CD

- ✅ Build automatizado en GitHub Actions (ver Fase 8.2)
- ✅ Push a registry (GHCR, Docker Hub, ECR)
- ✅ Scan de vulnerabilidades (Trivy, Snyk)
- ✅ Deploy automatizado a staging, manual a prod (aprobaciones)

---

## Checklist de implementación

- [x] Dockerfile multi-stage creado en `edufeed-backend/Dockerfile`
- [x] docker-compose.prod.yml con servicios db, pgAdmin, backend
- [x] Archivos .env.dev, .env.stage, .env.prod.example
- [x] .gitignore actualizado (.env.prod excluido)
- [x] Build local exitoso
- [x] Deploy dev validado (docker compose up)
- [ ] Deploy staging validado
- [ ] Deploy producción validado
- [ ] Reverse proxy HTTPS configurado (nginx/Traefik)
- [ ] Backups automatizados (Fase 8.4)
- [ ] Monitoreo configurado (Fase 8.3)
- [ ] CI/CD pipeline funcional (Fase 8.2)

---

## Referencias

- [Docker Multi-stage builds](https://docs.docker.com/build/building/multi-stage/)
- [Docker Compose reference](https://docs.docker.com/compose/compose-file/)
- [Eclipse Temurin images](https://hub.docker.com/_/eclipse-temurin)
- [PostgreSQL Docker](https://hub.docker.com/_/postgres)
- [Spring Boot Docker guide](https://spring.io/guides/topicals/spring-boot-docker)

---

**Última actualización**: 31 de octubre de 2025  
**Fase**: 8.1 - Dockerización  
**Estado**: ✅ Completado
