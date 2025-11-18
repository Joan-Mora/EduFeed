<div align="center">

# 🍽️ EduFeed

### Sistema Integral de Gestión para Restaurante Escolar

[![Java](https://img.shields.io/badge/Java-24-orange?style=flat&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-22-red?style=flat&logo=java)](https://openjfx.io/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=flat&logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat)](./LICENSE)

**Sistema biométrico de control de acceso con gestión de pagos y reportes administrativos para restaurantes escolares**

[Características](#-características-principales) •
[Tecnologías](#-stack-tecnológico) •
[Instalación](#-instalación) •
[Documentación](#-documentación) •
[Autores](#-autores)

</div>

---

## 📋 Descripción

**EduFeed** es una solución integral diseñada para la **gestión automatizada de restaurantes escolares** que atienden aproximadamente **500 usuarios diarios**. El sistema combina validación biométrica multimodal (huella dactilar, reconocimiento facial y voz) con un robusto sistema de gestión de pagos, control de acceso y generación de reportes administrativos.

### Problemática que resuelve

- ✅ **Control de acceso automatizado** basado en derechos adquiridos por pago
- ✅ **Eliminación de fraude** mediante validación biométrica multimodal
- ✅ **Trazabilidad completa** de asistencias, inasistencias y transacciones
- ✅ **Optimización operativa** con reportes en tiempo real
- ✅ **Integración con sistemas de caja** para sincronización automática de pagos
- ✅ **Seguridad de datos** con cifrado AES-256-GCM para información sensible  

---

## 🎯 Características Principales

### 🔐 Autenticación Biométrica Multimodal

- **Huella dactilar**: Compatibilidad con lectores Digital Persona, ZKTeco y Suprema
- **Reconocimiento facial**: Detección con OpenCV + embeddings FaceNet
- **Verificación de voz**: Captura y análisis de características vocales (MFCC)
- **WebAuthn/FIDO2**: Autenticación biométrica vía teléfono móvil (QR + PWA)

### 💳 Gestión de Pagos y Derechos

- **Tipos de pago soportados**:
  - 📅 **Diario**: Válido solo para el día actual
  - 📆 **Mensual**: Acceso ilimitado durante 30 días
  - 📦 **Paquete**: Días prepagados consumibles (ej: 10 días)
- **Conciliación automática** con sistemas de caja (webhooks)
- **Generación automática** de derechos de uso post-aprobación
- **Control de vigencias** y vencimientos

### 📊 Control de Acceso

- Verificación en tiempo real de derechos adquiridos
- Registro detallado de cada intento (fecha, hora, estado, motivo)
- Orientación automática a caja en caso de rechazo
- Historial completo de accesos por usuario

### 📈 Reportes Administrativos

- Asistencias y ausencias por tipo de pago
- Ingresos diarios, semanales y mensuales
- Usuarios con derecho activo vs inactivo
- Rechazos de acceso con análisis de causas
- Exportación a Excel/PDF

### 🔒 Seguridad y Auditoría

- Cifrado AES-256-GCM para plantillas biométricas
- Hashing seguro de contraseñas (BCrypt)
- Autenticación JWT con refresh tokens
- Auditoría completa de operaciones críticas
- Manejo de zona horaria (America/Bogota)

---

## 🛠️ Stack Tecnológico

### Backend (API REST)

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Java** | 24 | Lenguaje principal |
| **Spring Boot** | 3.3.13 | Framework backend |
| **Spring Data JPA** | - | Persistencia ORM |
| **Hibernate** | - | Implementación JPA |
| **Flyway** | - | Migraciones de BD |
| **PostgreSQL** | 16 | Base de datos relacional |
| **JWT** | - | Autenticación stateless |
| **Lombok** | - | Reducción de boilerplate |
| **MapStruct** | 1.6.3 | Mapeo DTO ↔ Entidad |

### Frontend Desktop

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **JavaFX** | 22 | Interfaz gráfica |
| **FXML** | - | Diseño de vistas |
| **ControlsFX** | - | Componentes UI avanzados |

### Biometría

| Tecnología | Propósito |
|------------|-----------|
| **OpenCV** | Detección facial |
| **FaceNet** | Embeddings faciales |
| **Digital Persona SDK** | Lector de huellas |
| **ZKTeco SDK** | Lector de huellas |
| **Suprema SDK** | Lector de huellas |
| **javax.sound** | Captura de audio (voz) |

### DevOps y Observabilidad

| Herramienta | Propósito |
|-------------|-----------|
| **Docker** | Contenerización |
| **Docker Compose** | Orquestación local |
| **GitHub Actions** | CI/CD |
| **Prometheus** | Métricas |
| **Grafana** | Dashboards |
| **ELK Stack** | Logs centralizados |

---

## 📁 Arquitectura del Proyecto

EduFeed utiliza una **arquitectura multi-módulo Maven** para separación de responsabilidades:

```
EduFeed/
├── edufeed-backend/          # API REST + lógica de negocio
│   ├── controller/            # Endpoints REST
│   ├── service/               # Lógica de negocio
│   ├── repository/            # Acceso a datos JPA
│   ├── model/                 # Entidades JPA
│   ├── dto/                   # DTOs (request/response)
│   ├── security/              # JWT, autenticación
│   └── config/                # Configuraciones Spring
│
├── edufeed-desktop/           # Aplicación de escritorio JavaFX
│   ├── controller/            # Controladores FXML
│   ├── view/                  # Archivos FXML
│   └── service/               # Servicios de UI
│
├── edufeed-biometric/         # Módulo de biometría
│   ├── fingerprint/           # Proveedores de huella
│   ├── face/                  # Reconocimiento facial
│   └── voice/                 # Verificación de voz
│
├── edufeed-common/            # Código compartido
│   ├── util/                  # Utilidades generales
│   └── exception/             # Excepciones comunes
│
├── docs/                      # Documentación
│   ├── architecture.md        # Documento de arquitectura
│   ├── manual-usuario.md      # Manual de usuario
│   └── manual-instalacion.md # Manual de instalación
│
├── scripts/                   # Scripts de utilidad
│   ├── db-up.ps1             # Levantar base de datos
│   ├── db-down.ps1           # Detener base de datos
│   └── backup/               # Scripts de respaldo
│
├── docker-compose.yml         # Desarrollo local
├── docker-compose.prod.yml    # Producción
└── pom.xml                    # POM padre (multi-módulo)
```

---

## 🚀 Instalación

### Requisitos Previos

- **Java JDK 24** ([OpenJDK](https://openjdk.org/) o [Temurin](https://adoptium.net/))
- **Maven 3.9+** ([Descarga](https://maven.apache.org/download.cgi))
- **PostgreSQL 16** (o Docker para desarrollo)
- **Docker Desktop** (opcional, recomendado para desarrollo)
- **Git** ([Descarga](https://git-scm.com/downloads))

### 1. Clonar el Repositorio

```bash
git clone https://github.com/Joan-Mora/EduFeed.git
cd EduFeed
```

### 2. Configurar Base de Datos

#### Opción A: Usando Docker (Recomendado)

```powershell
# Windows PowerShell
pwsh -NoProfile -ExecutionPolicy Bypass -File ./scripts/db-up.ps1

# Linux/Mac
./scripts/setup-dev.sh
```

#### Opción B: PostgreSQL Local

1. Crear base de datos:
```sql
CREATE DATABASE edufeed;
CREATE USER edufeed_user WITH PASSWORD 'edufeed_pass';
GRANT ALL PRIVILEGES ON DATABASE edufeed TO edufeed_user;
```

2. Configurar `edufeed-backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/edufeed
    username: edufeed_user
    password: edufeed_pass
```

### 3. Compilar el Proyecto

```bash
mvn clean install
```

### 4. Ejecutar Backend

```bash
cd edufeed-backend
mvn spring-boot:run
```

La API estará disponible en: `http://localhost:8080`

Documentación Swagger: `http://localhost:8080/swagger-ui/index.html`

### 5. Ejecutar Desktop (opcional)

```bash
cd edufeed-desktop
mvn javafx:run
```

---

## 🐳 Despliegue con Docker

### Desarrollo

```bash
docker compose up -d
```

### Producción

1. Crear archivo `.env.prod`:
```bash
cp .env.prod.example .env.prod
# Editar .env.prod con valores reales
```

2. Construir y desplegar:
```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

### Observabilidad

```bash
docker compose --env-file .env.prod \
  -f docker-compose.prod.yml \
  -f docker-compose.observability.yml up -d
```

**Servicios disponibles:**
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/admin)
- Kibana: `http://localhost:5601`
- pgAdmin: `http://localhost:5050`

---

## 📚 Documentación

| Documento | Descripción |
|-----------|-------------|
| [Manual de Usuario](./docs/manual-usuario.md) | Guía completa para usuarios finales |
| [Manual de Instalación](./docs/manual-instalacion.md) | Instalación detallada paso a paso |
| [Arquitectura](./docs/architecture.md) | Diseño técnico y decisiones de arquitectura |
| [Backup y Restauración](./docs/backup-restore.md) | Estrategias de respaldo |
| [REPO_STRUCTURE.md](./docs/REPO_STRUCTURE.md) | Estructura del repositorio |

---

## 🧪 Testing

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Solo backend
mvn test -pl edufeed-backend

# Con cobertura
mvn verify
```

---

## 🔒 Seguridad

- **Plantillas biométricas**: Cifradas con AES-256-GCM antes de persistir
- **Contraseñas**: Hashing con BCrypt (factor de trabajo: 12)
- **JWT**: Tokens firmados con algoritmo HS512
- **HTTPS**: Obligatorio en producción
- **Variables sensibles**: Nunca incluidas en el código fuente

**Reporte de vulnerabilidades**: Ver [SECURITY.md](./SECURITY.md)

---

## 📊 Casos de Uso Principales

### 1. Registro de Usuario
```
Actor: Operador de Caja
1. Captura datos personales
2. Enrolla huella/rostro/voz
3. Sistema cifra y almacena plantilla biométrica
4. Genera credencial de usuario
```

### 2. Control de Acceso
```
Actor: Usuario
1. Coloca huella en lector / escanea rostro
2. Sistema verifica identidad (threshold > 0.95)
3. Valida derecho de uso vigente
4. Registra acceso (fecha, hora, modalidad)
5. Permite ingreso o redirige a caja
```

### 3. Gestión de Pago
```
Actor: Cajero
1. Selecciona usuario y tipo de pago
2. Registra transacción
3. Sistema calcula vigencias automáticamente
4. Genera derecho de uso activo
5. Usuario puede acceder inmediatamente
```

---

## 🤝 Contribución

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit tus cambios (`git commit -m 'feat: agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Abre un Pull Request

### Convenciones de Commits

Seguimos [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: nueva característica
fix: corrección de bug
docs: cambios en documentación
style: formato, punto y coma faltante, etc
refactor: refactorización de código
test: agregar tests
chore: tareas de mantenimiento
```

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver [LICENSE](./LICENSE) para más detalles.

---

## 👥 Autores

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/Joan-Mora.png" width="100px;" alt="Darwin Joan Aveiga Mora"/><br />
      <sub><b>Darwin Joan Aveiga Mora</b></sub><br />
      <a href="https://github.com/Joan-Mora">@Joan-Mora</a><br />
      <sub>Desarrollador Full Stack</sub>
    </td>
    <td align="center">
      <sub><b>Julian Esteban Chavez Zamora</b></sub><br />
      <sub>Desarrollador Full Stack</sub>
    </td>
  </tr>
</table>

**Institución:** Corporación Universitaria Minuto de Dios  
**Programa:** Tecnología en Desarrollo de Software  
**Semestre:** II - 2025  
**Proyecto:** Reto de Transformación Digital - Restaurante Escolar

---

## 🙏 Agradecimientos

- **UNIMINUTO** por el apoyo académico y el reto propuesto
- **Spring Framework Team** por el excelente framework
- **OpenCV Community** por las herramientas de visión artificial
- Comunidad de **Stack Overflow** y **GitHub** por el soporte técnico

---

## 📞 Contacto y Soporte

- **Issues**: [GitHub Issues](https://github.com/Joan-Mora/EduFeed/issues)
- **Documentación**: [Wiki del Proyecto](https://github.com/Joan-Mora/EduFeed/wiki)
- **Email**: soporte@edufeed.co

---

<div align="center">

**⭐ Si este proyecto te resulta útil, considera darle una estrella ⭐**

Desarrollado con ❤️ por el equipo EduFeed

</div>

---

## 🔧 Configuración Avanzada

### Variables de Entorno

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | URL de PostgreSQL | `jdbc:postgresql://localhost:5432/edufeed` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de BD | `edufeed_user` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de BD | `edufeed_pass` |
| `JWT_SECRET` | Clave secreta JWT | (generado aleatorio) |
| `BIOMETRIC_ENCRYPTION_KEY` | Clave AES-256 | (generado aleatorio) |
| `EDUFEED_BIOMETRIC_PROVIDER` | Proveedor biométrico | `mock` / `hardware` |

---

## 🐳 Docker y CI/CD

### 8.1 Dockerización

- Dockerfile backend: `edufeed-backend/Dockerfile` (multi-stage build Temurin 24)
- Compose prod: `docker-compose.prod.yml` (servicios: Postgres 16, pgAdmin, backend)
- Entornos: `.env.dev`, `.env.stage`, `.env.prod.example` (copiar a `.env.prod` y NO subir)

Ejemplos (Linux/Mac/WSL):

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml build backend
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

En `.env.prod`, define al menos: `POSTGRES_PASSWORD`, `PGADMIN_DEFAULT_PASSWORD`, `JWT_SECRET`, `BIOMETRIC_ENCRYPTION_KEY`.

### 8.2 CI/CD (GitHub Actions)

Workflow: `.github/workflows/ci-cd.yml`

- Eventos: `push` y `pull_request` a `main`.
- Fases: build+test (Maven) → build+push imagen (GHCR) → deploy a `staging` → deploy a `production` (con aprobación vía Environments).

Registra estos Secrets para despliegue remoto opcional por SSH (si no existen, el job de deploy se omite):

- Staging: `STAGING_HOST`, `STAGING_USER`, `STAGING_SSH_KEY` (clave privada), `STAGING_PATH` (ej. `/opt/edufeed-staging`).
- Producción: `PROD_HOST`, `PROD_USER`, `PROD_SSH_KEY`, `PROD_PATH` (ej. `/opt/edufeed`).

Requisitos en el servidor remoto: Docker y Docker Compose v2, archivo `.env.prod` preprovisionado en `STAGING_PATH/PROD_PATH`.

Para exigir aprobación manual en producción: configura el Environment `production` en Settings → Environments con `Required reviewers`.

### 8.3 Observabilidad (Prometheus + Grafana + ELK)

- Archivos de configuración en `observability/` (Prometheus, Alertmanager, Grafana, Logstash, Filebeat).
- Levanta el stack junto al compose de prod (comparten red por defecto al usar ambos `-f`):

```bash
docker compose --env-file .env.prod \
	-f docker-compose.prod.yml \
	-f docker-compose.observability.yml up -d
```

Servicios:
- Prometheus: http://localhost:9090 (scrapea `backend:8080/actuator/prometheus`)
- Grafana: http://localhost:3000 (admin/admin por defecto; datasource y dashboard provisionados)
- Elasticsearch: http://localhost:9200
- Logstash (beats): :5044
- Kibana: http://localhost:5601
- Filebeat: colecta logs de Docker y los envía a Logstash

Alertas:
- Edita `observability/alertmanager/alertmanager.yml` y configura Slack o Email.
- Regla p95 y 500s en `observability/prometheus/rules.yml`.

### 8.4 Backups y restauración

Documentación completa y scripts en `docs/backup-restore.md` y `scripts/backup/`:

- `db-backup.ps1`: genera backups lógicos (`pg_dump -Fc -Z 9`) con retención diaria/semanal/mensual y subida opcional a S3/Azure.
- `db-restore.ps1`: restaura `.dump` (pg_restore) o `.sql` (psql) dentro del contenedor.

Ejemplos rápidos (PowerShell en Windows):

```powershell
pwsh ./scripts/backup/db-backup.ps1 -EnvFile .env -ContainerName edufeed-db -BackupRoot ./backups
pwsh ./scripts/backup/db-restore.ps1 -EnvFile .env -ContainerName edufeed-db -BackupFile ./backups/daily/2025-01-20/edufeed_20250120_010203.dump -DropAndCreate
```

