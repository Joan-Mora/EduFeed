<#
.SYNOPSIS
  Backup lógico de PostgreSQL (pg_dump) con retención diaria/semanal/mensual y subida opcional a S3/Azure.

.DESCRIPTION
  - Obtiene credenciales desde un archivo .env (POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD) o parámetros.
  - Ejecuta pg_dump dentro del contenedor Docker de Postgres (docker exec) para evitar dependencias locales.
  - Crea backups comprimidos en formato personalizado (-Fc -Z 9) para pg_restore.
  - Retención: conserva últimos N diarios, N semanales y N mensuales.
  - Opcional: sube el backup a S3 (aws cli) o Azure Blob (az CLI).

.PARAMETER EnvFile
  Ruta al archivo .env con variables POSTGRES_* (por defecto ./.env.prod si existe, sino ./.env).

.PARAMETER ContainerName
  Nombre del contenedor Postgres. Dev: 'edufeed-db'. Prod: 'edufeed-db-prod'.

.PARAMETER BackupRoot
  Carpeta local para almacenar backups. Por defecto ./backups

.PARAMETER RetentionDaily
  Días de retención de backups diarios. Por defecto 7

.PARAMETER RetentionWeekly
  Semanas de retención de backups semanales. Por defecto 4

.PARAMETER RetentionMonthly
  Meses de retención de backups mensuales. Por defecto 12

.PARAMETER UploadS3
  Si se indica, sube el backup a S3 usando aws cli. Requiere -S3Bucket y credenciales configuradas.

.PARAMETER S3Bucket
  Bucket S3 de destino (ej. my-bucket)

.PARAMETER S3Prefix
  Prefijo en S3 (ej. edufeed/backups). Opcional.

.PARAMETER UploadAzure
  Si se indica, sube el backup a Azure Blob. Requiere -AzureContainer y autenticación (AZURE_STORAGE_CONNECTION_STRING o az login).

.PARAMETER AzureContainer
  Nombre del contenedor en Azure Blob Storage.

.EXAMPLE
  ./scripts/backup/db-backup.ps1 -EnvFile .env.prod -ContainerName edufeed-db-prod -UploadS3 -S3Bucket my-bucket -S3Prefix edufeed/prod

.NOTES
  Probado en PowerShell 7+ y Windows PowerShell. Requiere Docker.
