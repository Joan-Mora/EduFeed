# Estructura del repositorio — EduFeed

Este documento describe qué hace cada carpeta y archivo importante del proyecto, cómo están organizados los módulos, y pasos rápidos para ejecutar y desarrollar localmente.

> Fecha: 2025-10-10

---

## Resumen de alto nivel
EduFeed es un proyecto multi-módulo Maven que contiene:
- `edufeed-backend`: API REST (Spring Boot) y lógica del dominio.
- `edufeed-desktop`: aplicación JavaFX para punto de control y caja.
- `edufeed-biometric`: librería/abstracción para proveedores biométricos (mock + plugins).
- `edufeed-common`: modelos/DTOs compartidos.
- Infraestructura: `docker-compose.yml`, scripts de arranque (`scripts/`), y documentación (`docs/`).

---

## Carpetas y archivos raíz

- `pom.xml` (root)
  - POM padre del multi-módulo. Define versiones comunes (Java, Spring Boot), pluginManagement y módulos.

- `docker-compose.yml`
  - Define servicios Docker para desarrollo: `db` (Postgres) y `pgadmin`.
  - Variables leídas desde `.env`.

- `.env` (no subir credenciales sensibles)
  - Variables de entorno para facilitar el desarrollo local (DB, pgAdmin, backend port, etc.).

- `README.md`
  - Documentación general del reto, requerimientos funcionales y NO funcionales, y comandos resumen.

- `docs/` (documentación)
  - `Contexto.md` — bitácora y decisiones de arquitectura.
  - `REPO_STRUCTURE.md` — este archivo.
  - `architecture.md`, `manual-instalacion.md`, `manual-usuario.md` — otros documentos de soporte.

- `scripts/`
  - `db-up.ps1` / `db-down.ps1` — scripts PowerShell para levantar y bajar la DB y pgAdmin, con esperas/healthchecks.
  - `setup-dev.ps1` / `setup-dev.sh` — scripts de instalación de dependencias locales y configuración del entorno.
  - `reportes/` — SQL y scripts para generar/reportes.

- `seed/`
  - SQL de seed con datos de prueba.

---

## Módulos Maven

### edufeed-backend/
Estructura principal del backend:
- `pom.xml` — configuración específica del módulo (dependencias Spring Boot, Flyway, springdoc, etc.).
- `src/main/java/co/cellano/edufeed/backend/` — código Java:
  - `EduFeedApplication.java` — clase `main` de Spring Boot.
  - `config/` — clases de configuración (DataSource, seguridad, TimeZone, Flyway overrides).
  - `controller/` — controladores REST (por ejemplo `HealthController`, `BiometricTestController`, endpoints de prueba).
  - `service/` — servicios de negocio (lógica central: acceso, pagos, etc.).
  - `repository/` — interfaces Spring Data JPA (si existen).
  - `model/` — entidades JPA y DTOs si aplica.
  - `db/migration/` — scripts Flyway (V1__init.sql y futuros V2, V3...).
- `src/main/resources/`:
  - `application.yml` — configuración de la app (datasource, JPA, Flyway, logging, springdoc, actuator).
  - `application.properties` — propiedades planas (usadas para evitar advertencias de lint en YAML), p. ej. `spring.jpa.properties.hibernate.jdbc.time_zone`.
  - `META-INF/spring-configuration-metadata.json` — metadata que ayuda al IDE con propiedades custom.

Notas:
- Flyway se usa para migraciones. Si añades scripts, ponlos en `src/main/resources/db/migration` con la convención `V{n}__descripcion.sql`.
- Las tablas y nombres actuales (según V1) están en español: `usuarios`, `accesos`, `pagos`, `derechos_uso`, etc.

### edufeed-desktop/
- `pom.xml` — dependencias JavaFX y plugin `javafx-maven-plugin`.
- `src/main/java/...` — código JavaFX, clases de UI, servicios que consumen la API del backend.
- Propósito: app de escritorio para realizar lecturas biométricas, visualización de estado de acceso y caja.

### edufeed-biometric/
- Interfaces para `BiometricProvider` y una implementación `MockBiometricProvider`.
- Aquí integrarán SDKs/hardwares reales en el futuro.

### edufeed-common/
- DTOs, enums y utilidades compartidas entre backend y desktop.

---

## Archivos de interés y cómo usarlos

- `edufeed-backend/src/main/resources/db/migration/V1__init.sql`
  - Contiene la estructura base de la BD: tablas `usuarios`, `plantillas_biometricas`, `pagos`, `paquetes_pago`, `derechos_uso`, `accesos`, `usos_paquete`, `auditoria`, `transacciones_caja`, `calendario_servicio`.
  - Al iniciar el backend Flyway aplica estas migraciones si aún no existen.

- `edufeed-backend/src/main/resources/application.yml` y `application.properties`
  - Contienen las variables de configuración. En local usamos `.env` para inyectar valores en `docker-compose` y la aplicación (cuando se ejecuta desde Maven se leen del entorno del proceso).

- `.vscode/tasks.json`
  - Tareas preconfiguradas para arrancar backend y desktop desde VS Code (usa `mvn -f edufeed-backend/pom.xml spring-boot:run`).

- `docker-compose.yml`
  - Define `db` y `pgadmin`. Usa volúmenes `db_data` y `pgadmin_data`.

---

## Cómo empezar a desarrollar (rápido)
1. Asegúrate de tener Java 21 (o compatible configurado en VS Code), Maven y Docker Desktop.
2. Copia `.env.example` a `.env` y ajusta credenciales si hace falta.
3. Levanta la BD y pgAdmin:
```powershell
./scripts/db-up.ps1
```
4. Ejecuta el backend (desde la raíz):
```powershell
mvn -f edufeed-backend/pom.xml spring-boot:run
```
5. Ejecuta el desktop (si necesitas UI):
```powershell
mvn -f edufeed-desktop/pom.xml -DskipTests javafx:run
```
6. Abre pgAdmin en http://localhost:5050 y registra el servidor `db` con credenciales de `.env`.

---

## Convenciones y notas de diseño
- Las migraciones Flyway son la fuente de la verdad para el esquema. Evita cambios manuales en la BD en producción: siempre crea una nueva migración.
- Nombres de tablas actuales están en español por claridad con stakeholders. Podemos crear vistas en inglés para compatibilidad si migras código o herramientas externas.
- Biometría: la librería `edufeed-biometric` expone una interfaz; usa `MockBiometricProvider` para pruebas end-to-end.

---

## Próximos cambios / recomendaciones inmediatas
1. Implementar el vertical slice de `access/check` + `payments` (migraciones V2) para controlar acceso según derecho adquirido.
2. Agregar tests unitarios e integración (contenerizados) y CI.
3. Preparar scripts de seed y vistas de reportes automáticos.
4. Documentar API con ejemplos curl en README.

---

Si quieres, puedo:
- Añadir un índice al README con enlaces a cada documento.
- Generar `docs/DEVELOPER_SETUP.md` con pasos detallados para configurar IDE, maven/Java y debug en VS Code.

Marca la opción que prefieras y lo incorporo.
