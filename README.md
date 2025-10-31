# 🍽️ Reto de Transformación Digital: Restaurante Escolar

## 1. Información General del Reto
**Título:** Restaurante Escolar  

---

## 2. Resumen Ejecutivo del Reto

Desarrollo de un **sistema integral para la gestión del restaurante escolar**, con aproximadamente **500 usuarios**, que maneje datos personales y validación biométrica (huella o reconocimiento facial).  

El sistema debe permitir:
- Control de acceso según derechos adquiridos por pago (**diario, mensual o paquetes**).  
- Generación de reportes de **asistencia, pagos e inasistencias**.  
- Integración con sistema de caja y registro de transacciones.  
- Gestión completa de usuarios, pagos, historial de accesos y auditoría.  
- Seguridad biométrica y trazabilidad administrativa.  

---

## 3. Requisitos Funcionales

| ID | Requerimiento | Descripción |
|----|----------------|-------------|
| **RF-01** | Registro de usuarios | Permitir registrar a los ~500 usuarios con datos personales, tipo de usuario (niño, estudiante, docente) y su huella o rostro para validación biométrica y voz. |
| **RF-02** | Validación biométrica | Validar la identidad del usuario mediante huella o reconocimiento facial y voz al ingresar al restaurante. |
| **RF-03** | Control de derecho adquirido | Verificar si el usuario tiene un pago válido (diario, mensual o paquete) antes de permitir el ingreso. |
| **RF-04** | Orientación a caja | En caso de no tener derecho adquirido, mostrar notificación para que el usuario sea orientado a la caja y adquiera el servicio. |
| **RF-05** | Registro de pagos | Permitir registrar pagos por tipo: **mensualidad**, **diario** o **paquete de días**. |
| **RF-06** | Reporte de asistencia | Generar reportes de: usuarios que pagaron mensualidad pero no asistieron, y usuarios que asistieron con pago diario o por días. |
| **RF-07** | Gestión de usuarios | Alta, baja y actualización de información personal y biométrica de los usuarios. |
| **RF-08** | Integración con caja | Integrarse con el sistema de caja para actualizar derechos adquiridos automáticamente tras el pago. |
| **RF-09** | Historial de accesos | Registrar fecha, hora y estado (aprobado/denegado) de cada intento de ingreso. |
| **RF-10** | Reportes administrativos | Generar reportes por tipo de pago, ingresos diarios/mensuales, asistencias y no asistencias. |
| **RF-11** | Auditoría de operaciones | Registrar en bitácora las modificaciones de usuarios, pagos y accesos. |
| **RF-12** | Registro y venta de mensualidades | Gestionar la venta de mensualidades de manera diferenciada, con control administrativo. |
| **RF-13** | Reporte de inasistencias | Generar reportes de personas con derecho mensual o días prepagados que no asistieron. |

---

## 4. Requisitos No Funcionales

| ID | Requerimiento | Descripción |
|----|----------------|-------------|
| **RNF-01** | Seguridad de datos | Cifrar y proteger la información biométrica y financiera conforme a normativa de protección de datos personales. |
| **RNF-02** | Compatibilidad hardware | Garantizar compatibilidad con lectores de huella y cámaras faciales estándar. |

---

## 5. Tecnologías Sugeridas y Otros

- Implementación de **reconocimiento facial y huella digital** mediante herramientas compatibles con hardware estándar.  

---

## 6. Incentivos

- 💰 **Valor total:** $1.500.000 COP  

---

## 7. Entregables Mínimos

1. Manual de Usuario (Digital)  
2. Manual de Instalación (Digital)  
3. URLs de acceso a la solución y/o aplicación  
4. Usuarios, roles y accesos configurados  
5. Documento de Arquitectura  
6. _(Espacio reservado para fecha y firma del responsable)_

---

## 8. Docker y CI/CD (FASE 8)

### 8.1 Dockerización

- Dockerfile backend: `edufeed-backend/Dockerfile` (multi-stage build Temurin 24)
- Compose prod: `docker-compose.prod.yml` (servicios: Postgres 16, pgAdmin, backend)
- Entornos: `.env.dev`, `.env.stage`, `.env.prod.example` (copiar a `.env.prod` y NO subir)

Ejemplos (Linux/Mac/WSL):

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml build backend
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

En `.env.prod`, define al menos: `POSTGRES_PASSWORD`, `PGADMIN_DEFAULT_PASSWORD`, `JWT_SECRET`, `BIOMETRIC_ENCRYPTION_KEY`.

### 8.2 CI/CD (GitHub Actions)

Workflow: `.github/workflows/ci-cd.yml`

- Eventos: `push` y `pull_request` a `main`.
- Fases: build+test (Maven) → build+push imagen (GHCR) → deploy a `staging` → deploy a `production` (con aprobación vía Environments).

Registra estos Secrets para despliegue remoto opcional por SSH (si no existen, el job de deploy se omite):

- Staging: `STAGING_HOST`, `STAGING_USER`, `STAGING_SSH_KEY` (clave privada), `STAGING_PATH` (ej. `/opt/edufeed-staging`).
- Producción: `PROD_HOST`, `PROD_USER`, `PROD_SSH_KEY`, `PROD_PATH` (ej. `/opt/edufeed`).

Requisitos en el servidor remoto: Docker y Docker Compose v2, archivo `.env.prod` preprovisionado en `STAGING_PATH/PROD_PATH`.

Para exigir aprobación manual en producción: configura el Environment `production` en Settings → Environments con `Required reviewers`.

### 8.3 Observabilidad (Prometheus + Grafana + ELK)

- Archivos de configuración en `observability/` (Prometheus, Alertmanager, Grafana, Logstash, Filebeat).
- Levanta el stack junto al compose de prod (comparten red por defecto al usar ambos `-f`):

```bash
docker compose --env-file .env.prod \
	-f docker-compose.prod.yml \
	-f docker-compose.observability.yml up -d
```

Servicios:
- Prometheus: http://localhost:9090 (scrapea `backend:8080/actuator/prometheus`)
- Grafana: http://localhost:3000 (admin/admin por defecto; datasource y dashboard provisionados)
- Elasticsearch: http://localhost:9200
- Logstash (beats): :5044
- Kibana: http://localhost:5601
- Filebeat: colecta logs de Docker y los envía a Logstash

Alertas:
- Edita `observability/alertmanager/alertmanager.yml` y configura Slack o Email.
- Regla p95 y 500s en `observability/prometheus/rules.yml`.

### 8.4 Backups y restauración

Documentación completa y scripts en `docs/backup-restore.md` y `scripts/backup/`:

- `db-backup.ps1`: genera backups lógicos (`pg_dump -Fc -Z 9`) con retención diaria/semanal/mensual y subida opcional a S3/Azure.
- `db-restore.ps1`: restaura `.dump` (pg_restore) o `.sql` (psql) dentro del contenedor.

Ejemplos rápidos (PowerShell en Windows):

```powershell
pwsh ./scripts/backup/db-backup.ps1 -EnvFile .env -ContainerName edufeed-db -BackupRoot ./backups
pwsh ./scripts/backup/db-restore.ps1 -EnvFile .env -ContainerName edufeed-db -BackupFile ./backups/daily/2025-01-20/edufeed_20250120_010203.dump -DropAndCreate
```

