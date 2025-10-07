<#
.SYNOPSIS
  Prepara un entorno de desarrollo para EduFeed en Windows (PowerShell 7).
.DESCRIPTION
  - Instala (si faltan): Git, JDK 21, Maven 3.9, Docker Desktop, VS Code, OpenJFX SDK (opcional).
  - Verifica variables y prepara .env, levanta la DB opcionalmente.
.PARAMETER WithDb
  Si se especifica, levanta PostgreSQL/pgAdmin con Docker al finalizar.
#>
param(
    [switch]$WithDb
)

$ErrorActionPreference = 'Stop'

function Install-IfMissing($name, $wingetId) {
    if (Get-Command $name -ErrorAction SilentlyContinue) {
        Write-Host "✔ $name ya instalado" -ForegroundColor Green
        return
    }
    if ($null -ne $wingetId) {
        Write-Host "➜ Instalando $name con winget ($wingetId) ..." -ForegroundColor Cyan
        winget install --id $wingetId -e --source winget --accept-source-agreements --accept-package-agreements | Out-Null
    }
    else {
        Write-Warning "No se encontró $name ni se especificó wingetId. Instálalo manualmente."
    }
}

# Validaciones iniciales
if (-not $IsWindows) { throw 'Este script es para Windows PowerShell 7' }
if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    Write-Warning 'winget no está disponible. Instala winget o instala manualmente JDK/Maven/Docker/VS Code.'
}

Install-IfMissing git 'Git.Git'
Install-IfMissing java 'EclipseAdoptium.Temurin.21.JDK'
Install-IfMissing mvn $null  # Maven puede venir por VS Code for Java o instalarse manualmente
Install-IfMissing code 'Microsoft.VisualStudioCode'
Install-IfMissing docker 'Docker.DockerDesktop'

# Extensiones VS Code recomendadas
try {
    code --install-extension vscjava.vscode-java-pack --force | Out-Null
    code --install-extension vmware.vscode-boot-dev-pack --force | Out-Null
    code --install-extension ms-azuretools.vscode-docker --force | Out-Null
    code --install-extension redhat.vscode-xml --force | Out-Null
    code --install-extension GitHub.copilot-chat --force | Out-Null
}
catch { Write-Warning "No se pudieron instalar extensiones VS Code automáticamente: $_" }

# Preparar .env
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$envFile = Join-Path $repoRoot '.env'
$envExample = Join-Path $repoRoot '.env.example'
if (-not (Test-Path $envFile) -and (Test-Path $envExample)) {
    Copy-Item $envExample $envFile
    Write-Host 'Creado .env desde .env.example' -ForegroundColor Green
}

# Compilar proyecto (sin tests)
Push-Location $repoRoot
try {
    mvn -T1C -DskipTests package
    Write-Host 'Build Maven completado.' -ForegroundColor Green
}
catch {
    Write-Warning "Fallo durante la compilación Maven: $_"
}
finally { Pop-Location }

if ($WithDb) {
    & (Join-Path $repoRoot 'scripts/db-up.ps1')
}

Write-Host 'Setup de entorno completado.' -ForegroundColor Green
