# Refresca MV y valida salidas en TZ America/Bogota
$ErrorActionPreference = 'Stop'
Get-Content "$PSScriptRoot/run_bogota.sql" | docker exec -i edufeed-db psql -U edufeed -d edufeed -v ON_ERROR_STOP=1 -t -A