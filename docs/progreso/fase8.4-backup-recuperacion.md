# Fase 8.4: Backup y Recuperación ante Desastres

## Índice

1. [Introducción](#introducción)
2. [Estrategia de backup](#estrategia-de-backup)
3. [Scripts de backup](#scripts-de-backup)
4. [Scripts de restauración](#scripts-de-restauración)
5. [Retención y rotación](#retención-y-rotación)
6. [Almacenamiento remoto](#almacenamiento-remoto)
7. [Automatización](#automatización)
8. [Pruebas de restauración](#pruebas-de-restauración)
9. [Plan de recuperación (DRP)](#plan-de-recuperación-drp)
10. [Troubleshooting](#troubleshooting)
11. [Mejores prácticas](#mejores-prácticas)

---

## Introducción

La estrategia de backup garantiza continuidad del negocio ante pérdida de datos por fallo de hardware, error humano, ransomware o desastres naturales.

### Objetivos

- ✅ Backups automáticos diarios de PostgreSQL
- ✅ Retención: 7 diarios, 4 semanales, 12 mensuales
- ✅ Subida opcional a S3/Azure Blob
- ✅ Restauración probada con RTO ≤ 1h
- ✅ RPO ≤ 24h (o 1h con backups horarios)
- ✅ Cifrado de backups en tránsito y reposo

### Definiciones

| Término | Definición | Objetivo EduFeed |
|---------|------------|------------------|
| **RPO** (Recovery Point Objective) | Pérdida de datos tolerada (tiempo desde último backup) | ≤ 24h (diario), ≤ 1h (horario opcional) |
| **RTO** (Recovery Time Objective) | Tiempo hasta sistema operativo tras fallo | ≤ 1h (restauración + validación) |
| **Backup completo** | Copia íntegra de la base de datos | Diario (pg_dump -Fc) |
| **Backup incremental** | Solo cambios desde último backup | No implementado (futuro con WAL archiving) |

### Datos a respaldar

- **Base de datos PostgreSQL** (edufeed_prod):
  - Usuarios, biometrías, pagos, accesos, auditoría
  - Tamaño estimado: 500 MB (500 usuarios × 1 MB promedio)
  - Crecimiento: ~50 MB/mes (logs, accesos históricos)

- **Archivos de configuración** (futuro):
  - `.env.prod` (secretos - backup cifrado)
  - `docker-compose.prod.yml`
  - Certificados SSL

- **Volúmenes Docker** (no incluido en v1):
  - `db_data_prod` (ya respaldado vía pg_dump)
  - `pgadmin_data_prod` (opcional, baja prioridad)

---

## Estrategia de backup

### Esquema 3-2-1

- **3** copias: original + 2 backups (local + remoto)
- **2** tipos de medios: disco local + S3/Azure
- **1** copia offsite: S3/Azure en región diferente

```
┌─────────────────────────────────────────────────────────┐
│                    EduFeed Prod                         │
│  ┌──────────────────┐                                   │
│  │  PostgreSQL DB   │ (Original)                        │
│  │  edufeed_prod    │                                   │
│  └────────┬─────────┘                                   │
│           │ pg_dump -Fc nightly 01:00                   │
│           ▼                                             │
│  ┌──────────────────┐                                   │
│  │ Local Backups    │ (Copia 1)                         │
│  │ ./backups/       │                                   │
│  │ ├─ daily/        │ 7 días                            │
│  │ ├─ weekly/       │ 4 semanas                         │
│  │ └─ monthly/      │ 12 meses                          │
│  └────────┬─────────┘                                   │
└───────────┼─────────────────────────────────────────────┘
            │
            │ aws s3 cp / az storage blob upload
            ▼
   ┌────────────────────┐
   │  S3 / Azure Blob   │ (Copia 2 - offsite)
   │  Región: us-east-1 │
   │  Bucket: edufeed-  │
   │  backups-prod      │
   │  Lifecycle: 90d    │
   └────────────────────┘
```

### Tipos de backup

| Tipo | Frecuencia | Retención | Herramienta |
|------|------------|-----------|-------------|
| **Diario completo** | 01:00 (lun-dom) | 7 días | pg_dump -Fc |
| **Semanal** | Domingo | 4 semanas | Copia del diario |
| **Mensual** | Día 1 | 12 meses | Copia del diario |
| **Pre-deploy** | Antes de `docker compose up` en CI/CD | 3 backups | pg_dump |

### Formato de backup

- **pg_dump -Fc** (custom format):
  - Comprimido (gzip -Z 9)
  - Restaurable con `pg_restore`
  - Soporta restore paralelo
  - Incluye schema + datos

**Alternativas consideradas** (no implementadas en v1):
- **pg_basebackup**: backup físico (requiere más espacio)
- **WAL archiving**: PITR (Point-in-Time Recovery)
- **Barman**: gestor de backups Postgres enterprise

---

## Scripts de backup

### db-backup.ps1

**Ubicación**: `scripts/backup/db-backup.ps1`

**Características**:
- Ejecuta `pg_dump` dentro del contenedor Docker (no requiere pg_dump local)
- Lee credenciales desde `.env` o `.env.prod`
- Genera backup en formato custom (-Fc) comprimido (-Z 9)
- Calcula checksum SHA256
- Retención automática (7 diarios, 4 semanales, 12 mensuales)
- Subida opcional a S3 o Azure Blob

**Parámetros**:

```powershell
db-backup.ps1
  -EnvFile <path>           # .env con POSTGRES_* vars (default: .env.prod)
  -ContainerName <string>   # Nombre contenedor (default: edufeed-db)
  -BackupRoot <path>        # Carpeta destino backups (default: ./backups)
  -RetentionDaily <int>     # Días retención diaria (default: 7)
  -RetentionWeekly <int>    # Semanas retención semanal (default: 4)
  -RetentionMonthly <int>   # Meses retención mensual (default: 12)
  -UploadS3                 # Switch: subir a S3
  -S3Bucket <string>        # Bucket S3 (ej. edufeed-backups)
  -S3Prefix <string>        # Prefijo S3 (ej. prod/)
  -UploadAzure              # Switch: subir a Azure Blob
  -AzureContainer <string>  # Contenedor Azure (ej. edufeedbackups)
```

**Ejemplo de ejecución**:

```powershell
# Backup local (dev)
pwsh ./scripts/backup/db-backup.ps1 `
  -EnvFile .env `
  -ContainerName edufeed-db `
  -BackupRoot ./backups

# Backup producción con subida a S3
pwsh ./scripts/backup/db-backup.ps1 `
  -EnvFile .env.prod `
  -ContainerName edufeed-db-prod `
  -BackupRoot ./backups `
  -UploadS3 `
  -S3Bucket edufeed-backups-prod `
  -S3Prefix prod/

# Backup con subida a Azure Blob
pwsh ./scripts/backup/db-backup.ps1 `
  -EnvFile .env.prod `
  -ContainerName edufeed-db-prod `
  -UploadAzure `
  -AzureContainer edufeedbackups
```

**Salida**:

```
[i] Usando contenedor: edufeed-db-prod
[i] Realizando pg_dump de 'edufeed_prod'...
[i] Copiando backup al host: ./backups/daily/2025-10-31/edufeed_prod_20251031_010203.dump
[i] Subiendo a s3://edufeed-backups-prod/prod/daily/2025-10-31/edufeed_prod_20251031_010203.dump
[OK] Backup exitoso: ./backups/daily/2025-10-31/edufeed_prod_20251031_010203.dump
```

**Archivos generados**:

```
backups/
├── daily/
│   ├── 2025-10-31/
│   │   ├── edufeed_prod_20251031_010203.dump
│   │   └── edufeed_prod_20251031_010203.dump.sha256
│   ├── 2025-10-30/
│   └── ... (últimos 7 días)
├── weekly/
│   ├── 2025-44/  (semana ISO)
│   └── ... (últimas 4 semanas)
└── monthly/
    ├── 2025-10/
    └── ... (últimos 12 meses)
```

### Flujo interno del script

1. **Validación de requisitos**:
   - Contenedor Docker existe y está corriendo
   - Variables POSTGRES_DB/USER/PASSWORD disponibles

2. **Ejecución de pg_dump**:
   ```bash
   docker exec -e PGPASSWORD=*** edufeed-db-prod \
     pg_dump -U edufeed_prod -d edufeed_prod \
     -h localhost -Fc -Z 9 -f /tmp/edufeed_prod_20251031_010203.dump
   ```

3. **Copia a host**:
   ```powershell
   docker cp edufeed-db-prod:/tmp/edufeed_prod_20251031_010203.dump ./backups/daily/2025-10-31/
   ```

4. **Checksum**:
   ```powershell
   Get-FileHash -Algorithm SHA256 backup.dump > backup.dump.sha256
   ```

5. **Copias semanales/mensuales**:
   - Si es domingo → copia a `weekly/YYYY-ww/`
   - Si es día 1 → copia a `monthly/YYYY-MM/`

6. **Retención**:
   - Ordena carpetas por fecha (descendente)
   - Elimina las que exceden límite (ej. 8va carpeta en daily)

7. **Subida remota** (opcional):
   ```powershell
   # S3
   aws s3 cp backup.dump s3://edufeed-backups-prod/prod/daily/2025-10-31/ --only-show-errors

   # Azure
   az storage blob upload --container edufeedbackups --name prod/daily/2025-10-31/backup.dump --file backup.dump
   ```

---

## Scripts de restauración

### db-restore.ps1

**Ubicación**: `scripts/backup/db-restore.ps1`

**Características**:
- Restaura `.dump` (pg_restore) o `.sql` (psql)
- Ejecuta dentro del contenedor Docker
- Opción `-DropAndCreate` para recrear DB limpia
- Termina conexiones activas antes de drop (evita errores)

**Parámetros**:

```powershell
db-restore.ps1
  -EnvFile <path>           # .env con credenciales (default: .env.prod)
  -ContainerName <string>   # Nombre contenedor (default: edufeed-db)
  -BackupFile <path>        # Ruta al .dump o .sql (REQUERIDO)
  -TargetDb <string>        # DB destino (default: POSTGRES_DB del .env)
  -DropAndCreate            # Switch: dropea y crea DB antes de restaurar
```

**Ejemplo de ejecución**:

```powershell
# Restaurar a DB nueva (staging/test)
pwsh ./scripts/backup/db-restore.ps1 `
  -EnvFile .env.stage `
  -ContainerName edufeed-db-stage `
  -BackupFile ./backups/daily/2025-10-31/edufeed_prod_20251031_010203.dump `
  -DropAndCreate `
  -TargetDb edufeed_stage

# Restaurar producción (PELIGRO: sobreescribe datos)
pwsh ./scripts/backup/db-restore.ps1 `
  -EnvFile .env.prod `
  -ContainerName edufeed-db-prod `
  -BackupFile ./backups/daily/2025-10-30/edufeed_prod_20251030_010203.dump `
  -DropAndCreate
```

**Salida**:

```
[i] Contenedor: edufeed-db-prod | DB destino: edufeed_prod
[i] Copiando backup al contenedor: /tmp/edufeed_prod_20251030_010203.dump
[i] Drop/Create database edufeed_prod
[i] Restaurando con pg_restore (formato personalizado)
[OK] Restauración completada en 'edufeed_prod'
```

### Flujo interno del script

1. **Validación**:
   - Archivo de backup existe
   - Contenedor corriendo

2. **Copiar backup al contenedor**:
   ```powershell
   docker cp backup.dump edufeed-db-prod:/tmp/
   ```

3. **Drop y crear DB** (si `-DropAndCreate`):
   ```sql
   -- Terminar conexiones activas
   SELECT pg_terminate_backend(pid)
   FROM pg_stat_activity
   WHERE datname = 'edufeed_prod' AND pid <> pg_backend_pid();

   -- Drop y create
   DROP DATABASE IF EXISTS "edufeed_prod";
   CREATE DATABASE "edufeed_prod";
   ```

4. **Restaurar**:
   ```bash
   # Formato custom (.dump)
   docker exec -e PGPASSWORD=*** edufeed-db-prod \
     pg_restore -U edufeed_prod -d edufeed_prod \
     --clean --if-exists --no-owner --no-privileges /tmp/backup.dump

   # Formato SQL (.sql)
   docker exec -e PGPASSWORD=*** edufeed-db-prod \
     psql -U edufeed_prod -d edufeed_prod -f /tmp/backup.sql
   ```

5. **Limpieza**:
   ```bash
   docker exec edufeed-db-prod rm -f /tmp/backup.dump
   ```

---

## Retención y rotación

### Política de retención

| Frecuencia | Retención | Copias guardadas | Espacio estimado |
|------------|-----------|------------------|------------------|
| Diario | 7 días | 7 | 7 × 500 MB = 3.5 GB |
| Semanal | 4 semanas | 4 | 4 × 500 MB = 2 GB |
| Mensual | 12 meses | 12 | 12 × 500 MB = 6 GB |
| **Total** | - | **23** | **~12 GB** |

**Nota**: Con compresión -Z 9, tamaño real ~50% (6 GB total).

### Calendario de retención (ejemplo)

```
Hoy: 2025-10-31

Daily (últimos 7):
  2025-10-31 ✓
  2025-10-30 ✓
  2025-10-29 ✓
  2025-10-28 ✓
  2025-10-27 ✓
  2025-10-26 ✓
  2025-10-25 ✓
  2025-10-24 ✗ (eliminado)

Weekly (últimas 4 semanas, domingos):
  2025-44 (27 oct) ✓
  2025-43 (20 oct) ✓
  2025-42 (13 oct) ✓
  2025-41 (06 oct) ✓
  2025-40 ✗ (eliminado)

Monthly (últimos 12 meses, día 1):
  2025-10 ✓
  2025-09 ✓
  ...
  2024-11 ✓
  2024-10 ✗ (eliminado)
```

### Ajustar retención

Editar parámetros al ejecutar script:

```powershell
# Retener 14 días, 8 semanas, 24 meses
pwsh ./scripts/backup/db-backup.ps1 `
  -RetentionDaily 14 `
  -RetentionWeekly 8 `
  -RetentionMonthly 24
```

O modificar valores por defecto en el script:

```powershell
# En db-backup.ps1, líneas 35-37
[int]$RetentionDaily = 14,
[int]$RetentionWeekly = 8,
[int]$RetentionMonthly = 24
```

---

## Almacenamiento remoto

### Amazon S3

#### Configuración

1. **Crear bucket**:
   ```bash
   aws s3 mb s3://edufeed-backups-prod --region us-east-1
   ```

2. **Configurar lifecycle** (borrar backups > 90 días):
   ```json
   {
     "Rules": [
       {
         "Id": "DeleteOldBackups",
         "Status": "Enabled",
         "Prefix": "prod/",
         "Expiration": { "Days": 90 }
       }
     ]
   }
   ```
   ```bash
   aws s3api put-bucket-lifecycle-configuration \
     --bucket edufeed-backups-prod \
     --lifecycle-configuration file://lifecycle.json
   ```

3. **Habilitar cifrado**:
   ```bash
   aws s3api put-bucket-encryption \
     --bucket edufeed-backups-prod \
     --server-side-encryption-configuration '{
       "Rules": [{
         "ApplyServerSideEncryptionByDefault": {
           "SSEAlgorithm": "AES256"
         }
       }]
     }'
   ```

4. **Configurar credenciales** (en máquina que ejecuta backup):
   ```bash
   aws configure
   # AWS Access Key ID: AKIA...
   # AWS Secret Access Key: ...
   # Default region: us-east-1
   ```

#### Ejecución

```powershell
pwsh ./scripts/backup/db-backup.ps1 `
  -UploadS3 `
  -S3Bucket edufeed-backups-prod `
  -S3Prefix prod/
```

### Azure Blob Storage

#### Configuración

1. **Crear storage account y contenedor**:
   ```bash
   az storage account create \
     --name edufeedbackups \
     --resource-group edufeed-prod-rg \
     --location eastus \
     --sku Standard_LRS

   az storage container create \
     --name edufeedbackups \
     --account-name edufeedbackups
   ```

2. **Habilitar cifrado** (habilitado por defecto en Azure).

3. **Obtener connection string**:
   ```bash
   az storage account show-connection-string \
     --name edufeedbackups \
     --resource-group edufeed-prod-rg
   ```

4. **Configurar autenticación**:
   ```powershell
   # Opción 1: Connection string
   $env:AZURE_STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=https;..."

   # Opción 2: az login (usa identidad actual)
   az login
   ```

#### Ejecución

```powershell
pwsh ./scripts/backup/db-backup.ps1 `
  -UploadAzure `
  -AzureContainer edufeedbackups
```

### Verificar subida

```powershell
# S3
aws s3 ls s3://edufeed-backups-prod/prod/daily/2025-10-31/

# Azure
az storage blob list \
  --container-name edufeedbackups \
  --account-name edufeedbackups \
  --prefix prod/daily/2025-10-31/
```

---

## Automatización

### Task Scheduler (Windows)

#### Crear tarea programada

1. Abrir **Task Scheduler** (taskschd.msc)
2. **Action → Create Task** (no "Create Basic Task")

**General**:
- Name: `EduFeed DB Backup Diario`
- User: `SYSTEM` (o usuario con permisos Docker)
- Run whether user is logged on or not: ✓
- Run with highest privileges: ✓

**Triggers**:
- New → Daily
- Start: 01:00 AM
- Recur every: 1 day
- Enabled: ✓

**Actions**:
- New → Start a program
- Program/script: `pwsh`
- Arguments:
  ```
  -NoProfile -ExecutionPolicy Bypass -File "C:\opt\edufeed\scripts\backup\db-backup.ps1" -EnvFile "C:\opt\edufeed\.env.prod" -ContainerName edufeed-db-prod -BackupRoot "C:\opt\edufeed\backups" -UploadS3 -S3Bucket edufeed-backups-prod -S3Prefix prod/
  ```

**Conditions**:
- Wake computer to run: ✓ (si es servidor físico)
- Start only if on AC power: ✗ (para servidores)

**Settings**:
- Allow task to run on demand: ✓
- Stop task if runs longer than: 1 hour
- If running task does not end when requested, force stop: ✓

3. **Guardar** y probar: Right-click → Run

#### Verificar ejecución

```powershell
# Ver últimas ejecuciones
Get-ScheduledTask -TaskName "EduFeed DB Backup Diario" | Get-ScheduledTaskInfo

# Ver logs
Get-EventLog -LogName Application -Source "Task Scheduler" -Newest 10
```

### Cron (Linux)

```bash
# Editar crontab del usuario deploy
sudo su - deploy
crontab -e

# Agregar línea (diario 01:00)
0 1 * * * /usr/bin/pwsh -NoProfile -File /opt/edufeed/scripts/backup/db-backup.ps1 -EnvFile /opt/edufeed/.env.prod -ContainerName edufeed-db-prod -BackupRoot /opt/edufeed/backups -UploadS3 -S3Bucket edufeed-backups-prod -S3Prefix prod/ >> /var/log/edufeed-backup.log 2>&1

# Verificar
crontab -l
```

### GitHub Actions (CI/CD - pre-deploy backup)

Agregar step en `.github/workflows/ci-cd.yml`:

```yaml
- name: Backup DB before deploy
  if: ${{ secrets.PROD_HOST != '' }}
  uses: appleboy/ssh-action@master
  with:
    host: ${{ secrets.PROD_HOST }}
    username: ${{ secrets.PROD_USER }}
    key: ${{ secrets.PROD_SSH_KEY }}
    script: |
      cd ${{ secrets.PROD_PATH }}
      pwsh -File scripts/backup/db-backup.ps1 \
        -EnvFile .env.prod \
        -ContainerName edufeed-db-prod \
        -BackupRoot ./backups \
        -UploadS3 \
        -S3Bucket edufeed-backups-prod \
        -S3Prefix prod/pre-deploy/
```

---

## Pruebas de restauración

### Objetivo

Validar que:
- Backups son restaurables (no corruptos)
- RTO ≤ 1h (tiempo de restauración completa)
- RPO ≤ 24h (datos recientes preservados)

### Procedimiento (staging)

1. **Levantar entorno de prueba**:
   ```powershell
   # En staging server
   cd /opt/edufeed-staging
   docker compose --env-file .env.stage -f docker-compose.prod.yml up -d db
   ```

2. **Descargar backup reciente**:
   ```powershell
   # Desde S3
   aws s3 cp s3://edufeed-backups-prod/prod/daily/2025-10-30/ ./backups/daily/2025-10-30/ --recursive

   # O usar backup local
   ```

3. **Restaurar**:
   ```powershell
   pwsh ./scripts/backup/db-restore.ps1 `
     -EnvFile .env.stage `
     -ContainerName edufeed-db-stage `
     -BackupFile ./backups/daily/2025-10-30/edufeed_prod_20251030_010203.dump `
     -DropAndCreate `
     -TargetDb edufeed_test
   ```

4. **Validar integridad**:
   ```sql
   -- Conectar a DB restaurada
   docker exec -it edufeed-db-stage psql -U edufeed_stage -d edufeed_test

   -- Verificar conteos
   SELECT COUNT(*) FROM users;  -- Debe coincidir con producción
   SELECT COUNT(*) FROM access_logs WHERE created_at >= CURRENT_DATE - INTERVAL '1 day';

   -- Verificar último registro
   SELECT MAX(created_at) FROM access_logs;
   -- Debe ser ~24h atrás (RPO)
   ```

5. **Levantar backend de prueba** (opcional):
   ```yaml
   # docker-compose.test.yml
   services:
     backend-test:
       image: edufeed-backend:latest
       environment:
         DB_URL: jdbc:postgresql://db:5432/edufeed_test
         DB_USER: edufeed_stage
         DB_PASSWORD: ...
       ports:
         - "8082:8080"
   ```

   ```powershell
   docker compose -f docker-compose.test.yml up -d
   curl http://localhost:8082/actuator/health
   ```

6. **Medir RTO**:
   - Inicio: timestamp inicio restauración
   - Fin: backend respondiendo 200 en /health
   - RTO real = Fin - Inicio
   - Objetivo: ≤ 1h

### Frecuencia de pruebas

- **Mensual**: restauración en staging (validación básica)
- **Trimestral**: drill completo (simular desastre, medir RTO/RPO)
- **Anual**: prueba con equipo completo (DRP completo)

---

## Plan de Recuperación (DRP)

### Disaster Recovery Plan

#### Escenarios

| Escenario | Severidad | RTO | RPO | Procedimiento |
|-----------|-----------|-----|-----|---------------|
| Corrupción de tabla | Baja | 30 min | 0 | Restore selectivo con pg_restore -t |
| Borrado accidental DB | Media | 1h | 24h | Restore completo desde backup diario |
| Fallo de disco (servidor) | Alta | 2h | 24h | Provisionar nuevo servidor, restore desde S3 |
| Ransomware | Crítica | 4h | 24h | Servidor nuevo, restore backup pre-infección |
| Desastre datacenter | Crítica | 8h | 24h | Provisionar en región alterna, restore S3 |

#### Contactos de emergencia

| Rol | Nombre | Contacto | Responsabilidad |
|-----|--------|----------|-----------------|
| DBA | Joan Mora | +57 xxx | Restauración DB |
| DevOps | TBD | +57 xxx | Infraestructura |
| Seguridad | TBD | +57 xxx | Análisis ransomware |
| Gerente TI | TBD | +57 xxx | Decisiones de negocio |

#### Runbook: Recuperación completa

**Prerequisitos**:
- Acceso a backups (S3/Azure)
- Servidor nuevo o restaurado
- Credenciales de producción

**Pasos**:

1. **Provisionar infraestructura** (15 min):
   ```bash
   # En nuevo servidor
   git clone https://github.com/Joan-Mora/EduFeed.git /opt/edufeed
   cd /opt/edufeed
   cp .env.prod.example .env.prod
   # Editar .env.prod con secretos (obtener de Vault o backup cifrado)
   ```

2. **Levantar PostgreSQL** (5 min):
   ```bash
   docker compose --env-file .env.prod -f docker-compose.prod.yml up -d db
   # Esperar healthcheck
   docker ps  # Verificar STATUS: healthy
   ```

3. **Descargar último backup** (10 min):
   ```bash
   # Desde S3
   aws s3 cp s3://edufeed-backups-prod/prod/daily/YYYY-MM-DD/ ./backups/ --recursive

   # Verificar checksum
   sha256sum -c backup.dump.sha256
   ```

4. **Restaurar DB** (20 min para 500 MB):
   ```bash
   pwsh ./scripts/backup/db-restore.ps1 \
     -EnvFile .env.prod \
     -ContainerName edufeed-db-prod \
     -BackupFile ./backups/daily/YYYY-MM-DD/edufeed_prod_YYYYMMDD_HHMMSS.dump \
     -DropAndCreate
   ```

5. **Levantar backend** (5 min):
   ```bash
   docker compose --env-file .env.prod -f docker-compose.prod.yml up -d backend pgadmin
   ```

6. **Validar** (5 min):
   ```bash
   # Health check
   curl https://edufeed.com/actuator/health

   # Query básico
   docker exec edufeed-db-prod psql -U edufeed_prod -d edufeed_prod -c "SELECT COUNT(*) FROM users;"

   # Login en UI
   # Acceso biométrico de prueba
   ```

7. **Actualizar DNS** (si IP cambió):
   ```bash
   # Actualizar registro A en proveedor DNS
   # edufeed.com → nueva_ip
   ```

8. **Comunicar a usuarios**:
   - Email/SMS: "Servicio restaurado. Si experimenta problemas, contactar soporte."

**Tiempo total estimado**: 60 min (objetivo RTO ≤ 1h ✓)

---

## Troubleshooting

### Error: "pg_dump: [archiver] connection to server failed"

**Causa**: Contenedor no está corriendo o DB no acepta conexiones.

**Solución**:
```powershell
# Verificar contenedor
docker ps -a | Select-String edufeed-db-prod

# Ver logs
docker logs edufeed-db-prod --tail 50

# Healthcheck manual
docker exec edufeed-db-prod pg_isready -U edufeed_prod
```

### Error: "Cannot remove directory: Permission denied"

**Causa**: Permisos insuficientes en carpeta de backups.

**Solución**:
```powershell
# Windows
icacls "C:\opt\edufeed\backups" /grant Users:F /t

# Linux
sudo chown -R deploy:deploy /opt/edufeed/backups
sudo chmod -R 755 /opt/edufeed/backups
```

### Error: "aws: command not found"

**Causa**: AWS CLI no instalado.

**Solución**:
```powershell
# Windows (Chocolatey)
choco install awscli

# Linux
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Verificar
aws --version
```

### Backup muy lento (> 1h para 500 MB)

**Causa**: Compresión -Z 9 demasiado intensiva o I/O lento.

**Solución**:
```powershell
# Reducir compresión
# En db-backup.ps1, cambiar -Z 9 a -Z 6
'-Z', '6',

# O deshabilitar compresión (backup más grande pero rápido)
'-Fc',  # sin -Z
```

### Restauración falla con "duplicate key violates unique constraint"

**Causa**: DB destino ya tiene datos (no se usó `-DropAndCreate`).

**Solución**:
```powershell
# Agregar -DropAndCreate
pwsh ./scripts/backup/db-restore.ps1 ... -DropAndCreate

# O limpiar manualmente
docker exec edufeed-db-prod psql -U edufeed_prod -d postgres -c "DROP DATABASE edufeed_prod;"
docker exec edufeed-db-prod psql -U edufeed_prod -d postgres -c "CREATE DATABASE edufeed_prod;"
```

---

## Mejores prácticas

### Seguridad

- ✅ **Cifrar backups** en tránsito (S3/Azure usan HTTPS)
- ✅ **Cifrar backups** en reposo (S3 SSE-AES256, Azure encryption)
- ✅ **Cifrar backups locales** con gpg (opcional):
  ```bash
  gpg --symmetric --cipher-algo AES256 backup.dump
  # Genera backup.dump.gpg
  ```
- ✅ **NO** versionar `.env.prod` con secretos
- ✅ Restringir acceso a bucket S3/Azure (IAM/RBAC)
- ✅ Habilitar versionado en S3 (protege contra borrado accidental)

### Monitoreo

- ✅ Alertar si backup falla (integrar con Prometheus/Grafana):
  ```powershell
  # Al final de db-backup.ps1
  if ($LASTEXITCODE -ne 0) {
    curl -X POST $env:SLACK_WEBHOOK -d '{"text":"❌ Backup falló"}'
  }
  ```
- ✅ Dashboard con métricas:
  - Última ejecución exitosa
  - Tamaño de backups (tendencia)
  - Tiempo de backup (tendencia)

### Testing

- ✅ Probar restore mensualmente
- ✅ Automatizar validación post-restore (script SQL con checks)
- ✅ Simular desastres (chaos engineering)

### Documentación

- ✅ Runbook actualizado con IPs/credenciales
- ✅ Changelog de cambios en estrategia de backup
- ✅ Post-mortem de incidentes (lecciones aprendidas)

---

## Checklist de implementación

- [x] Scripts `db-backup.ps1` y `db-restore.ps1` creados
- [x] Documentación en `docs/backup-restore.md`
- [x] Tasks de VS Code para backup/restore
- [ ] Backup diario programado (Task Scheduler/cron)
- [ ] Subida a S3 o Azure Blob configurada
- [ ] Lifecycle policy en S3/Azure (90 días)
- [ ] Cifrado habilitado en repositorio remoto
- [ ] Prueba de restauración exitosa (staging)
- [ ] RTO medido (≤ 1h)
- [ ] RPO validado (≤ 24h)
- [ ] Runbook de DRP documentado
- [ ] Contactos de emergencia actualizados
- [ ] Alertas de backup configuradas
- [ ] Prueba trimestral agendada

---

## Referencias

- [PostgreSQL Backup and Restore](https://www.postgresql.org/docs/current/backup.html)
- [pg_dump documentation](https://www.postgresql.org/docs/current/app-pgdump.html)
- [pg_restore documentation](https://www.postgresql.org/docs/current/app-pgrestore.html)
- [AWS S3 Lifecycle](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lifecycle-mgmt.html)
- [Azure Blob Lifecycle](https://learn.microsoft.com/en-us/azure/storage/blobs/lifecycle-management-overview)
- [Disaster Recovery Planning](https://www.ready.gov/it-disaster-recovery-plan)

---

**Última actualización**: 31 de octubre de 2025  
**Fase**: 8.4 - Backup y Recuperación  
**Estado**: ✅ Completado (automatización y pruebas pendientes)
