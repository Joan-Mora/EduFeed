param()
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Resolve-Path (Join-Path $root '..')

Write-Host "[EduFeed] Levantando PostgreSQL y pgAdmin con Docker..." -ForegroundColor Cyan

if (-Not (Test-Path (Join-Path $root '.env'))) {
  Write-Host "No existe .env; copiando desde .env.example" -ForegroundColor Yellow
  Copy-Item (Join-Path $root '.env.example') (Join-Path $root '.env')
}

Push-Location $root
try {
  docker compose up -d

  # Esperar a que el contenedor de Postgres esté healthy
  Write-Host "Esperando a que PostgreSQL esté listo (healthcheck)..." -ForegroundColor Yellow
  $maxAttempts = 40
  $attempt = 0
  do {
    Start-Sleep -Seconds 3
    $status = (docker inspect --format '{{.State.Health.Status}}' edufeed-db 2>$null)
    $attempt++
    Write-Host ("  intento {0}/{1}: estado={2}" -f $attempt, $maxAttempts, ($status | ForEach-Object { if ($_ -eq $null -or $_ -eq '') { 'unknown' } else { $_ } })) -ForegroundColor DarkGray
  } while ($attempt -lt $maxAttempts -and $status -ne 'healthy')

  if ($status -ne 'healthy') {
    Write-Warning "El contenedor de Postgres no alcanzó estado 'healthy' a tiempo. Puedes intentar de nuevo o revisar logs con 'docker logs edufeed-db'."
  }
  else {
    Write-Host "PostgreSQL está listo." -ForegroundColor Green
  }

  Write-Host "Listo. pgAdmin en http://localhost:5050" -ForegroundColor Green
}
finally { Pop-Location }
