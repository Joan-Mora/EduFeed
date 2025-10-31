# Guía de Troubleshooting - Sistema EduFeed

**Versión**: 2.0  
**Fecha**: 31 de octubre de 2025  
**Audiencia**: Administradores de sistemas, DevOps, Desarrolladores

---

## Índice

1. [Introducción](#introducción)
2. [Problemas de compilación](#problemas-de-compilación)
3. [Problemas de Docker](#problemas-de-docker)
4. [Problemas de base de datos](#problemas-de-base-de-datos)
5. [Problemas de despliegue CI/CD](#problemas-de-despliegue-cicd)
6. [Problemas de observabilidad](#problemas-de-observabilidad)
7. [Problemas de backups](#problemas-de-backups)
8. [Problemas de la aplicación](#problemas-de-la-aplicación)
9. [Problemas de hardware biométrico](#problemas-de-hardware-biométrico)
10. [Diagnóstico general](#diagnóstico-general)
11. [Checklist de verificación](#checklist-de-verificación)

---

## Introducción

Esta guía consolida todos los problemas comunes documentados en las fases de desarrollo del sistema EduFeed. Cada problema está estructurado con:

- **Síntoma**: Qué se observa
- **Causa**: Por qué ocurre
- **Diagnóstico**: Cómo investigar
- **Solución**: Cómo resolverlo
- **Prevención**: Cómo evitarlo en el futuro

### Formato de comandos

```bash
# Linux/Mac
comando_linux

# Windows PowerShell
comando_powershell
```

---

## Problemas de compilación

### Maven: "Cannot resolve dependency"

**Síntoma**: Build falla con error de dependencia no encontrada.

**Causa**: Módulos `edufeed-common` o `edufeed-biometric` no compilados antes de `edufeed-backend` o `edufeed-desktop`.

**Diagnóstico**:

```powershell
# Verificar repositorio local Maven
ls ~/.m2/repository/co/cellano/
# Debe contener edufeed-common, edufeed-biometric
```

**Solución**:

```powershell
# Compilar en orden correcto
mvn clean install -DskipTests

# O compilar módulos comunes primero
cd edufeed-common
mvn clean install
cd ../edufeed-biometric
mvn clean install
cd ../edufeed-backend
mvn clean package
```

**Prevención**: Usar Maven multi-module build con reactor order correcto (ya configurado en pom.xml raíz).

---

### Error: "Class not found: java.lang.UnsupportedClassVersionError"

**Síntoma**: Compilación o ejecución falla con error de versión de clase.

**Causa**: JDK/JRE versión incorrecta (requiere JDK 24).

**Diagnóstico**:

```powershell
# Verificar versión Java
java -version
javac -version

# Verificar JAVA_HOME
echo $env:JAVA_HOME  # Windows
echo $JAVA_HOME      # Linux
```

**Solución**:

```powershell
# Windows
$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Linux
export JAVA_HOME=/opt/jdk-24
export PATH=$JAVA_HOME/bin:$PATH

# Verificar
java -version
# openjdk version "24" ...
```

**Prevención**: Documentar versión en README y validar en scripts de build.

---

### Maven: "Tests failed"

**Síntoma**: `mvn test` falla pero tests pasan en IDE.

**Causa**: Diferencias de entorno (zona horaria, base de datos, configuración).

**Diagnóstico**:

```powershell
# Ver detalles de falla
mvn test -X  # modo debug

# Ver reporte Surefire
cat edufeed-backend/target/surefire-reports/*.txt
```

**Solución**:

```powershell
# Asegurar Testcontainers funciona
docker ps
# Debe mostrar contenedores de test (postgres)

# Configurar zona horaria
# En pom.xml o application-test.yml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC

# Ejecutar con profile de test
mvn test -Dspring.profiles.active=test
```

**Prevención**: Usar Testcontainers para tests de integración (ya implementado).

---

## Problemas de Docker

### Error: "Cannot connect to Docker daemon"

**Síntoma**: `docker ps` falla con error de conexión.

**Causa**: Docker Desktop no está corriendo o permisos insuficientes.

**Diagnóstico**:

```powershell
# Verificar estado Docker
docker info
# Si falla: Docker no está corriendo

# Windows: verificar Docker Desktop
Get-Process | Where-Object { $_.Name -like "*Docker*" }

# Linux: verificar servicio
sudo systemctl status docker
```

**Solución**:

```powershell
# Windows: iniciar Docker Desktop manualmente
# O desde PowerShell
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# Linux: iniciar servicio
sudo systemctl start docker

# Agregar usuario a grupo docker (Linux)
sudo usermod -aG docker $USER
newgrp docker  # o logout/login
```

**Prevención**: Configurar Docker para iniciar al boot del sistema.

---

### Error: "port is already allocated"

**Síntoma**: `docker compose up` falla con error de puerto ocupado.

**Causa**: Puerto (5432, 8080, 5050, etc.) ocupado por otra aplicación.

**Diagnóstico**:

```powershell
# Windows: ver qué proceso usa el puerto
netstat -ano | findstr :8080
# Última columna es PID

tasklist | findstr <PID>

# Linux
sudo lsof -i :8080
# Muestra proceso
```

**Solución**:

```powershell
# Opción 1: Detener proceso conflictivo
# Windows
taskkill /PID <PID> /F

# Linux
sudo kill -9 <PID>

# Opción 2: Cambiar puerto en .env
PORT=8081

# Opción 3: Detener contenedores anteriores
docker compose down
```

**Prevención**: Usar puertos no estándar (ej. 8081 para staging) o detener servicios antes de levantar.

---

### Error: "backend exited with code 1"

**Síntoma**: Contenedor de backend se detiene inmediatamente.

**Causa**: Backend no puede conectar a DB, configuración incorrecta o migraciones Flyway fallan.

**Diagnóstico**:

```powershell
# Ver logs detallados
docker compose logs backend

# Buscar errores específicos
docker compose logs backend | Select-String "ERROR"

# Verificar DB está healthy
docker ps
# STATUS de edufeed-db debe ser "healthy"

# Verificar variables de entorno
docker exec edufeed-backend env | grep DB_

# Probar conexión manual a DB
docker exec -it edufeed-db psql -U edufeed -d edufeed -c "SELECT 1;"
```

**Solución**:

```powershell
# 1. Verificar .env tiene credenciales correctas
DB_URL=jdbc:postgresql://db:5432/edufeed
DB_USER=edufeed
DB_PASSWORD=edufeed

# 2. Esperar a que DB esté healthy
docker compose up -d db
# Esperar 30 segundos
docker compose up -d backend

# 3. Ver logs Flyway específicamente
docker compose logs backend | Select-String "Flyway"

# 4. Si Flyway falla, ver siguiente sección
```

**Prevención**: Usar `depends_on` con `condition: service_healthy` en docker-compose.yml (ya configurado).

---

### Error: "Flyway baseline required"

**Síntoma**: Backend falla con `FlywayException: Found non-empty schema(s) "public" but no schema history table`.

**Causa**: DB existente sin metadata Flyway (migración desde schema manual).

**Diagnóstico**:

```powershell
# Conectar a DB
docker exec -it edufeed-db psql -U edufeed -d edufeed

# Verificar si existe tabla flyway_schema_history
\dt flyway_schema_history

# Si no existe: necesita baseline
```

**Solución**:

```powershell
# Opción 1: Configurar baselining en .env
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
SPRING_FLYWAY_BASELINE_VERSION=0

# Recrear backend
docker compose up -d --force-recreate backend

# Opción 2: Baseline manual (si Opción 1 no funciona)
docker exec edufeed-db psql -U edufeed -d edufeed
```

**SQL**:

```sql
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT now(),
    execution_time INT NOT NULL,
    success BOOLEAN NOT NULL,
    PRIMARY KEY (installed_rank)
);

INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (1, '0', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'edufeed', now(), 0, true);
```

**Prevención**: Iniciar DB vacía o usar Flyway desde el principio.

---

### Error: "OOM (Out of Memory)" en contenedor backend

**Síntoma**: Contenedor se reinicia constantemente, logs muestran `java.lang.OutOfMemoryError`.

**Causa**: JVM usa más memoria que límite del contenedor.

**Diagnóstico**:

```powershell
# Ver uso de memoria del contenedor
docker stats edufeed-backend

# Ver logs de OOM
docker logs edufeed-backend | Select-String "OutOfMemoryError"

# Ver límites configurados
docker inspect edufeed-backend | Select-String "Memory"
```

**Solución**:

```yaml
# En docker-compose.prod.yml, agregar:
services:
  backend:
    environment:
      JAVA_OPTS: "-Xms512m -Xmx1g -XX:MaxMetaspaceSize=256m"
    deploy:
      resources:
        limits:
          memory: 1.5G
        reservations:
          memory: 512M
```

```powershell
# Aplicar cambios
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --force-recreate backend
```

**Prevención**: Monitorear uso de memoria con Prometheus/Grafana.

---

## Problemas de base de datos

### DB no acepta conexiones

**Síntoma**: `FATAL: password authentication failed for user "edufeed"`.

**Causa**: Contraseña incorrecta o usuario no existe.

**Diagnóstico**:

```powershell
# Verificar variables de entorno
docker exec edufeed-db env | grep POSTGRES

# Intentar conexión manual
docker exec -it edufeed-db psql -U edufeed -d edufeed
# Si pide password, probar con valor de .env
```

**Solución**:

```powershell
# Recrear DB con credenciales correctas
docker compose down -v  # -v elimina volúmenes
docker compose up -d db

# Esperar a que esté healthy
docker ps

# Probar conexión
docker exec -it edufeed-db psql -U edufeed -d edufeed -c "SELECT version();"
```

**Prevención**: Documentar credenciales en `.env.example` y usar secretos en producción.

---

### Error: "FATAL: database 'edufeed_prod' does not exist"

**Síntoma**: Backend no inicia, logs muestran DB no existe.

**Causa**: DB no fue creada al inicializar contenedor.

**Diagnóstico**:

```powershell
# Listar DBs
docker exec edufeed-db psql -U postgres -c "\l"
# Buscar edufeed_prod
```

**Solución**:

```powershell
# Crear DB manualmente
docker exec edufeed-db psql -U postgres -c "CREATE DATABASE edufeed_prod OWNER edufeed_prod;"

# O recrear contenedor
docker compose down -v
docker compose up -d db
```

**Prevención**: Asegurar `POSTGRES_DB` en docker-compose.yml coincide con `DB_URL` en backend.

---

### DB lenta (queries > 5s)

**Síntoma**: Aplicación responde lento, logs muestran queries lentas.

**Causa**: Falta de índices, tablas muy grandes sin particionado, o consultas N+1.

**Diagnóstico**:

```sql
-- Conectar a DB
docker exec -it edufeed-db psql -U edufeed_prod -d edufeed_prod

-- Ver queries lentas (requiere pg_stat_statements)
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Ver tablas grandes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Ver índices faltantes
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats
WHERE schemaname = 'public'
  AND n_distinct > 100
  AND correlation < 0.1;
```

**Solución**:

```sql
-- Crear índices para queries frecuentes
-- Ejemplo: búsqueda por documento
CREATE INDEX IF NOT EXISTS idx_usuarios_documento ON usuarios(documento);

-- Ejemplo: filtros en accesos
CREATE INDEX IF NOT EXISTS idx_accesos_usuario_fecha ON accesos(usuario_id, fecha_hora DESC);

-- Ejemplo: reportes de pagos
CREATE INDEX IF NOT EXISTS idx_pagos_fecha_tipo ON pagos(fecha_pago, tipo);

-- VACUUM para liberar espacio
VACUUM ANALYZE;
```

**Prevención**: Revisar execution plans con `EXPLAIN ANALYZE` antes de desplegar queries complejas.

---

## Problemas de despliegue CI/CD

### Error: "Permission denied (publickey)" en deploy SSH

**Síntoma**: GitHub Actions falla al hacer SSH al servidor staging/prod.

**Causa**: Clave SSH incorrecta o no agregada al servidor.

**Diagnóstico**:

```bash
# En servidor staging/prod, verificar authorized_keys
ssh deploy@staging.edufeed.com
cat ~/.ssh/authorized_keys
# Debe contener la clave pública correspondiente a STAGING_SSH_KEY

# Verificar permisos
ls -la ~/.ssh/
# .ssh debe ser 700, authorized_keys 600
```

**Solución**:

```bash
# En servidor
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys

# Verificar owner
chown -R deploy:deploy ~/.ssh

# Probar SSH desde local
ssh -i ~/.ssh/github_actions_edufeed deploy@staging.edufeed.com

# Si falla, regenerar clave
ssh-keygen -t ed25519 -C "github-actions-edufeed" -f ~/.ssh/github_actions_edufeed
# Agregar pública a authorized_keys
cat ~/.ssh/github_actions_edufeed.pub >> ~/.ssh/authorized_keys
# Agregar privada a GitHub Secrets
```

**Prevención**: Documentar proceso de setup de claves SSH en manual de instalación.

---

### Error: "docker: command not found" en SSH

**Síntoma**: Workflow falla al ejecutar `docker compose` vía SSH.

**Causa**: Docker no está en PATH del usuario SSH no-login.

**Diagnóstico**:

```bash
# SSH al servidor
ssh deploy@staging.edufeed.com

# Verificar Docker
which docker
# /usr/bin/docker

# Probar no-login shell
ssh deploy@staging.edufeed.com 'which docker'
# Si vacío: PATH incorrecto
```

**Solución**:

```yaml
# En workflow, agregar export PATH antes de docker
- name: Deploy to Staging
  run: |
    ssh ${{ secrets.STAGING_SSH_USER }}@${{ secrets.STAGING_SSH_HOST }} << 'EOF'
      export PATH=/usr/local/bin:/usr/bin:$PATH
      cd /opt/edufeed
      docker compose pull
      docker compose up -d
    EOF
```

**Prevención**: Agregar PATH en `.bashrc` o `.profile` del usuario deploy.

---

### Error: "docker login ghcr.io: denied"

**Síntoma**: GitHub Actions no puede pushear imagen a GHCR.

**Causa**: `GITHUB_TOKEN` sin permisos `packages: write`.

**Diagnóstico**:

```yaml
# Ver logs de workflow
# Buscar:
# Error: denied: permission_denied: write_package
```

**Solución**:

```yaml
# En .github/workflows/deploy.yml, agregar en job docker-build-push:
jobs:
  docker-build-push:
    permissions:
      contents: read
      packages: write
    steps:
      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
```

**Prevención**: Template de workflow con permisos correctos.

---

### Error: "Health check failed" post-deploy

**Síntoma**: Deploy exitoso pero health check falla.

**Causa**: Backend tarda más de 10s en iniciar o puerto incorrecto.

**Diagnóstico**:

```bash
# En servidor, verificar backend
docker ps
# edufeed-backend debe estar "Up"

docker logs edufeed-backend --tail 50
# Buscar "Started EdufeedBackendApplication"

# Probar health endpoint manualmente
curl http://localhost:8080/actuator/health
```

**Solución**:

```yaml
# En workflow, aumentar timeout
- name: Health Check
  run: |
    sleep 30  # aumentar de 10 a 30
    curl --fail --retry 5 --retry-delay 10 http://staging:8081/actuator/health
```

**Prevención**: Configurar readiness probe en backend y esperarla antes de health check.

---

### Pipeline muy lento (> 15 minutos)

**Síntoma**: Workflow tarda mucho en completar.

**Causa**: Maven descarga dependencias cada vez, sin cache.

**Diagnóstico**:

```yaml
# Ver logs de workflow
# Buscar:
# Downloading from central: https://repo.maven.apache.org/...
# Si aparece muchas veces: no hay cache
```

**Solución**:

```yaml
# Habilitar cache Maven (ya implementado)
- name: Set up JDK 24
  uses: actions/setup-java@v4
  with:
    java-version: '24'
    distribution: 'temurin'
    cache: 'maven'  # crucial

# O cache manual
- name: Cache Maven packages
  uses: actions/cache@v3
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: |
      ${{ runner.os }}-maven-
```

**Prevención**: Usar cache en todos los workflows.

---

## Problemas de observabilidad

### Prometheus no scrapea backend

**Síntoma**: Target "edufeed-backend" DOWN en http://localhost:9090/targets.

**Diagnóstico**:

```powershell
# Verificar backend expone /actuator/prometheus
curl http://localhost:8080/actuator/prometheus

# Verificar red Docker
docker network inspect edufeed_default
# backend y prometheus deben estar en misma red

# Verificar configuración Prometheus
docker exec prometheus cat /etc/prometheus/prometheus.yml | Select-String "edufeed-backend"

# Ver logs Prometheus
docker logs prometheus | Select-String "backend"
```

**Solución**:

```yaml
# En application.yml, asegurar:
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

# En prometheus.yml, usar nombre DNS del servicio:
scrape_configs:
  - job_name: 'edufeed-backend'
    static_configs:
      - targets: ['backend:8080']  # NO localhost

# Recrear Prometheus
docker compose -f docker-compose.observability.yml up -d --force-recreate prometheus
```

**Prevención**: Validar targets en Prometheus UI después de cada deploy.

---

### Grafana no muestra datos

**Síntoma**: Dashboard vacío o "No data".

**Diagnóstico**:

```powershell
# Verificar datasource en Grafana
# UI: http://localhost:3000
# Connections → Data sources → Prometheus → Test
# Debe decir "Data source is working"

# Probar query manual en Explore
# Paste: up{job="edufeed-backend"}
# Debe retornar 1
```

**Solución**:

```powershell
# Opción 1: Reconfigurar datasource
# Grafana UI → Connections → Data sources → Prometheus
# URL: http://prometheus:9090  # NO localhost
# Access: Server (Default)
# Save & test

# Opción 2: Verificar UID en dashboard JSON
# Abrir dashboard JSON
# Buscar "datasource": { "uid": "PROM_DS" }
# Debe coincidir con UID en datasource (ver en URL: /connections/datasources/edit/PROM_DS)

# Opción 3: Verificar time range
# Dashboard → Time range picker → Last 24 hours
# Si no hay tráfico reciente, no habrá datos
```

**Prevención**: Importar dashboards desde UI, no manualmente editando JSON.

---

### Alertas no se disparan

**Síntoma**: Métrica supera umbral pero no hay alerta en Slack.

**Diagnóstico**:

```promql
# En Prometheus, evaluar manualmente la regla
# http://localhost:9090/graph
# Paste query de la alerta, ej:
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket{job="edufeed-backend"}[5m])) by (le)
) > 1

# Verificar reglas cargadas
# http://localhost:9090/rules
# Debe aparecer la regla, estado "OK" o "FIRING"

# Verificar Alertmanager recibe
# http://localhost:9093/#/alerts
```

**Solución**:

```yaml
# Revisar sintaxis de regla en prometheus-rules.yml
groups:
  - name: edufeed_alerts
    interval: 30s
    rules:
      - alert: HighLatency
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket{job="edufeed-backend"}[5m])) by (le)
          ) > 1
        for: 5m  # Debe cumplirse por 5 min
        labels:
          severity: warning
        annotations:
          summary: "Alta latencia en backend"

# Verificar Alertmanager configurado en prometheus.yml
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

# Recrear Prometheus
docker compose -f docker-compose.observability.yml up -d --force-recreate prometheus
```

**Prevención**: Testear alertas con queries que fuerzan FIRING.

---

### Filebeat no envía logs a Elasticsearch

**Síntoma**: Kibana no muestra logs de backend.

**Diagnóstico**:

```powershell
# Verificar Filebeat corriendo
docker ps | Select-String filebeat

# Ver logs Filebeat
docker logs filebeat | Select-String "ERROR"

# Verificar Elasticsearch accesible
curl http://localhost:9200
# Debe retornar JSON con cluster info

# Verificar índices en Elasticsearch
curl http://localhost:9200/_cat/indices?v
# Buscar filebeat-*
```

**Solución**:

```yaml
# Verificar filebeat.yml
output.elasticsearch:
  hosts: ["elasticsearch:9200"]  # NO localhost
  index: "filebeat-%{+yyyy.MM.dd}"

# Recrear Filebeat
docker compose -f docker-compose.observability.yml up -d --force-recreate filebeat

# Forzar logs
curl http://localhost:8080/api/usuarios
# Generar actividad

# Verificar en Kibana
# Discover → Create data view → Index pattern: filebeat-*
```

**Prevención**: Monitorear índices de Elasticsearch con alerta si no hay nuevos docs.

---

## Problemas de backups

### Error: "pg_dump: connection to server failed"

**Síntoma**: Script de backup falla al conectar a DB.

**Causa**: Contenedor no está corriendo o DB no acepta conexiones.

**Diagnóstico**:

```powershell
# Verificar contenedor
docker ps -a | Select-String edufeed-db-prod

# Ver estado
docker inspect edufeed-db-prod | Select-String "Status"

# Ver logs
docker logs edufeed-db-prod --tail 50

# Healthcheck manual
docker exec edufeed-db-prod pg_isready -U edufeed_prod
```

**Solución**:

```powershell
# Reiniciar contenedor
docker compose -f docker-compose.prod.yml restart db

# Esperar a que esté healthy
Start-Sleep -Seconds 30

# Probar conexión
docker exec edufeed-db-prod psql -U edufeed_prod -d edufeed_prod -c "SELECT 1;"

# Ejecutar backup nuevamente
pwsh ./scripts/backup/db-backup.ps1 -ContainerName edufeed-db-prod
```

**Prevención**: Agregar healthcheck en script antes de pg_dump.

---

### Error: "Cannot remove directory: Permission denied"

**Síntoma**: Script de rotación de backups falla al eliminar backups antiguos.

**Causa**: Permisos insuficientes en carpeta de backups.

**Diagnóstico**:

```powershell
# Windows
icacls "C:\opt\edufeed\backups"

# Linux
ls -la /opt/edufeed/backups
```

**Solución**:

```powershell
# Windows
icacls "C:\opt\edufeed\backups" /grant Users:F /t

# Linux
sudo chown -R deploy:deploy /opt/edufeed/backups
sudo chmod -R 755 /opt/edufeed/backups
```

**Prevención**: Documentar permisos en manual de instalación.

---

### Error: "aws: command not found"

**Síntoma**: Script falla al subir backup a S3.

**Causa**: AWS CLI no instalado.

**Diagnóstico**:

```powershell
# Verificar AWS CLI
aws --version
# Si no existe: instalar
```

**Solución**:

```powershell
# Windows (Chocolatey)
choco install awscli

# Linux
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Configurar credenciales
aws configure
# AWS Access Key ID: AKIA...
# AWS Secret Access Key: ...
# Default region: us-east-1

# Probar
aws s3 ls s3://edufeed-backups-prod/
```

**Prevención**: Incluir AWS CLI en checklist de instalación.

---

### Backup muy lento (> 1 hora para 500 MB)

**Síntoma**: Script de backup tarda demasiado.

**Causa**: Compresión `-Z 9` muy intensiva o I/O lento.

**Diagnóstico**:

```powershell
# Medir tiempo de backup sin compresión
Measure-Command {
  docker exec edufeed-db-prod pg_dump -U edufeed_prod -Fc -d edufeed_prod > test.dump
}

# Comparar con compresión
Measure-Command {
  docker exec edufeed-db-prod pg_dump -U edufeed_prod -Fc -Z 9 -d edufeed_prod > test_z9.dump
}
```

**Solución**:

```powershell
# En scripts/backup/db-backup.ps1, reducir compresión
'-Z', '6',  # era '-Z', '9'

# O deshabilitar compresión (backup más grande)
'-Fc',  # sin -Z

# Para DBs muy grandes (> 10 GB), usar dump paralelo
'-Fc', '-j', '4',  # 4 cores
```

**Prevención**: Ajustar compresión según tamaño DB y tiempo disponible.

---

### Restauración falla con "duplicate key"

**Síntoma**: `db-restore.ps1` falla con error de clave duplicada.

**Causa**: DB destino ya tiene datos (no se usó `-DropAndCreate`).

**Diagnóstico**:

```powershell
# Conectar a DB destino
docker exec -it edufeed-db-prod psql -U edufeed_prod -d edufeed_prod

# Verificar datos
SELECT COUNT(*) FROM usuarios;
# Si > 0: DB no vacía
```

**Solución**:

```powershell
# Opción 1: Usar -DropAndCreate
pwsh ./scripts/backup/db-restore.ps1 \
  -BackupFile ./backups/daily/2025-10-31/edufeed_prod_20251031_020000.dump \
  -DropAndCreate

# Opción 2: Limpiar manualmente
docker exec edufeed-db-prod psql -U postgres -c "DROP DATABASE edufeed_prod;"
docker exec edufeed-db-prod psql -U postgres -c "CREATE DATABASE edufeed_prod OWNER edufeed_prod;"

# Luego restaurar
pwsh ./scripts/backup/db-restore.ps1 -BackupFile ...
```

**Prevención**: Siempre usar `-DropAndCreate` en restauraciones, excepto append intencional.

---

## Problemas de la aplicación

### Desktop app no se conecta al backend

**Síntoma**: Error "Connection refused" al iniciar sesión.

**Diagnóstico**:

```powershell
# Verificar backend corriendo
curl http://localhost:8080/actuator/health
# Debe retornar {"status":"UP"}

# Verificar firewall
# Windows Firewall → permitir puerto 8080

# Verificar desde red externa
curl http://192.168.1.100:8080/actuator/health
# Cambiar IP por la del servidor
```

**Solución**:

```powershell
# En desktop app, ir a Configuración → Servidor
# URL: http://SERVER_IP:8080
# Probar conexión

# Si es red externa, asegurar puerto abierto
# Router: port forwarding 8080 → IP_SERVIDOR:8080

# Si usa HTTPS, verificar certificado válido
curl -k https://edufeed.com/actuator/health
```

**Prevención**: Documentar configuración de red en manual de instalación.

---

### Error: "JWT token expired"

**Síntoma**: Desktop app cierra sesión automáticamente cada hora.

**Causa**: Access token expira (default: 1 hora).

**Diagnóstico**:

```yaml
# Ver configuración JWT en application.yml
jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000  # 1 hora en ms
```

**Solución**:

```yaml
# Opción 1: Aumentar expiration (no recomendado)
jwt:
  expiration: 28800000  # 8 horas

# Opción 2: Implementar refresh automático (recomendado)
# Desktop app debe detectar 401 y llamar /api/auth/refresh
```

**Prevención**: Implementar refresh token automático en desktop app.

---

### Hardware biométrico no detectado

**Síntoma**: Desktop app no encuentra lector de huella/cámara.

**Diagnóstico**:

**Windows**:
```powershell
# Device Manager → Biometric Devices
# Debe aparecer lector

# Si no aparece, verificar USB
Get-PnpDevice | Where-Object {$_.FriendlyName -like "*finger*"}
```

**Linux**:
```bash
# Verificar dispositivo USB
lsusb | grep -i finger

# Verificar permisos
ls -la /dev/bus/usb/002/003
# Debe ser accesible por usuario actual
```

**Solución**:

**Windows**:
```powershell
# Reinstalar driver del fabricante
# ej. DigitalPersona: descargar de sitio oficial

# Verificar en Device Manager
devmgmt.msc
```

**Linux**:
```bash
# Agregar usuario a grupo plugdev
sudo usermod -aG plugdev $USER

# Cambiar permisos dispositivo
sudo chmod 666 /dev/bus/usb/002/003

# O crear regla udev permanente
sudo nano /etc/udev/rules.d/99-fingerprint.rules
# SUBSYSTEM=="usb", ATTRS{idVendor}=="05ba", MODE="0666"

sudo udevadm control --reload-rules
```

**Prevención**: Documentar instalación de drivers en manual de instalación.

---

## Diagnóstico general

### Checklist de diagnóstico rápido

Cuando algo falla, seguir este orden:

1. **Ver logs**:
   ```powershell
   # Backend
   docker logs edufeed-backend --tail 100 -f
   
   # DB
   docker logs edufeed-db --tail 50
   
   # Todos
   docker compose logs -f
   ```

2. **Verificar servicios corriendo**:
   ```powershell
   docker ps
   # Todos deben estar "Up" y "healthy"
   ```

3. **Verificar conectividad de red**:
   ```powershell
   # Desde backend a DB
   docker exec edufeed-backend ping db
   
   # Desde host a backend
   curl http://localhost:8080/actuator/health
   ```

4. **Verificar variables de entorno**:
   ```powershell
   docker exec edufeed-backend env | grep -E "DB_|PORT|JWT"
   ```

5. **Verificar recursos**:
   ```powershell
   docker stats
   # CPU%, MEM USAGE / LIMIT
   ```

6. **Buscar errores específicos**:
   ```powershell
   docker logs edufeed-backend | Select-String "ERROR"
   docker logs edufeed-backend | Select-String "Exception"
   ```

---

### Comandos útiles de diagnóstico

```powershell
# Ver todos los contenedores (incluidos detenidos)
docker ps -a

# Ver redes
docker network ls
docker network inspect edufeed_default

# Ver volúmenes
docker volume ls
docker volume inspect edufeed_db-data

# Ver uso de disco
docker system df

# Limpiar contenedores detenidos
docker container prune

# Limpiar imágenes sin usar
docker image prune -a

# Limpiar todo (CUIDADO: elimina volúmenes)
docker system prune -a --volumes

# Entrar a contenedor en ejecución
docker exec -it edufeed-backend /bin/sh

# Ver procesos en contenedor
docker top edufeed-backend

# Ver cambios en filesystem del contenedor
docker diff edufeed-backend

# Inspeccionar imagen
docker inspect edufeed-backend:latest
```

---

## Checklist de verificación

### Pre-despliegue

- [ ] Tests unitarios pasan: `mvn test`
- [ ] Tests de integración pasan: `mvn verify`
- [ ] Build exitoso: `mvn clean package`
- [ ] Docker Compose inicia: `docker compose up -d`
- [ ] Backend responde: `curl http://localhost:8080/actuator/health`
- [ ] DB accesible: `docker exec edufeed-db psql -U edufeed -c "SELECT 1;"`
- [ ] Desktop app conecta al backend
- [ ] Migraciones Flyway aplicadas sin errores

### Post-despliegue

- [ ] Todos los contenedores "Up": `docker ps`
- [ ] Backend healthy: `curl /actuator/health`
- [ ] Prometheus scrapea backend: http://localhost:9090/targets
- [ ] Grafana muestra datos: http://localhost:3000
- [ ] Logs aparecen en Kibana
- [ ] Backup automático configurado: `crontab -l` o Task Scheduler
- [ ] Alertas funcionan: forzar alerta y verificar Slack
- [ ] Hardware biométrico detectado en desktop app
- [ ] Login funciona con usuario admin
- [ ] Endpoint críticos funcionan:
  - POST /api/usuarios
  - POST /api/pagos
  - POST /api/accesos/verificar
  - GET /api/reportes/ingresos

### Monitoreo continuo

- [ ] Revisar dashboards Grafana diariamente
- [ ] Verificar backups exitosos en logs
- [ ] Revisar alertas en Slack
- [ ] Verificar logs de errores en Kibana
- [ ] Monitorear uso de disco: `df -h`
- [ ] Monitorear uso de memoria: `docker stats`
- [ ] Revisar auditoría semanal: GET /api/auditoria

---

## Recursos adicionales

### Documentación relacionada

- [Manual de instalación](./manual-instalacion.md)
- [Manual de usuario](./manual-usuario.md)
- [Referencia de API](./api-reference.md)
- [Fase 8.1: Dockerización](./fase8.1-dockerizacion.md)
- [Fase 8.2: CI/CD](./fase8.2-cicd.md)
- [Fase 8.3: Monitoreo](./fase8.3-monitoreo.md)
- [Fase 8.4: Backup/Recuperación](./fase8.4-backup-recuperacion.md)

### Contacto de soporte

- **Email**: soporte@edufeed.com
- **Slack**: #edufeed-soporte
- **Issues**: https://github.com/Joan-Mora/EduFeed/issues

---

**Última actualización**: 31 de octubre de 2025  
**Versión del sistema**: 2.0  
**Mantenido por**: Equipo EduFeed
