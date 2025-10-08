# Contexto y Bitácora de Cambios — EduFeed

Fecha: 2025-10-07

## Antecedentes
Proyecto: Sistema de control de acceso para restaurante escolar (~500 usuarios) con validación biométrica (huella, rostro y voz), control de derechos por pago (diario, mensual, paquetes), reportes de asistencia/inasistencias, integración con caja, historial de accesos y auditoría.

## Decisiones de Arquitectura
- Java 21 + Spring Boot 3.4.10 para el backend (se actualizó desde 3.3.x para resolver warnings y compatibilidad).
- Aplicación de escritorio JavaFX para punto de control y caja (consumidor de API).
- Librería `edufeed-biometric` con interfaz `BiometricProvider` para huella, rostro y voz.
- Proveedor mock por defecto (`MockBiometricProvider`) para desarrollo sin hardware.
- PostgreSQL 16 como base de datos, gestionado con Docker Compose. Se eligió la imagen `postgres:16.4-alpine` para mayor estabilidad con Flyway.
- Migraciones de esquema con Flyway. Se añadió la dependencia de soporte `flyway-database-postgresql` y se ajustaron propiedades para tolerar el arranque de la BD (reintentos) antes de aplicar migraciones.
- OpenAPI vía springdoc, Actuator habilitado. La ruta de documentación es `/api-docs` y Swagger UI está expuesta en `/swagger`.
- Seguridad básica inicial permitiendo endpoints públicos clave; se reforzará con autenticación/autorización por rol.

## Estructura creada
- Multi-módulo Maven (`edufeed-backend`, `edufeed-desktop`, `edufeed-biometric`, `edufeed-common`).
- `docker-compose.yml` con PostgreSQL + pgAdmin.
- `.env.example` con variables iniciales.
- Scripts PowerShell `db-up.ps1` y `db-down.ps1`.
- Configuración de VS Code (`launch.json`, `tasks.json`, `extensions.json`, `settings.json`).
- Documentación base (`README.md`, `docs/architecture.md`, `docs/manual-usuario.md`, `docs/manual-instalacion.md`, `SECURITY.md`).

## Implementaciones puntuales
- Backend: `EduFeedApplication`, `HealthController` (`/health`), `BiometricTestController` (`/biometric/verify/{modality}`), `SecurityConfig` (lista blanca básica) y `BiometricConfig` (inyecta `MockBiometricProvider`).
- Flyway: `V1__init.sql` con tablas `app_user` y `access_log`.
- Desktop JavaFX: `DesktopApp` que consume `/health` del backend y muestra estado.
- Biometric Lib: `BiometricProvider` y `MockBiometricProvider`.
-- Se retiró temporalmente la dependencia `be.tarsos:TarsosDSP` por resolución en Maven Central; se evaluarán alternativas/hosting.

## Cambios recientes (bitácora técnica)
- 2025-10-06: Se creó la estructura multi-módulo y las implementaciones iniciales (backend, desktop, biometric lib y migración V1).
- 2025-10-06: Fallos de compilación por dependencia externa (TarsosDSP) — se removió temporalmente para permitir build.
- 2025-10-06: Flyway reportó "Unsupported Database" para algunas tags de PostgreSQL 16.x; se agregó el módulo de soporte y se fijó la imagen a `postgres:16.4-alpine`.
- 2025-10-06: El backend arrancaba antes que la base de datos (Connection refused). Se añadió lógica de espera en `scripts/db-up.ps1` que consulta el estado de salud del contenedor y reintenta hasta que la BD esté lista.
- 2025-10-07: Se actualizó Spring Boot a 3.4.10 y se configuró `mainClass` en `edufeed-backend/pom.xml` para permitir `mvn spring-boot:run` desde el módulo.

## Cómo reproducir el entorno (rápido)
1. Asegúrate de tener Docker Desktop ejecutándose y `JAVA_HOME` apuntando a un JDK 21.
2. Copia el archivo de ejemplo de variables de entorno:

```pwsh
cp .env.example .env
```

3. Levanta la base de datos y espera a que esté saludable:

```pwsh
./scripts/db-up.ps1
```

4. Construye el proyecto (sin tests la primera vez para acelerar):

```pwsh
mvn -T1C -DskipTests package
```

5. Ejecuta el backend desde su módulo (recomendado):

```pwsh
mvn -f edufeed-backend/pom.xml spring-boot:run
```

6. Verifica endpoints:

- Salud: GET http://localhost:8080/health  → {"service":"edufeed-backend","status":"UP"}
- OpenAPI: GET http://localhost:8080/api-docs  → HTTP 200

## Archivos clave modificados
- `edufeed-backend/pom.xml` — mainClass agregado, dependencias Flyway ajustadas.
- `edufeed-backend/src/main/resources/application.yml` — configuraciones de datasource, Hikari y Flyway (reintentos) afinadas.
- `docs/Contexto.md` — esta bitácora actualizada.
- `docker-compose.yml` — imagen de Postgres fijada a `postgres:16.4-alpine`.
- `scripts/db-up.ps1` — espera por health del contenedor y reintentos.
- `.vscode/tasks.json` — tarea `Backend: run` actualizada para ejecutar `-f edufeed-backend/pom.xml`.

## Verificaciones realizadas
- Flyway aplicó `V1__init.sql` correctamente (mensaje de validación y aplicación de migración en logs de arranque).
- Backend arrancó correctamente y respondió `/health` y `/api-docs`.

## Riesgos y puntos pendientes
- Reintegrar o reemplazar `TarsosDSP` (procesamiento de audio) si se requiere verificación de voz en local.
- Añadir pruebas automáticas (unit + integración) y pipeline CI.
- Implementar proveedores biométricos reales (SDKs) y pruebas con hardware.

## Próximos pasos (priorizados)
1. Modelado de dominio para pagos y derechos (RF-03/05/06/08/10/12/13).
2. Seguridad: JWT, roles y auditoría detallada (RF-11).
3. Integración con hardware biométrico y validación en campo.
4. Tests automáticos y CI/CD.


## Estado de compilación
- Build del multi-módulo: OK (sin tests) tras remover TarsosDSP.
- Backend ejecutable; endpoints `/health` y `/biometric/verify/*` funcionales con mock.
- Desktop ejecutable; prueba `/health` OK.

## Próximos pasos sugeridos
1. Modelar dominio de pagos/derechos y reportes según RF-03/05/06/10/12/13.
2. Seguridad real: autenticación (JWT/sesión), autorización por rol y auditoría (RF-11).
3. Integración con caja (RF-08): diseño y endpoints/cola.
4. Selección de hardware de huella y SDK; proveedor real. Pipeline reconocimiento facial y voz.
5. Pruebas automatizadas y datos seed.

## Notas de seguridad (RNF-01)
- No almacenar biometría cruda; sólo templates/rasgos cifrados.
- Cifrado en reposo y en tránsito (HTTPS en despliegues).
- Gestión de secretos por entorno/secret manager.
