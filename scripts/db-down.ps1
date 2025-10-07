param([switch]$Purge)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Resolve-Path (Join-Path $root '..')

Push-Location $root
try {
    if ($Purge) {
        docker compose down -v
    }
    else {
        docker compose down
    }
    Write-Host "[EduFeed] DB containers detenidos." -ForegroundColor Green
}
finally { Pop-Location }
