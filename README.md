<div align="center">

# 🍽️ EduFeed — Restaurante Escolar

Plataforma para control de acceso del restaurante escolar con validación biométrica (🖐️ huella, 🙂 rostro y 🎙️ voz), gestión de usuarios y pagos, reportes administrativos y auditoría.

[![Java](https://img.shields.io/badge/Java-21-red?logo=openjdk)](https://adoptium.net/)  
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.10-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![JavaFX](https://img.shields.io/badge/JavaFX-22-3776AB)](https://openjfx.io/)  
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql)](https://www.postgresql.org/)  
[![License](https://img.shields.io/badge/License-Private-informational)](#)

</div>

---

## 🧩 Módulos
- `edufeed-backend` (Spring Boot): API REST, seguridad, reportes, integración con DB y caja.
- `edufeed-desktop` (JavaFX): App de escritorio para punto de acceso y caja, consume la API.
- `edufeed-biometric` (lib): Interfaces y proveedores biométricos (mock y plugins reales).
- `edufeed-common`: DTOs/modelos compartidos.

-## ✅ Requisitos de desarrollo
- JDK 21 (LTS)
- Maven 3.9+
- Docker Desktop (PostgreSQL + pgAdmin)
- PowerShell 7 (Windows) o Bash (Linux/macOS)
- VS Code + extensiones recomendadas (`.vscode/extensions.json`)

Tip: ejecuta el script de setup para instalar herramientas y configurar el entorno rápidamente.

```pwsh
# Windows (PowerShell 7)
./scripts/setup-dev.ps1
```
```bash
# Linux/macOS (Bash)
./scripts/setup-dev.sh
```

## 🗄️ Base de datos (Docker)
1. Copia `.env.example` a `.env` y ajusta si es necesario.
2. Levanta la base (el script espera a que Postgres esté listo antes de continuar):
```pwsh
./scripts/db-up.ps1
```
3. pgAdmin: http://localhost:5050 (credenciales en `.env`).

Para detener:
```pwsh
./scripts/db-down.ps1
# Con -Purge elimina volúmenes
./scripts/db-down.ps1 -Purge
```

## ▶️ Ejecutar aplicaciones
### Backend (primera vez)
Recomendado: compilar todo y ejecutar el módulo backend explícitamente para evitar problemas con multi-módulo.

```pwsh
# Construir artefactos (sin tests para acelerar la primera vez)
mvn -T1C -DskipTests package

# Ejecutar el backend desde su POM (recomendado)
mvn -f edufeed-backend/pom.xml spring-boot:run
```
- Salud: http://localhost:8080/health
-- Swagger UI: http://localhost:8080/swagger
- Prueba biométrica mock:
  - /biometric/verify/fingerprint
  - /biometric/verify/face
  - /biometric/verify/voice

### Desktop (JavaFX)
```pwsh
mvn -pl edufeed-desktop -am -DskipTests javafx:run
```
La app muestra un botón que prueba `/health` del backend. Lee `BACKEND_BASE_URL` desde `.env`.

## 🧱 Estructura del repositorio
```
.
├─ edufeed-backend/        # API Spring Boot
│  ├─ src/main/java/.../api
│  ├─ src/main/java/.../config
│  └─ src/main/resources/db/migration
├─ edufeed-desktop/        # App JavaFX
├─ edufeed-biometric/      # Lib biometría (interfaces + mock)
├─ edufeed-common/         # DTOs comunes
├─ scripts/                # db-up/down, setup-dev
├─ docs/                   # architecture, manuales y contexto
├─ .vscode/                # tareas y lanzadores
├─ docker-compose.yml
├─ .env.example
└─ SECURITY.md
```

## 🔐 Seguridad (RNF-01)
- No se almacenan imágenes crudas de biometría; sólo templates/rasgos cifrados.
- Cifrado en reposo (AES-256) y en tránsito (HTTPS en despliegue).
- Auditoría de operaciones y accesos.
- Gestión de secretos vía variables de entorno o gestor de secretos en producción.

## 🧪 Endpoints de verificación (mock)
- `GET /health` — estado del servicio.
- `GET /biometric/verify/{modality}` — `fingerprint|face|voice`.

## 🧭 Roadmap inmediato
- Modelar dominio de pagos y derechos (mensual, diario, paquetes) y reportes (RF-03/05/06/10/12/13).
- Roles y autenticación real (JWT/sesión) y auditoría (RF-11).
- Elegir hardware de huella y añadir proveedor real; pipeline de rostro (OpenCV + embeddings); voz (motor de verificación).
- Integración con caja (RF-08): endpoints/webhook/cola.

## 📚 Documentación
- `docs/architecture.md` — decisiones y visión.
- `docs/manual-usuario.md` — borrador del manual.
- `docs/manual-instalacion.md` — guía de instalación.
- `docs/Contexto.md` — bitácora y contexto de cambios.
- `SECURITY.md` — controles de seguridad.

> ℹ️ Nota: Por ahora se usa `MockBiometricProvider` para flujos end-to-end sin hardware. Los proveedores reales se añadirán como plugins.

## 🛠️ Notas rápidas de troubleshooting
- Si el backend falla con "Connection refused" al arrancar: asegúrate de ejecutar `./scripts/db-up.ps1` y esperar a que Postgres esté `healthy` antes de `spring-boot:run`.
- Si Flyway informa "Unsupported Database": la imagen recomendada es `postgres:16.4-alpine` (ver `docker-compose.yml`) y el proyecto incluye la dependencia `flyway-database-postgresql`.
- Para ejecutar la tarea de VS Code "Backend: run" asegúrate que la tarea usa `-f edufeed-backend/pom.xml` (ya configurada en `.vscode/tasks.json`).

## ✅ Resumen de verificación rápida
- `./scripts/db-up.ps1` → crea contenedores Postgres + pgAdmin y espera health.
- `mvn -f edufeed-backend/pom.xml spring-boot:run` → backend en http://localhost:8080
- Verificar `/health` y `/api-docs` para confirmar que el servicio está listo.
