param()
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Resolve-Path (Join-Path $here '..')

Write-Host "[EduFeed:STAGING] Sembrando datos de ejemplo en edufeed_staging..." -ForegroundColor Cyan

$container = 'edufeed-db'
$dbName = $env:POSTGRES_DB
if ([string]::IsNullOrWhiteSpace($dbName)) { $dbName = 'edufeed_staging' }
$dbUser = $env:POSTGRES_USER
if ([string]::IsNullOrWhiteSpace($dbUser)) { $dbUser = 'edufeed' }

$seedPath = Join-Path $root 'scripts/seed/EduFeed_seed.sql'
if (-Not (Test-Path $seedPath)) {
  Throw "No se encontró el archivo de semilla: $seedPath"
}

Write-Host "Copiando seed al contenedor $container..." -ForegroundColor Yellow
docker cp "$seedPath" "${container}:/tmp/edufeed_seed.sql"

Write-Host "Ejecutando seed con psql..." -ForegroundColor Yellow
# Usar la variable de entorno dentro del contenedor para el password
# Ejecutar psql con PGPASSWORD en línea, evitando problemas de parsing
$innerCmd = "PGPASSWORD=`"`$POSTGRES_PASSWORD`" psql -v ON_ERROR_STOP=1 -U $dbUser -d $dbName -f /tmp/edufeed_seed.sql"
docker exec -i $container bash -lc "$innerCmd"

Write-Host "Semilla aplicada correctamente a $dbName" -ForegroundColor Green
