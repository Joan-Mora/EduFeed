# Manual de Instalación - Sistema EduFeed

**Versión**: 2.0  
**Fecha**: 31 de octubre de 2025  
**Audiencia**: Administradores de sistemas, DevOps

---

## Índice

1. [Introducción](#introducción)
2. [Requisitos del sistema](#requisitos-del-sistema)
3. [Instalación en entorno de desarrollo](#instalación-en-entorno-de-desarrollo)
4. [Instalación en staging](#instalación-en-staging)
5. [Instalación en producción](#instalación-en-producción)
6. [Configuración de hardware biométrico](#configuración-de-hardware-biométrico)
7. [Configuración de observabilidad](#configuración-de-observabilidad)
8. [Configuración de backups](#configuración-de-backups)
9. [Verificación de instalación](#verificación-de-instalación)
10. [Troubleshooting](#troubleshooting)

---

## Introducción

Este manual cubre la instalación completa del sistema EduFeed en tres entornos:

- **Desarrollo**: Para programadores trabajando en nuevas funcionalidades
- **Staging**: Para pruebas de aceptación de usuario (UAT)
- **Producción**: Entorno real con usuarios finales

### Arquitectura de despliegue

```
┌──────────────────────────────────────────────────────────┐
│                    PRODUCCIÓN                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ PostgreSQL   │  │   Backend    │  │   Desktop    │   │
│  │ (Docker)     │◀─│  (Docker)    │◀─│  (Cliente)   │   │
│  │ Puerto: 5432 │  │ Puerto: 8080 │  │              │   │
│  └──────────────┘  └──────────────┘  └──────────────┘   │
│                                                          │
│  Monitoreo: Prometheus + Grafana + ELK                  │
│  Backups: Automáticos diarios a S3/Azure                │
└──────────────────────────────────────────────────────────┘
```

---

## Requisitos del sistema

### Hardware mínimo

**Servidor (Backend + DB)**:
- CPU: 4 cores (Intel i5/AMD Ryzen 5 o superior)
- RAM: 8 GB (16 GB recomendado)
- Disco: 100 GB SSD (para DB + logs + backups)
- Red: 100 Mbps

**Clientes (Desktop App)**:
- CPU: 2 cores (Intel i3/AMD Ryzen 3)
- RAM: 4 GB
- Disco: 10 GB
- Cámara web (1080p recomendado para reconocimiento facial)
- Lector de huella dactilar USB (opcional)
- Micrófono (para reconocimiento de voz)

### Software requerido

#### Servidor

| Componente | Versión | Propósito |
|------------|---------|-----------|
| **Sistema Operativo** | Ubuntu 22.04 LTS, Windows Server 2022 | Base |
| **Docker** | 24.0+ | Containerización |
| **Docker Compose** | v2.20+ | Orquestación |
| **Git** | 2.40+ | Control de versiones |
| **PowerShell** | 7.3+ (Windows) | Scripts de automatización |
| **Bash** | 5.0+ (Linux) | Scripts |

#### Cliente Desktop

| Componente | Versión | Propósito |
|------------|---------|-----------|
| **Sistema Operativo** | Windows 10/11, Linux (Ubuntu 22.04+) | Base |
| **Java Runtime (JRE)** | OpenJDK 24 (Temurin) | Ejecución de JavaFX |
| **Drivers biométricos** | Según fabricante | Hardware biométrico |

#### Desarrollo (adicional)

| Componente | Versión | Propósito |
|------------|---------|-----------|
| **JDK** | OpenJDK 24 (Temurin) | Compilación |
| **Maven** | 3.9.9 | Build tool |
| **VS Code** | 1.85+ | IDE |
| **pgAdmin** | 8.12 (opcional) | Gestión de BD |

---

## Instalación en entorno de desarrollo

### 1. Clonar repositorio

```powershell
# Windows PowerShell
cd C:\proyectos
git clone https://github.com/Joan-Mora/EduFeed.git
cd EduFeed
```

```bash
# Linux/Mac
cd ~/proyectos
git clone https://github.com/Joan-Mora/EduFeed.git
cd EduFeed
```

### 2. Instalar dependencias

**Windows**:

```powershell
# Instalar Chocolatey (si no está instalado)
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Instalar dependencias
choco install openjdk --version=24.0.0
choco install maven --version=3.9.9
choco install docker-desktop
choco install vscode
choco install git
```

**Linux (Ubuntu)**:

```bash
# JDK 24
wget https://download.java.net/java/GA/jdk24/latest/GPL/openjdk-24_linux-x64_bin.tar.gz
sudo tar -xzf openjdk-24_linux-x64_bin.tar.gz -C /opt/
sudo update-alternatives --install /usr/bin/java java /opt/jdk-24/bin/java 1

# Maven
wget https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
sudo tar -xzf apache-maven-3.9.9-bin.tar.gz -C /opt/
echo 'export PATH=/opt/apache-maven-3.9.9/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

### 3. Configurar entorno

**Crear archivo `.env` (desarrollo)**:

```powershell
# Windows
Copy-Item .env.dev .env

# Linux
cp .env.dev .env
```

Contenido de `.env` (ajustar si es necesario):

```bash
# Database
POSTGRES_DB=edufeed
POSTGRES_USER=edufeed
POSTGRES_PASSWORD=edufeed
POSTGRES_PORT=5432

# pgAdmin
PGADMIN_PORT=5050
PGADMIN_DEFAULT_EMAIL=admin@edufeed.local
PGADMIN_DEFAULT_PASSWORD=admin123

# Backend
PORT=8080
DB_URL=jdbc:postgresql://db:5432/edufeed
DB_USER=edufeed
DB_PASSWORD=edufeed
SPRING_PROFILES_ACTIVE=dev

# Security (dev - NO usar en producción)
JWT_SECRET=dev-secret-key-not-for-production
BIOMETRIC_ENCRYPTION_KEY=dev-biometric-key-32-chars-min

# App
APP_TIMEZONE=America/Bogota
SEED_OPERATOR_ENABLED=true
```

### 4. Levantar base de datos

**Usando VS Code tasks** (recomendado):

1. Abrir carpeta en VS Code: `code .`
2. `Ctrl+Shift+P` → `Tasks: Run Task` → `DB: up`

**Usando script PowerShell**:

```powershell
.\scripts\db-up.ps1
```

**Usando Docker Compose directo**:

```bash
docker compose up -d db pgadmin
```

Verificar:
```powershell
docker ps
# Debe mostrar: edufeed-db (healthy), edufeed-pgadmin
```

### 5. Compilar proyecto

```powershell
# Windows
$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
mvn clean install -DskipTests

# Linux
export JAVA_HOME=/opt/jdk-24
mvn clean install -DskipTests
```

### 6. Ejecutar backend

**Opción A: VS Code task**:

`Ctrl+Shift+P` → `Tasks: Run Task` → `Backend: run`

**Opción B: Maven directo**:

```powershell
mvn -f edufeed-backend/pom.xml spring-boot:run
```

Verificar en http://localhost:8080/actuator/health

### 7. Ejecutar desktop app

**VS Code task**:

`Tasks: Run Task` → `Desktop: run`

**Maven directo**:

```powershell
mvn -f edufeed-desktop/pom.xml -DskipTests javafx:run
```

### 8. Datos iniciales (seed)

El seed se ejecuta automáticamente al levantar el backend por primera vez si `SEED_OPERATOR_ENABLED=true`.

**Usuarios creados**:
- **Usuario**: `admin` | **Contraseña**: `Admin123!` (Administrador)
- **Usuario**: `cajero01` | **Contraseña**: `Cajero123!` (Cajero)
- **Usuario**: `operador01` | **Contraseña**: `Operador123!` (Operador de Acceso)

Datos de prueba: 50 usuarios con biometría simulada, 30 pagos.

---

## Instalación en staging

Staging replica producción para pruebas UAT.

### 1. Provisionar servidor

**Opción A: Cloud (AWS/Azure)**:

```bash
# Ejemplo AWS EC2
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \  # Ubuntu 22.04
  --instance-type t3.medium \
  --key-name edufeed-staging-key \
  --security-group-ids sg-0123456789abcdef0 \
  --subnet-id subnet-0123456789abcdef0 \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=edufeed-staging}]'
```

**Opción B: On-premise**:

Instalar Ubuntu Server 22.04 LTS en máquina virtual o física.

### 2. Configurar servidor

```bash
# SSH al servidor
ssh ubuntu@staging.edufeed.com

# Actualizar sistema
sudo apt update && sudo apt upgrade -y

# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu

# Instalar Docker Compose v2
sudo apt install docker-compose-plugin

# Instalar Git
sudo apt install git -y

# Clonar repositorio
cd /opt
sudo git clone https://github.com/Joan-Mora/EduFeed.git edufeed-staging
sudo chown -R ubuntu:ubuntu edufeed-staging
cd edufeed-staging
```

### 3. Configurar variables de entorno

```bash
# Copiar plantilla
cp .env.stage .env.prod

# Editar con valores reales
nano .env.prod
```

**Variables críticas a cambiar**:
- `POSTGRES_PASSWORD`: Contraseña fuerte (min 16 caracteres)
- `PGADMIN_DEFAULT_PASSWORD`: Contraseña de acceso a pgAdmin
- `JWT_SECRET`: Generar con `openssl rand -base64 32`
- `BIOMETRIC_ENCRYPTION_KEY`: Generar con `openssl rand -base64 32`

### 4. Desplegar con Docker Compose

```bash
# Build y levantar servicios
docker compose --env-file .env.prod -f docker-compose.prod.yml build
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d

# Verificar
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f backend
```

### 5. Configurar firewall

```bash
# Ubuntu UFW
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 8081/tcp    # Backend staging
sudo ufw allow 5051/tcp    # pgAdmin staging
sudo ufw enable
```

### 6. Validar instalación

```bash
# Health check
curl http://localhost:8081/actuator/health

# Desde máquina externa
curl http://staging.edufeed.com:8081/actuator/health
```

---

## Instalación en producción

### 1. Provisionar servidor

**Recomendaciones de producción**:
- **Cloud**: AWS EC2 t3.large (2vCPU, 8GB RAM) o Azure Standard_B2s
- **On-premise**: Servidor dedicado con SSD RAID 1
- **Alta disponibilidad**: Considerar cluster PostgreSQL (Patroni) + load balancer (nginx)

### 2. Hardening del servidor

```bash
# Deshabilitar root login
sudo nano /etc/ssh/sshd_config
# PermitRootLogin no
sudo systemctl restart sshd

# Fail2ban para bloquear ataques SSH
sudo apt install fail2ban -y

# Actualizaciones automáticas de seguridad
sudo apt install unattended-upgrades -y
sudo dpkg-reconfigure --priority=low unattended-upgrades
```

### 3. Configuración de producción

**`.env.prod`** (NUNCA versionar):

```bash
# Database
POSTGRES_DB=edufeed_prod
POSTGRES_USER=edufeed_prod
POSTGRES_PASSWORD=<GENERAR_FUERTE>
POSTGRES_PORT=5432

# pgAdmin
PGADMIN_PORT=5052
PGADMIN_DEFAULT_EMAIL=admin@edufeed.com
PGADMIN_DEFAULT_PASSWORD=<GENERAR_FUERTE>

# Backend
PORT=8080
DB_URL=jdbc:postgresql://db:5432/edufeed_prod
DB_USER=edufeed_prod
DB_PASSWORD=<MISMO_QUE_POSTGRES_PASSWORD>
SPRING_PROFILES_ACTIVE=prod

# Security (GENERAR CON openssl rand -base64 32)
JWT_SECRET=<GENERAR_64_CHARS>
BIOMETRIC_ENCRYPTION_KEY=<GENERAR_64_CHARS>

# App
APP_TIMEZONE=America/Bogota
SEED_OPERATOR_ENABLED=false  # NO seed en producción

# Flyway
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
SPRING_FLYWAY_BASELINE_VERSION=0
```

### 4. Deploy inicial

```bash
cd /opt/edufeed
git pull origin main

# Build módulos comunes primero (en máquina con Maven/JDK)
# O usar imagen pre-built de GitHub Container Registry

docker login ghcr.io -u GITHUB_USER
docker pull ghcr.io/joan-mora/edufeed-backend:latest

# O build local
docker compose --env-file .env.prod -f docker-compose.prod.yml build backend

# Levantar
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d

# Logs
docker compose logs -f backend
```

### 5. Configurar reverse proxy (HTTPS)

**Instalar nginx**:

```bash
sudo apt install nginx certbot python3-certbot-nginx -y
```

**Configurar virtual host** (`/etc/nginx/sites-available/edufeed`):

```nginx
server {
    listen 80;
    server_name edufeed.com www.edufeed.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket support (si aplica)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Timeouts
    proxy_connect_timeout 60s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;

    # Max body size (para uploads biométricos)
    client_max_body_size 50M;
}
```

**Habilitar y obtener certificado SSL**:

```bash
sudo ln -s /etc/nginx/sites-available/edufeed /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx

# Certbot (Let's Encrypt)
sudo certbot --nginx -d edufeed.com -d www.edufeed.com
```

### 6. Crear usuario admin inicial

Como `SEED_OPERATOR_ENABLED=false` en producción:

```bash
# Conectar a DB
docker exec -it edufeed-db-prod psql -U edufeed_prod -d edufeed_prod

# Crear operador admin (contraseña hasheada con BCrypt)
INSERT INTO operators (username, password, role, full_name, email, created_at)
VALUES (
  'admin',
  '$2a$10$XYZ...',  -- Generar con https://bcrypt-generator.com/
  'ADMIN',
  'Administrador Principal',
  'admin@edufeed.com',
  NOW()
);
```

**O usar endpoint de bootstrap** (si implementado):

```bash
curl -X POST http://localhost:8080/api/v1/setup/bootstrap \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "TuPasswordSegura123!",
    "email": "admin@edufeed.com"
  }'
```

---

## Configuración de hardware biométrico

### Lector de huella dactilar

**Modelos compatibles**:
- DigitalPersona U.are.U 4500
- ZKTeco ZK9500
- Futronic FS88

**Instalación** (Windows):

1. Conectar lector USB
2. Descargar driver del fabricante
3. Instalar driver (ej. `DigitalPersona_4.5.0.exe`)
4. Verificar en Device Manager → Biometric Devices

**Instalación** (Linux):

```bash
# libfprint (genérico para varios lectores)
sudo apt install libfprint-2-2 fprintd -y

# Verificar detección
lsusb | grep -i fingerprint
```

### Cámara web (reconocimiento facial)

**Recomendaciones**:
- Resolución: 1080p (1920×1080) mínimo
- FPS: 30fps
- Autofocus
- Micrófono integrado (para reconocimiento de voz)

**Modelos recomendados**:
- Logitech C920 HD Pro
- Microsoft LifeCam HD-3000

**Configuración**:

1. Conectar cámara USB
2. Verificar en desktop app:
   - Ir a **Configuración → Dispositivos → Cámara**
   - Seleccionar cámara de la lista
   - Hacer clic en **"Probar"** para ver vista previa

### Micrófono (reconocimiento de voz)

**Recomendaciones**:
- Micrófono de diadema o condensador USB
- Cancelación de ruido

**Configuración**:

Windows:
1. `Panel de Control → Sonido → Grabación`
2. Seleccionar micrófono → Propiedades
3. Pestaña "Niveles": Ajustar a 80-90%
4. Pestaña "Avanzadas": Formato → 44100 Hz, 16 bits

---

## Configuración de observabilidad

Ver documentación completa en `docs/fase8.3-monitoreo.md`.

### Despliegue rápido

```bash
cd /opt/edufeed

# Levantar stack de observabilidad
docker compose --env-file .env.prod \
  -f docker-compose.prod.yml \
  -f docker-compose.observability.yml up -d

# Verificar
docker ps | grep -E 'prometheus|grafana|elasticsearch'
```

### Accesos:
- Prometheus: http://server:9090
- Grafana: http://server:3000 (admin/admin)
- Kibana: http://server:5601

### Configurar alertas Slack

1. Crear Webhook en Slack: https://api.slack.com/apps
2. Agregar a `.env.prod`:
   ```bash
   SLACK_WEBHOOK_URL=https://hooks.slack.com/services/T.../B.../XXX
   ```
3. Reiniciar Grafana:
   ```bash
   docker compose restart grafana
   ```

---

## Configuración de backups

Ver documentación completa en `docs/fase8.4-backup-recuperacion.md`.

### Configurar backup diario automático

**Linux (cron)**:

```bash
# Editar crontab
crontab -e

# Agregar línea (diario a las 2 AM)
0 2 * * * /usr/bin/pwsh -File /opt/edufeed/scripts/backup/db-backup.ps1 -EnvFile /opt/edufeed/.env.prod -ContainerName edufeed-db-prod -BackupRoot /opt/edufeed/backups -UploadS3 -S3Bucket edufeed-backups-prod >> /var/log/edufeed-backup.log 2>&1
```

**Windows (Task Scheduler)**:

1. Abrir `taskschd.msc`
2. Create Task → "EduFeed Backup Diario"
3. Trigger: Daily 2:00 AM
4. Action: Start program
   - Program: `pwsh`
   - Arguments:
     ```
     -File "C:\opt\edufeed\scripts\backup\db-backup.ps1" -EnvFile "C:\opt\edufeed\.env.prod" -ContainerName edufeed-db-prod -BackupRoot "C:\opt\edufeed\backups"
     ```

### Configurar subida a S3

```bash
# Instalar AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Configurar credenciales
aws configure
# AWS Access Key ID: AKIA...
# AWS Secret Access Key: ...
# Default region: us-east-1
```

---

## Verificación de instalación

### Checklist post-instalación

- [ ] Docker contenedores corriendo: `docker ps`
- [ ] Backend responde: `curl http://localhost:8080/actuator/health`
- [ ] DB accesible: `docker exec edufeed-db-prod pg_isready -U edufeed_prod`
- [ ] pgAdmin accesible: http://localhost:5052
- [ ] Desktop app inicia sin errores
- [ ] Login con admin funciona
- [ ] Prometheus scrapeando: http://localhost:9090/targets
- [ ] Grafana dashboards muestran datos: http://localhost:3000
- [ ] Backup manual exitoso: `pwsh scripts/backup/db-backup.ps1 ...`
- [ ] Firewall configurado correctamente
- [ ] HTTPS funcionando (si aplica)
- [ ] Hardware biométrico detectado

### Tests de humo

```bash
# Health check
curl http://localhost:8080/actuator/health
# Esperado: {"status":"UP"}

# Metrics
curl http://localhost:8080/actuator/prometheus | grep http_server_requests
# Esperado: líneas con métricas

# Login API
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123!"}'
# Esperado: {"token":"eyJhbGc..."}

# Database connectivity
docker exec edufeed-db-prod psql -U edufeed_prod -d edufeed_prod -c "SELECT COUNT(*) FROM operators;"
# Esperado: count >= 1
```

---

## Troubleshooting

### Backend no inicia

**Síntoma**: `docker logs edufeed-backend-prod` muestra error de conexión a DB.

**Solución**:

```bash
# Verificar DB está healthy
docker ps
# STATUS debe ser "healthy" para edufeed-db-prod

# Si no está healthy, ver logs de DB
docker logs edufeed-db-prod

# Verificar variables de entorno
docker exec edufeed-backend-prod env | grep DB_
# DB_URL=jdbc:postgresql://db:5432/edufeed_prod
# DB_USER=edufeed_prod
# DB_PASSWORD=...

# Verificar conectividad desde backend a DB
docker exec edufeed-backend-prod ping db
```

### Flyway migration falla

**Síntoma**: `Flyway baseline required`.

**Solución**:

```bash
# En .env.prod, asegurar:
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
SPRING_FLYWAY_BASELINE_VERSION=0

# Recrear backend
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --force-recreate backend
```

### Desktop app no se conecta al backend

**Síntoma**: Error "Connection refused" al iniciar sesión.

**Solución**:

1. Verificar backend corriendo: `curl http://server:8080/actuator/health`
2. Verificar firewall permite puerto 8080
3. En desktop app, ir a **Configuración → Servidor**:
   - URL: `http://server_ip:8080`
   - Probar conexión

### Hardware biométrico no detectado

**Windows**:
1. Device Manager → Biometric Devices → debe aparecer lector
2. Si no, reinstalar driver del fabricante

**Linux**:
```bash
lsusb
# Debe mostrar dispositivo
# Ej: Bus 002 Device 003: ID 05ba:000a DigitalPersona, Inc. Fingerprint Reader

# Permisos
sudo usermod -aG plugdev $USER
sudo chmod 666 /dev/bus/usb/002/003
```

### Prometheus no scrapea backend

**Síntoma**: Target "edufeed-backend" DOWN en http://localhost:9090/targets.

**Solución**:

```bash
# Verificar backend expone /actuator/prometheus
curl http://backend:8080/actuator/prometheus

# Verificar Prometheus puede alcanzar backend
docker exec prometheus ping backend

# Ver logs Prometheus
docker logs prometheus | grep backend
```

---

## Anexos

### A. Puertos utilizados

| Servicio | Puerto | Acceso |
|----------|--------|--------|
| Backend (dev) | 8080 | localhost |
| Backend (staging) | 8081 | staging.edufeed.com |
| Backend (prod) | 8080 | edufeed.com (vía nginx 443) |
| PostgreSQL (dev) | 5432 | localhost |
| PostgreSQL (staging) | 5433 | staging.edufeed.com |
| PostgreSQL (prod) | 5432 | localhost (no expuesto) |
| pgAdmin (dev) | 5050 | localhost |
| pgAdmin (staging) | 5051 | staging.edufeed.com |
| pgAdmin (prod) | 5052 | edufeed.com/pgadmin |
| Prometheus | 9090 | localhost |
| Grafana | 3000 | localhost |
| Kibana | 5601 | localhost |

### B. Comandos útiles

```bash
# Ver todos los contenedores
docker ps -a

# Ver logs de un servicio
docker compose logs -f backend

# Reiniciar servicio
docker compose restart backend

# Entrar a contenedor
docker exec -it edufeed-backend-prod /bin/sh

# Ver uso de recursos
docker stats

# Limpiar imágenes huérfanas
docker image prune -a

# Backup manual de DB
pwsh scripts/backup/db-backup.ps1 -ContainerName edufeed-db-prod

# Restaurar backup
pwsh scripts/backup/db-restore.ps1 -BackupFile backups/daily/2025-10-31/edufeed_prod_20251031_020000.dump -DropAndCreate
```

---

**Última actualización**: 31 de octubre de 2025  
**Versión del sistema**: 2.0  
**Manual elaborado por**: Equipo EduFeed

Para soporte: instalacion@edufeed.com
