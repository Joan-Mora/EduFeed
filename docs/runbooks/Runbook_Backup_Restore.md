# Runbook: Backup y Restore
## Sistema EduFeed v2.0

**Versión**: 1.0  
**Fecha**: 31 de octubre de 2025  
**Responsable**: Administrador de Base de Datos / DevOps

---

## 📋 Tabla de Contenidos

1. [Backup Automático](#backup-automático)
2. [Backup Manual](#backup-manual)
3. [Restore Completo](#restore-completo)
4. [Verificación de Integridad](#verificación-de-integridad)
5. [Política de Retención](#política-de-retención)
6. [Troubleshooting](#troubleshooting)

---

## 🤖 Backup Automático

### Configuración

**Frecuencia**: Diaria a las 2:00 AM  
**Método**: Script PowerShell + Cron (Linux) o Task Scheduler (Windows)  
**Ubicación**: `c:\backups\edufeed\` o `/var/backups/edufeed/`  
**Retención**: 30 días (últimos 30 backups)

---

### Script de Backup Automático (Windows)

**Ubicación**: `scripts/backup-automatico.ps1`

```powershell
# Backup automático de EduFeed
$DATE = Get-Date -Format "yyyyMMdd_HHmmss"
$BACKUP_DIR = "C:\backups\edufeed"
$DB_NAME = "edufeed_db"
$DB_USER = "edufeed_user"
$DB_HOST = "localhost"
$DB_PORT = "5432"

# Crear directorio si no existe
if (-not (Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR
}

# Nombre del archivo de backup
$BACKUP_FILE = "$BACKUP_DIR\edufeed_backup_$DATE.sql"

# Configurar contraseña (almacenar en variable de entorno)
$env:PGPASSWORD = $env:EDUFEED_DB_PASSWORD

# Ejecutar pg_dump
Write-Host "Iniciando backup de $DB_NAME..."
& "C:\Program Files\PostgreSQL\14\bin\pg_dump.exe" `
    -h $DB_HOST `
    -p $DB_PORT `
    -U $DB_USER `
    -F c `
    -b `
    -v `
    -f $BACKUP_FILE `
    $DB_NAME

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Backup completado exitosamente: $BACKUP_FILE"
    
    # Comprimir con 7-Zip (opcional)
    & "C:\Program Files\7-Zip\7z.exe" a "$BACKUP_FILE.7z" $BACKUP_FILE
    Remove-Item $BACKUP_FILE
    
    # Eliminar backups antiguos (>30 días)
    Get-ChildItem $BACKUP_DIR -Filter "*.7z" | 
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-30) } | 
        Remove-Item -Force
    
    Write-Host "✓ Limpieza de backups antiguos completada"
} else {
    Write-Host "✗ Error en el backup. Código de salida: $LASTEXITCODE"
    exit 1
}
```

---

### Programar Tarea en Windows

```powershell
# Crear tarea programada
$Action = New-ScheduledTaskAction -Execute "PowerShell.exe" `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File C:\Users\Julia\OneDrive\Documentos\GitHub\EduFeed\scripts\backup-automatico.ps1"

$Trigger = New-ScheduledTaskTrigger -Daily -At 2:00AM

$Settings = New-ScheduledTaskSettingsSet `
    -RunOnlyIfNetworkAvailable `
    -StartWhenAvailable `
    -DontStopOnIdleEnd

Register-ScheduledTask -TaskName "EduFeed Backup Diario" `
    -Action $Action `
    -Trigger $Trigger `
    -Settings $Settings `
    -User "SYSTEM" `
    -RunLevel Highest `
    -Description "Backup automático diario de la base de datos EduFeed"
```

---

### Script de Backup Automático (Linux)

**Ubicación**: `scripts/backup-automatico.sh`

```bash
#!/bin/bash

# Configuración
DATE=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="/var/backups/edufeed"
DB_NAME="edufeed_db"
DB_USER="edufeed_user"
DB_HOST="localhost"
DB_PORT="5432"
RETENTION_DAYS=30

# Crear directorio si no existe
mkdir -p $BACKUP_DIR

# Nombre del archivo
BACKUP_FILE="$BACKUP_DIR/edufeed_backup_$DATE.sql"

# Ejecutar pg_dump
echo "Iniciando backup de $DB_NAME..."
PGPASSWORD=$EDUFEED_DB_PASSWORD pg_dump \
    -h $DB_HOST \
    -p $DB_PORT \
    -U $DB_USER \
    -F c \
    -b \
    -v \
    -f $BACKUP_FILE \
    $DB_NAME

if [ $? -eq 0 ]; then
    echo "✓ Backup completado: $BACKUP_FILE"
    
    # Comprimir
    gzip $BACKUP_FILE
    
    # Eliminar backups antiguos
    find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete
    echo "✓ Limpieza completada"
else
    echo "✗ Error en el backup"
    exit 1
fi
```

---

### Programar Cron en Linux

```bash
# Editar crontab
crontab -e

# Agregar línea (backup diario a las 2 AM)
0 2 * * * /path/to/edufeed/scripts/backup-automatico.sh >> /var/log/edufeed_backup.log 2>&1
```

---

## 🖱️ Backup Manual

### Desde pgAdmin

1. Abrir pgAdmin: `http://localhost:5050`
2. Login con credenciales
3. Conectar a servidor PostgreSQL
4. Click derecho en base de datos `edufeed_db`
5. Seleccionar "Backup..."
6. Configurar opciones:
   - **Filename**: `edufeed_backup_manual_YYYYMMDD.backup`
   - **Format**: Custom
   - **Encoding**: UTF8
   - **Role name**: edufeed_user
7. Tab "Data/Objects":
   - ✅ Blobs
   - ✅ Data
   - ✅ Pre-data (schema)
   - ✅ Post-data (indices, constraints)
8. Click "Backup"
9. Esperar confirmación
10. Verificar archivo generado

---

### Desde Línea de Comandos (Windows)

```powershell
# Definir variables
$DATE = Get-Date -Format "yyyyMMdd_HHmmss"
$BACKUP_FILE = "C:\backups\edufeed\manual\edufeed_manual_$DATE.backup"

# Configurar contraseña
$env:PGPASSWORD = "EduFeed_DB_P@ssw0rd!2025"

# Ejecutar pg_dump
& "C:\Program Files\PostgreSQL\14\bin\pg_dump.exe" `
    -h localhost `
    -p 5432 `
    -U edufeed_user `
    -F c `
    -b `
    -v `
    -f $BACKUP_FILE `
    edufeed_db

Write-Host "✓ Backup manual completado: $BACKUP_FILE"
```

---

### Desde Línea de Comandos (Linux)

```bash
# Definir variables
DATE=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="/var/backups/edufeed/manual/edufeed_manual_$DATE.backup"

# Ejecutar pg_dump
PGPASSWORD=EduFeed_DB_P@ssw0rd!2025 pg_dump \
    -h localhost \
    -p 5432 \
    -U edufeed_user \
    -F c \
    -b \
    -v \
    -f $BACKUP_FILE \
    edufeed_db

echo "✓ Backup manual completado: $BACKUP_FILE"
```

---

## 🔄 Restore Completo

### ⚠️ ADVERTENCIAS

- **ESTO ELIMINARÁ TODOS LOS DATOS ACTUALES**
- Solo ejecutar en caso de desastre o migración
- Hacer backup del estado actual antes de restore
- Verificar que nadie esté usando el sistema
- Detener aplicaciones que se conectan a la BD

---

### Procedimiento de Restore

#### Paso 1: Detener Aplicaciones

**Windows**:
```powershell
# Detener backend (si está como servicio)
Stop-Service -Name "EduFeedBackend"

# O matar proceso Java
Get-Process java | Stop-Process -Force
```

**Linux**:
```bash
# Detener servicio
sudo systemctl stop edufeed-backend

# O matar proceso
pkill -9 -f "edufeed-backend"
```

---

#### Paso 2: Desconectar Usuarios Activos

```sql
-- Conectar como superusuario
psql -U postgres

-- Terminar todas las conexiones a edufeed_db
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'edufeed_db'
  AND pid <> pg_backend_pid();
```

---

#### Paso 3: Eliminar Base de Datos Actual

```sql
-- Conectado como postgres
DROP DATABASE IF EXISTS edufeed_db;
```

---

#### Paso 4: Crear Nueva Base de Datos

```sql
-- Crear base de datos vacía
CREATE DATABASE edufeed_db
    WITH OWNER = edufeed_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- Dar permisos
GRANT ALL PRIVILEGES ON DATABASE edufeed_db TO edufeed_user;
```

---

#### Paso 5: Restaurar desde Backup

**Windows**:
```powershell
# Definir archivo de backup
$BACKUP_FILE = "C:\backups\edufeed\edufeed_backup_20251031_020000.backup"

# Configurar contraseña
$env:PGPASSWORD = "EduFeed_DB_P@ssw0rd!2025"

# Ejecutar pg_restore
& "C:\Program Files\PostgreSQL\14\bin\pg_restore.exe" `
    -h localhost `
    -p 5432 `
    -U edufeed_user `
    -d edufeed_db `
    -v `
    -c `
    --if-exists `
    $BACKUP_FILE

Write-Host "✓ Restore completado"
```

**Linux**:
```bash
# Definir archivo de backup
BACKUP_FILE="/var/backups/edufeed/edufeed_backup_20251031_020000.sql.gz"

# Descomprimir si está en .gz
gunzip -k $BACKUP_FILE

# Ejecutar pg_restore
PGPASSWORD=EduFeed_DB_P@ssw0rd!2025 pg_restore \
    -h localhost \
    -p 5432 \
    -U edufeed_user \
    -d edufeed_db \
    -v \
    -c \
    --if-exists \
    ${BACKUP_FILE%.gz}

echo "✓ Restore completado"
```

---

#### Paso 6: Verificar Restore

```sql
-- Conectar a base de datos
psql -U edufeed_user -d edufeed_db

-- Verificar tablas
\dt

-- Contar registros en tablas principales
SELECT 'usuarios' as tabla, COUNT(*) FROM usuarios
UNION ALL
SELECT 'pagos', COUNT(*) FROM pagos
UNION ALL
SELECT 'accesos', COUNT(*) FROM accesos
UNION ALL
SELECT 'huellas_dactilares', COUNT(*) FROM huellas_dactilares;
```

Resultado esperado:
```
   tabla         | count
-----------------+-------
 usuarios        |   150
 pagos           |   450
 accesos         |  1200
 huellas_dactilares |   120
```

---

#### Paso 7: Reiniciar Aplicaciones

**Windows**:
```powershell
# Iniciar backend
Start-Service -Name "EduFeedBackend"

# O ejecutar manualmente
cd C:\path\to\edufeed-backend
java -jar target\edufeed-backend-2.0.0.jar
```

**Linux**:
```bash
# Iniciar servicio
sudo systemctl start edufeed-backend

# Verificar estado
sudo systemctl status edufeed-backend
```

---

## ✅ Verificación de Integridad

### Checklist Post-Restore

- [ ] Backend inicia correctamente
- [ ] Desktop app se conecta a backend
- [ ] Login con usuario admin funciona
- [ ] Consulta de usuarios retorna datos
- [ ] Consulta de pagos retorna datos
- [ ] Verificación biométrica funciona (si hay huellas registradas)
- [ ] Reportes se generan correctamente
- [ ] No hay errores en logs del backend

---

### Verificar Consistencia de Datos

```sql
-- Conectar a base de datos
psql -U edufeed_user -d edufeed_db

-- Verificar integridad referencial
-- 1. Pagos deben tener usuario válido
SELECT COUNT(*) as pagos_huerfanos
FROM pagos p
LEFT JOIN usuarios u ON p.usuario_id = u.id
WHERE u.id IS NULL;
-- Esperado: 0

-- 2. Accesos deben tener usuario válido
SELECT COUNT(*) as accesos_huerfanos
FROM accesos a
LEFT JOIN usuarios u ON a.usuario_id = u.id
WHERE u.id IS NULL;
-- Esperado: 0

-- 3. Huellas deben tener usuario válido
SELECT COUNT(*) as huellas_huerfanas
FROM huellas_dactilares h
LEFT JOIN usuarios u ON h.usuario_id = u.id
WHERE u.id IS NULL;
-- Esperado: 0

-- 4. Verificar fechas coherentes
SELECT COUNT(*) as pagos_invalidos
FROM pagos
WHERE vigencia_inicio > vigencia_fin;
-- Esperado: 0
```

---

## 📅 Política de Retención

| Tipo de Backup | Frecuencia | Retención | Ubicación |
|----------------|------------|-----------|-----------|
| **Diario** | Todos los días 2:00 AM | 30 días | Local: `C:\backups\edufeed\` |
| **Semanal** | Domingos 3:00 AM | 12 semanas | Local + Cloud (opcional) |
| **Mensual** | Primer día del mes 4:00 AM | 12 meses | Cloud obligatorio |
| **Manual** | Bajo demanda | Indefinido | `C:\backups\edufeed\manual\` |

---

### Implementar Backup Semanal

```powershell
# Script: backup-semanal.ps1
$DATE = Get-Date -Format "yyyyMMdd"
$BACKUP_FILE = "C:\backups\edufeed\semanal\edufeed_semanal_$DATE.backup"

# Backup
& "C:\Program Files\PostgreSQL\14\bin\pg_dump.exe" -F c -f $BACKUP_FILE edufeed_db

# Copiar a cloud (ejemplo con Azure)
# az storage blob upload --account-name edufeedbackups --container-name weekly --file $BACKUP_FILE

# Limpiar locales >12 semanas
Get-ChildItem "C:\backups\edufeed\semanal\" -Filter "*.backup" | 
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-84) } | 
    Remove-Item -Force
```

**Programar**: Domingos a las 3:00 AM

---

## 🔧 Troubleshooting

### Problema: Backup falla con "permission denied"

**Causa**: Usuario no tiene permisos en directorio de destino

**Solución**:
```powershell
# Windows: Dar permisos al directorio
icacls "C:\backups\edufeed" /grant "Users:(OI)(CI)F" /T
```

```bash
# Linux: Cambiar permisos
sudo chown -R postgres:postgres /var/backups/edufeed
sudo chmod 755 /var/backups/edufeed
```

---

### Problema: Restore falla con "role does not exist"

**Causa**: Usuario `edufeed_user` no existe en PostgreSQL de destino

**Solución**:
```sql
-- Crear usuario si no existe
CREATE USER edufeed_user WITH PASSWORD 'EduFeed_DB_P@ssw0rd!2025';
ALTER USER edufeed_user WITH SUPERUSER;
```

---

### Problema: Backup muy grande (>1GB)

**Causa**: Muchos datos acumulados

**Solución**:
1. **Limpiar logs antiguos**:
```sql
DELETE FROM auditoria WHERE timestamp < NOW() - INTERVAL '6 months';
DELETE FROM accesos WHERE timestamp < NOW() - INTERVAL '1 year';
```

2. **Usar compresión agresiva**:
```powershell
& "C:\Program Files\7-Zip\7z.exe" a -t7z -mx=9 "$BACKUP_FILE.7z" $BACKUP_FILE
```

---

### Problema: Restore tarda mucho (>30 min)

**Causa**: Base de datos muy grande o servidor lento

**Solución**:
1. **Desactivar índices temporalmente** (solo durante restore):
```sql
-- Antes de restore
DROP INDEX IF EXISTS idx_pagos_usuario_id;
DROP INDEX IF EXISTS idx_accesos_timestamp;

-- Después de restore, recrear
CREATE INDEX idx_pagos_usuario_id ON pagos(usuario_id);
CREATE INDEX idx_accesos_timestamp ON accesos(timestamp);
```

2. **Aumentar parámetros de PostgreSQL** (`postgresql.conf`):
```ini
maintenance_work_mem = 256MB
shared_buffers = 512MB
```

---

## 📞 Contactos de Emergencia

| Situación | Contacto | Teléfono | Email |
|-----------|----------|----------|-------|
| Fallo de backup | DBA | [Número] | dba@edufeed.com |
| Necesidad de restore | DevOps Lead | [Número] | devops@edufeed.com |
| Pérdida de datos | Director TI | [Número] | ti@edufeed.com |

---

**Última actualización**: 31 de octubre de 2025  
**Próxima revisión**: Trimestral  
**Responsable**: Administrador de Base de Datos
