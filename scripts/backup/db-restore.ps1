<#
.SYNOPSIS
  Restaura un backup lógico de PostgreSQL (pg_restore/psql) desde backups generados por db-backup.ps1.

.DESCRIPTION
  - Soporta formato personalizado (-Fc) usando pg_restore.
  - Ejecuta la restauración dentro del contenedor Docker de Postgres (docker exec).
  - Puede restaurar sobre una nueva base (drop/create) o sobre una existente.

.PARAMETER EnvFile
  Ruta al .env que contiene POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD (opcional).

.PARAMETER ContainerName
  Nombre del contenedor de Postgres. Por defecto 'edufeed-db'.

.PARAMETER BackupFile
  Ruta al archivo .dump a restaurar (local host).

.PARAMETER TargetDb
  Nombre de la base de datos destino. Si no se especifica, usa POSTGRES_DB del .env.

.PARAMETER DropAndCreate
  Si se indica, dropea y crea la base antes de restaurar.

.EXAMPLE
  ./scripts/backup/db-restore.ps1 -EnvFile .env.stage -ContainerName edufeed-db-stage -BackupFile ./backups/daily/2025-01-20/edufeed_20250120_010203.dump -DropAndCreate

.NOTES
  Requiere Docker. Asegúrate que el contenedor esté en ejecución.
#>
[CmdletBinding()]
param(
    [string]$EnvFile,
    [string]$ContainerName = 'edufeed-db',
    [Parameter(Mandatory = $true)][string]$BackupFile,
    [string]$TargetDb,
    [switch]$DropAndCreate
)

$ErrorActionPreference = 'Stop'

function Load-Env($path) {
    if (-not (Test-Path $path)) { return @{} }
    $dict = @{}
    foreach ($line in (Get-Content -Path $path)) {
        $line = $line.Trim()
        # Omitir líneas vacías o comentarios sin abortar la función completa
        if ($line -match '^(#|;|$)') { continue }
        $kv = $line -split '=', 2
        if ($kv.Length -eq 2) { $dict[$kv[0].Trim()] = $kv[1].Trim() }
    }
    return $dict
}

if (-not (Test-Path $BackupFile)) { throw "No existe el archivo: $BackupFile" }

if (-not $EnvFile) {
    $EnvFile = (Test-Path './.env.prod') ? './.env.prod' : (Test-Path './.env') ? './.env' : ''
}
$envVars = Load-Env $EnvFile

$POSTGRES_DB = $envVars['POSTGRES_DB']; if (-not $POSTGRES_DB) { $POSTGRES_DB = 'edufeed' }
$POSTGRES_USER = $envVars['POSTGRES_USER']; if (-not $POSTGRES_USER) { $POSTGRES_USER = 'edufeed' }
$POSTGRES_PASSWORD = $envVars['POSTGRES_PASSWORD']; if (-not $POSTGRES_PASSWORD) { $POSTGRES_PASSWORD = 'edufeed' }

if (-not $TargetDb) { $TargetDb = $POSTGRES_DB }

Write-Host "[i] Contenedor: $ContainerName | DB destino: $TargetDb" -ForegroundColor Cyan

$containerExists = (docker ps -a --format '{{.Names}}' | Where-Object { $_ -eq $ContainerName })
if (-not $containerExists) { throw "No se encontró el contenedor '$ContainerName'" }
$running = (docker inspect -f '{{.State.Running}}' $ContainerName)
if ($running -ne 'true') { throw "El contenedor '$ContainerName' no está en ejecución." }

# Copiar dump al contenedor
$filename = Split-Path $BackupFile -Leaf
$containerPath = "/tmp/$filename"
Write-Host "[i] Copiando backup al contenedor: $containerPath" -ForegroundColor Cyan
& docker cp "$BackupFile" "$($ContainerName):$containerPath"

# Opcional: dropear y crear DB
if ($DropAndCreate) {
    Write-Host "[i] Drop/Create database $TargetDb" -ForegroundColor Cyan
    try {
        & docker exec -e PGPASSWORD=$POSTGRES_PASSWORD $ContainerName psql -U $POSTGRES_USER -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$TargetDb' AND pid <> pg_backend_pid();" | Out-Null
        & docker exec -e PGPASSWORD=$POSTGRES_PASSWORD $ContainerName psql -U $POSTGRES_USER -d postgres -c "DROP DATABASE IF EXISTS \"$TargetDb\";" | Out-Null
        & docker exec -e PGPASSWORD=$POSTGRES_PASSWORD $ContainerName psql -U $POSTGRES_USER -d postgres -c "CREATE DATABASE \"$TargetDb\";" | Out-Null
    }
    catch {
        throw "No se pudo crear la base '$TargetDb'. Verifica que el usuario '$POSTGRES_USER' tenga permisos de superusuario. Detalle: $($_.Exception.Message)"
    }
}

# Detectar formato
$ext = [IO.Path]::GetExtension($BackupFile)
if ($ext -ieq '.dump') {
    Write-Host "[i] Restaurando con pg_restore (formato personalizado)" -ForegroundColor Cyan
    & docker exec -e PGPASSWORD=$POSTGRES_PASSWORD $ContainerName pg_restore -U $POSTGRES_USER -d $TargetDb --clean --if-exists --no-owner --no-privileges $containerPath
}
elseif ($ext -ieq '.sql') {
    Write-Host "[i] Restaurando con psql (script SQL)" -ForegroundColor Cyan
    & docker exec -e PGPASSWORD=$POSTGRES_PASSWORD $ContainerName psql -U $POSTGRES_USER -d $TargetDb -f $containerPath
}
else {
    throw "Extensión de backup no soportada: $ext (usa .dump o .sql)"
}

# Limpiar
& docker exec $ContainerName rm -f $containerPath | Out-Null

Write-Host "[OK] Restauración completada en '$TargetDb'" -ForegroundColor Green
