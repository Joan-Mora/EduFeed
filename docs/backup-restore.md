# Copias de seguridad y restauración (8.4)

Este documento describe cómo ejecutar backups lógicos de PostgreSQL (pg_dump) con retención diaria/semanal/mensual y cómo restaurar.

## Scripts

- `scripts/backup/db-backup.ps1`:
  - Hace `pg_dump` dentro del contenedor Docker de Postgres (no requiere pg_dump instalado en el host).
  - Formato personalizado (-Fc) comprimido (-Z 9), adecuado para `pg_restore`.
  - Retención: conserva últimos 7 diarios, 4 semanales y 12 mensuales (configurable).
  - Opcional: sube a S3 (aws cli) o Azure Blob (az cli).

- `scripts/backup/db-restore.ps1`:
  - Restaura un `.dump` (pg_restore) o `.sql` (psql) dentro del contenedor.
  - Opción `-DropAndCreate` para dropear/crear la base de destino antes de restaurar.

Ambos scripts leen credenciales desde `.env` si se indica (POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD). Por defecto usan `.env.prod` si existe, de lo contrario `.env`.

## Requisitos

- Docker y Docker Compose v2.
- Contenedores corriendo:
  - Dev: `edufeed-db` (desde `docker-compose.yml`)
  - Prod: `edufeed-db-prod` (desde `docker-compose.prod.yml`)
- En Windows PowerShell 7+ (o Windows PowerShell), ejecuta con `pwsh`/`powershell`.

## Uso

Backups locales (dev):

```powershell
pwsh ./scripts/backup/db-backup.ps1 -EnvFile .env -ContainerName edufeed-db -BackupRoot ./backups
```

Backups en producción y subida a S3:

```powershell
pwsh ./scripts/backup/db-backup.ps1 -EnvFile .env.prod -ContainerName edufeed-db-prod -BackupRoot ./backups \
  -UploadS3 -S3Bucket my-bucket -S3Prefix edufeed/prod
```

Subida a Azure Blob:

```powershell
pwsh ./scripts/backup/db-backup.ps1 -EnvFile .env.prod -ContainerName edufeed-db-prod -UploadAzure -AzureContainer edufeedbackups -S3Prefix edufeed/prod
```

Restauración a una nueva base (staging/pruebas):

```powershell
pwsh ./scripts/backup/db-restore.ps1 -EnvFile .env.stage -ContainerName edufeed-db-stage \
  -BackupFile ./backups/daily/2025-01-20/edufeed_20250120_010203.dump -DropAndCreate -TargetDb edufeed_stage
```

Nota: Para restaurar sobre la misma base en ejecución, usa `-DropAndCreate` para evitar conflictos por conexiones activas. El script terminará conexiones previas de esa DB.

## Retención

Parámetros por defecto:
- Diaria: 7 días
- Semanal: 4 semanas (copias creadas los domingos)
- Mensual: 12 meses (copias creadas el día 1)

Puedes cambiar con `-RetentionDaily`, `-RetentionWeekly`, `-RetentionMonthly`.

## Automatización de backups (Windows)

Usa el Programador de tareas (Task Scheduler):
- Acción: `pwsh`
- Argumentos: `-NoProfile -ExecutionPolicy Bypass -File "C:/ruta/EduFeed/scripts/backup/db-backup.ps1" -EnvFile "C:/ruta/EduFeed/.env.prod" -ContainerName edufeed-db-prod -BackupRoot "C:/ruta/EduFeed/backups"`
- Programación: diario a la 01:00.

Asegura que AWS CLI o AZ CLI estén configurados si usas subida a nube.

## Prueba de restauración (RTO/RPO)

- RPO objetivo: ≤ 1h (programa backups cada hora o diario con incrementos si aplica en el futuro).
- RTO objetivo: ≤ 1h (restauración full con pg_restore sobre una DB vacía normalmente toma minutos para bases medianas; valida con tus datos reales).

Procedimiento sugerido:
1. Toma un backup.
2. Levanta un contenedor de base separado (o una DB nueva en el mismo contenedor con distinto nombre).
3. Ejecuta `db-restore.ps1` con `-DropAndCreate` hacia esa DB.
4. Verifica la aplicación conectando a esa base restaurada.

## Seguridad

- No subas `.env.prod` al repositorio. Contiene secretos.
- Considera cifrar backups en repositorio remoto (S3/Azure). Puedes usar bucket con cifrado server-side o agregar `gpg` paso adicional antes de subir.

## Problemas comunes

- "No se encontró el contenedor": levanta el stack con `docker compose up -d` y verifica el `container_name`.
- Permisos en Windows al escribir en `./backups`: ejecuta PowerShell con permisos o cambia `-BackupRoot` a una ruta accesible.
- Subida a S3/Azure falla: verifica autenticación (`aws configure` o `az login`/`AZURE_STORAGE_CONNECTION_STRING`).