#>
[CmdletBinding()]
param(
    [string]$EnvFile,
    [string]$ContainerName = 'edufeed-db',
    [string]$BackupRoot = './backups',
    [int]$RetentionDaily = 7,
    [int]$RetentionWeekly = 4,
    [int]$RetentionMonthly = 12,
    [switch]$UploadS3,
    [string]$S3Bucket,
    [string]$S3Prefix = '',
    [switch]$UploadAzure,
    [string]$AzureContainer
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

# Seleccionar .env por defecto
if (-not $EnvFile) {
    $EnvFile = (Test-Path './.env.prod') ? './.env.prod' : (Test-Path './.env') ? './.env' : ''
}
$envVars = Load-Env $EnvFile

$POSTGRES_DB = $envVars['POSTGRES_DB']; if (-not $POSTGRES_DB) { $POSTGRES_DB = 'edufeed' }
$POSTGRES_USER = $envVars['POSTGRES_USER']; if (-not $POSTGRES_USER) { $POSTGRES_USER = 'edufeed' }
$POSTGRES_PASSWORD = $envVars['POSTGRES_PASSWORD']; if (-not $POSTGRES_PASSWORD) { $POSTGRES_PASSWORD = 'edufeed' }

# Validar contenedor Docker
Write-Host "[i] Usando contenedor: $ContainerName" -ForegroundColor Cyan
$containerExists = (docker ps -a --format '{{.Names}}' | Where-Object { $_ -eq $ContainerName })
if (-not $containerExists) {
    throw "No se encontró el contenedor '$ContainerName'. Asegúrate de que docker-compose esté levantado."
}
$running = (docker inspect -f '{{.State.Running}}' $ContainerName)
if ($running -ne 'true') {
    throw "El contenedor '$ContainerName' no está en ejecución."
}

# Preparar rutas
$now = Get-Date
$timestamp = $now.ToString('yyyyMMdd_HHmmss')
$day = $now.ToString('yyyy-MM-dd')
# Cálculo correcto de semana ISO: yyyy-Www
try {
    $isoWeek = [System.Globalization.ISOWeek]::GetWeekOfYear($now)
}
catch {
    # Fallback en caso de que ISOWeek no esté disponible (PowerShell muy antiguo)
    $ci = [System.Globalization.CultureInfo]::InvariantCulture
    $calendar = $ci.Calendar
    $isoWeek = $calendar.GetWeekOfYear($now, [System.Globalization.CalendarWeekRule]::FirstFourDayWeek, [DayOfWeek]::Monday)
}
$week = ('{0:0000}-W{1:00}' -f $now.Year, $isoWeek)
$month = $now.ToString('yyyy-MM')
$dailyDir = Join-Path $BackupRoot "daily/$day"
$weeklyDir = Join-Path $BackupRoot "weekly/$week"
$monthlyDir = Join-Path $BackupRoot "monthly/$month"
$null = New-Item -ItemType Directory -Force -Path $dailyDir | Out-Null

$fileBase = "${POSTGRES_DB}_$timestamp.dump"
$localFile = Join-Path $dailyDir $fileBase
$containerFile = "/tmp/$fileBase"

# Ejecutar pg_dump dentro del contenedor
Write-Host "[i] Realizando pg_dump de '$POSTGRES_DB'..." -ForegroundColor Cyan
$dumpCmd = @(
    'exec',
    '-e', "PGPASSWORD=$POSTGRES_PASSWORD",
    $ContainerName,
    'pg_dump',
    '-U', $POSTGRES_USER,
    '-d', $POSTGRES_DB,
    '-h', 'localhost',
    '-Fc',
    '-Z', '9',
    '-f', $containerFile
)
& docker @dumpCmd | Out-Null

# Copiar a host
Write-Host "[i] Copiando backup al host: $localFile" -ForegroundColor Cyan
& docker cp "$($ContainerName):$containerFile" "$localFile"
# Limpiar archivo temporal en contenedor
& docker exec $ContainerName rm -f $containerFile | Out-Null

# Checksum
$sha = (Get-FileHash -Algorithm SHA256 $localFile).Hash
$shaFile = "$localFile.sha256"
$sha | Out-File -Encoding ascii $shaFile

# Semanal/mensual (copias)
if ($now.DayOfWeek -eq 'Sunday') {
    New-Item -ItemType Directory -Force -Path $weeklyDir | Out-Null
    Copy-Item $localFile -Destination (Join-Path $weeklyDir $fileBase)
    if (Test-Path $shaFile) { Copy-Item $shaFile -Destination (Join-Path $weeklyDir (Split-Path $shaFile -Leaf)) }
}
if ($now.Day -eq 1) {
    New-Item -ItemType Directory -Force -Path $monthlyDir | Out-Null
    Copy-Item $localFile -Destination (Join-Path $monthlyDir $fileBase)
    if (Test-Path $shaFile) { Copy-Item $shaFile -Destination (Join-Path $monthlyDir (Split-Path $shaFile -Leaf)) }
}

# Retención: mantener últimos N
function Trim-Old($path, $keep) {
    if (-not (Test-Path $path)) { return }
    $dirs = Get-ChildItem $path -Directory | Sort-Object Name -Descending
    $toDelete = $dirs | Select-Object -Skip $keep
    foreach ($d in $toDelete) {
        Write-Host "[i] Eliminando antiguo: $($d.FullName)" -ForegroundColor DarkYellow
        Remove-Item -Recurse -Force $d.FullName
    }
}
Trim-Old (Join-Path $BackupRoot 'daily') $RetentionDaily
Trim-Old (Join-Path $BackupRoot 'weekly') $RetentionWeekly
Trim-Old (Join-Path $BackupRoot 'monthly') $RetentionMonthly

# Subidas opcionales
if ($UploadS3) {
    if (-not $S3Bucket) { throw 'Debe especificar -S3Bucket para subir a S3.' }
    $key = ($S3Prefix -ne '') ? "$S3Prefix/daily/$day/$fileBase" : "daily/$day/$fileBase"
    Write-Host "[i] Subiendo a s3://$S3Bucket/$key" -ForegroundColor Cyan
    aws s3 cp "$localFile" "s3://$S3Bucket/$key" --only-show-errors
}

if ($UploadAzure) {
    if (-not $AzureContainer) { throw 'Debe especificar -AzureContainer para subir a Azure Blob.' }
    $blobPath = ($S3Prefix -ne '') ? "$S3Prefix/daily/$day/$fileBase" : "daily/$day/$fileBase"
    Write-Host "[i] Subiendo a Azure Blob: container=$AzureContainer blob=$blobPath" -ForegroundColor Cyan
    az storage blob upload --no-progress --overwrite --container-name $AzureContainer --name $blobPath --file "$localFile" | Out-Null
}

Write-Host "[OK] Backup exitoso: $localFile" -ForegroundColor Green
