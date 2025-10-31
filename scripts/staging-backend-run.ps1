param()
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Resolve-Path (Join-Path $root '..')
Push-Location $root
try {
  $env:JAVA_HOME = 'C:\Program Files\Java\jdk-24'
  $env:SPRING_PROFILES_ACTIVE = 'staging'
  # Cargar variables de .env.staging si existe
  $envFile = Join-Path $root '.env.staging'
  if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
      if ($_ -match '^(?<k>[^#=]+)=(?<v>.*)$') {
        $k = $Matches['k'].Trim(); $v = $Matches['v']
        if (-not [string]::IsNullOrWhiteSpace($k)) { Set-Item -Path Env:$k -Value $v }
      }
    }
  }
  & "$env:USERPROFILE\tools\maven\apache-maven-3.9.9\bin\mvn.cmd" -q -f edufeed-backend/pom.xml spring-boot:run
}
finally { Pop-Location }
