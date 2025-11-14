Param(
    [string]$ComposeFile = "docker-compose.yml",
    [switch]$Rebuild,
    [switch]$FollowLogs
)

Write-Host "[EduFeed] Iniciando stack Docker usando $ComposeFile" -ForegroundColor Cyan

$buildFlag = $Rebuild.IsPresent ? "--build" : ""

docker compose -f $ComposeFile up -d $buildFlag
if ($LASTEXITCODE -ne 0) { Write-Error "Fallo al levantar los servicios"; exit 1 }

Write-Host "[EduFeed] Servicios levantados:" -ForegroundColor Green

docker compose -f $ComposeFile ps

if ($FollowLogs) {
    Write-Host "[EduFeed] Siguiendo logs (Ctrl+C para salir)..." -ForegroundColor Yellow
    docker compose -f $ComposeFile logs -f --tail=200
}
