param()
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Resolve-Path (Join-Path $root '..')

Write-Host "[EduFeed:STAGING] Levantando PostgreSQL (DB=edufeed_staging) y pgAdmin..." -ForegroundColor Cyan

# Preparar .env.staging si no existe
$envFile = Join-Path $root '.env.staging'
if (-Not (Test-Path $envFile)) {
  @'
POSTGRES_DB=edufeed_staging
POSTGRES_USER=edufeed
POSTGRES_PASSWORD=edufeed
POSTGRES_PORT=5433
PGADMIN_DEFAULT_EMAIL=uat@local.test
PGADMIN_DEFAULT_PASSWORD=admin123
PGADMIN_PORT=5051
DB_URL=jdbc:postgresql://localhost:5433/edufeed_staging
DB_USER=edufeed
DB_PASSWORD=edufeed
PORT=8081
'@ | Out-File -FilePath $envFile -Encoding utf8
  Write-Host "Creado .env.staging con valores por defecto" -ForegroundColor Yellow
}

Push-Location $root
try {
  $env:COMPOSE_PROJECT_NAME = 'edufeed-staging'
  docker compose --env-file .env.staging up -d
  Write-Host "pgAdmin (staging) en http://localhost:5051" -ForegroundColor Green
}
finally { Pop-Location }
