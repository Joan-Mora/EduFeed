Param(
    [string]$ComposeFile = "docker-compose.yml",
    [switch]$RemoveVolumes,
    [switch]$Prune
)

Write-Host "[EduFeed] Deteniendo stack Docker ($ComposeFile)" -ForegroundColor Cyan

$volFlag = $RemoveVolumes.IsPresent ? "-v" : ""

docker compose -f $ComposeFile down $volFlag
if ($LASTEXITCODE -ne 0) { Write-Error "Fallo al detener los servicios"; exit 1 }

if ($Prune.IsPresent) {
    Write-Host "[EduFeed] Limpiando recursos huérfanos" -ForegroundColor Yellow
    docker image prune -f
    docker volume prune -f
    docker container prune -f
}

Write-Host "[EduFeed] Stack detenido" -ForegroundColor Green
