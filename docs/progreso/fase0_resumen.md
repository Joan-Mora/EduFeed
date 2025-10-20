# FASE 0: Configuración de Entorno y Planificación

**Periodo:** Semana 1  
**Estado:** ✅ COMPLETADO  
**Fecha de finalización:** 20 de octubre de 2025

---

## 📋 Índice

1. [Objetivos](#objetivos)
2. [Entregables](#entregables)
3. [Infraestructura y Herramientas](#infraestructura-y-herramientas)
4. [Base de Datos](#base-de-datos)
5. [Documentación Técnica](#documentación-técnica)
6. [Automatización y Scripts](#automatización-y-scripts)
7. [Siguiente Fase](#siguiente-fase)

---

## 🎯 Objetivos

### Completados:
- ✅ Configurar entorno de desarrollo completo (JDK 24, Maven 3.9.9, Docker Desktop)
- ✅ Establecer infraestructura de base de datos con PostgreSQL 16.4 y pgAdmin
- ✅ Crear estructura multi-módulo Maven para el proyecto
- ✅ Definir arquitectura técnica y decisiones de diseño
- ✅ Documentar requisitos funcionales y no funcionales
- ✅ Preparar scripts de automatización para desarrollo local
- ✅ Configurar Visual Studio Code con Live Share para trabajo colaborativo

---

## 📦 Entregables

### **1. Estructura Multi-Módulo Maven**

#### Módulos creados:

```
EduFeed/
├── pom.xml                            [Proyecto padre - Spring Boot 3.4.10]
├── edufeed-backend/                   [API REST + Spring Boot]
│   ├── pom.xml
│   └── src/main/java/co/cellano/edufeed/backend/
├── edufeed-desktop/                   [Cliente JavaFX 22]
│   ├── pom.xml
│   └── src/main/java/co/cellano/edufeed/desktop/
├── edufeed-biometric/                 [Librería de biometría]
│   ├── pom.xml
│   └── src/main/java/co/cellano/edufeed/biometric/
└── edufeed-common/                    [DTOs y utilidades compartidas]
    ├── pom.xml
    └── src/main/java/co/cellano/edufeed/common/
```

**Tecnologías configuradas:**
- Java 24 (JDK 24.0.2)
- Spring Boot 3.4.10
- PostgreSQL 16.4 (driver JDBC)
- JavaFX 22 (para módulo desktop)
- Flyway 10.21.0 (migraciones de BD)
- Lombok 1.18.36 (reducción de boilerplate)
- SpringDoc OpenAPI 2.7.0 (documentación Swagger)

---

### **2. Infraestructura de Base de Datos**

#### Docker Compose configurado:

**Archivo:** `docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:16.4
    container_name: edufeed-db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: edufeed
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres-data:/var/lib/postgresql/data

  pgadmin:
    image: dpage/pgadmin4:8.13
    container_name: edufeed-pgadmin
    ports:
      - "5050:80"
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@edufeed.local
      PGADMIN_DEFAULT_PASSWORD: admin
```

**Estado:** ✅ Operativo

**Accesos:**
- PostgreSQL: `localhost:5432` (usuario: `postgres`, password: `postgres`)
- pgAdmin: `http://localhost:5050` (email: `admin@edufeed.local`, password: `admin`)

---

### **3. Scripts de Automatización**

#### Scripts PowerShell creados:

**Ubicación:** `scripts/`

| Script | Propósito | Estado |
|--------|-----------|--------|
| `db-up.ps1` | Levantar stack de Docker (PostgreSQL + pgAdmin) | ✅ Funcional |
| `db-down.ps1` | Detener y limpiar contenedores Docker | ✅ Funcional |
| `setup-dev.ps1` | Validar entorno completo (JDK, Maven, Docker, compilación) | ✅ Funcional |
| `setup-dev.sh` | Versión Bash para Linux/macOS | ✅ Funcional |

**Ejemplo de uso:**
```powershell
# Levantar base de datos
.\scripts\db-up.ps1

# Validar entorno de desarrollo
.\scripts\setup-dev.ps1

# Detener base de datos
.\scripts\db-down.ps1
```

---

### **4. Configuración de Visual Studio Code**

#### Tareas automatizadas (`.vscode/tasks.json`):

| Tarea | Comando | Variables de Entorno |
|-------|---------|---------------------|
| **DB: up** | `pwsh scripts/db-up.ps1` | - |
| **DB: down** | `pwsh scripts/db-down.ps1` | - |
| **Backend: run** | `mvn spring-boot:run` | `JAVA_HOME=C:/Program Files/Java/jdk-24`<br>`SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` |
| **Desktop: run** | `mvn javafx:run` | `JAVA_HOME=C:/Program Files/Java/jdk-24` |

**Nota:** Las tareas configuran explícitamente `JAVA_HOME` para usar Java 24, ya que Maven detecta LibericaJDK 17 en el sistema.

---

### **5. Documentación Técnica**

#### Documentos creados:

```
docs/
├── architecture.md                     [Decisiones de arquitectura]
├── criterios_aceptacion.md             [Criterios SMART para cada RF]
├── manual-instalacion.md               [Guía de instalación paso a paso]
├── manual-usuario.md                   [Manual de usuario (borrador)]
├── plan-intercalado-fases.md           [Planificación de desarrollo]
└── REPO_STRUCTURE.md                   [Organización del repositorio]
```

#### Contenido clave:

**architecture.md:**
- Decisión de arquitectura multi-módulo
- Separación de responsabilidades (backend REST, desktop JavaFX, biometric library)
- Justificación de tecnologías (Spring Boot 3, PostgreSQL 16, JavaFX 22)
- Patrones arquitectónicos (DTO, Repository, Service, Controller)

**criterios_aceptacion.md:**
- Criterios SMART para RF-01 a RF-13
- Métricas de éxito (ej: precisión biométrica ≥95%, latencia <2s)
- Plan de pruebas (unitarias, integración, UAT)

**manual-instalacion.md:**
- Requisitos de hardware y software
- Pasos de instalación de JDK, Maven, Docker
- Configuración de entorno de desarrollo
- Verificación de instalación

---

## 🛠️ Infraestructura y Herramientas

### **Java Development Kit (JDK)**

**Versión:** Java 24.0.2  
**Ubicación:** `C:/Program Files/Java/jdk-24`  
**Verificación:**

```bash
java --version
# java version "24.0.2" 2025-07-15
# Java(TM) SE Runtime Environment (build 24.0.2+12-54)
# Java HotSpot(TM) 64-Bit Server VM (build 24.0.2+12-54, mixed mode)
```

**Configuración:**
- Variable `JAVA_HOME` configurada en tareas de VS Code
- Compatible con Spring Boot 3.4.10 y JavaFX 22

---

### **Apache Maven**

**Versión:** Apache Maven 3.9.9  
**Ubicación:** `C:\Users\Julia\tools\maven\apache-maven-3.9.9`  
**Verificación:**

```bash
mvn --version
# Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
# Maven home: C:\Users\Julia\tools\maven\apache-maven-3.9.9
# Java version: 17.0.13, vendor: BellSoft
```

**Nota:** Maven detecta LibericaJDK 17, pero las tareas de VS Code configuran explícitamente `JAVA_HOME` para usar Java 24.

---

### **Docker Desktop**

**Versión:** Docker Desktop 28.4.0  
**Docker Engine:** 27.4.1  
**Docker Compose:** v2.39.4  

**Verificación:**

```bash
docker --version
# Docker version 27.4.1, build b9d17ea

docker compose version
# Docker Compose version v2.39.4
```

**Contenedores activos:**
- `edufeed-db` (PostgreSQL 16.4) - Puerto 5432
- `edufeed-pgadmin` (pgAdmin 4 8.13) - Puerto 5050

---

## 💾 Base de Datos

### **Esquema Inicial**

**Archivo:** `EduFeed_DB.sql`

**Tablas creadas (13 tablas):**
1. `usuarios` - Información de estudiantes, docentes, personal
2. `plantillas_biometricas` - Datos biométricos cifrados (huella, rostro, voz)
3. `pagos` - Registro de pagos (DIARIO, MENSUAL, PAQUETE)
4. `paquetes_pago` - Paquetes de días prepagados
5. `derechos_uso` - Derechos de acceso generados por pagos
6. `accesos` - Historial de intentos de acceso (APROBADO/DENEGADO)
7. `usos_paquete` - Consumo de días de paquetes
8. `auditoria` - Trazabilidad de cambios administrativos
9. `roles` - Catálogo de roles del sistema
10. `usuarios_roles` - Asignación de roles a usuarios
11. `transacciones_caja` - Integración con sistema de caja
12. `calendario_servicio` - Calendario para reportes de inasistencias
13. `dispositivos` + `credenciales_webauthn` - Soporte WebAuthn (V2)

**Índices optimizados:**
- Búsqueda por documento (usuarios)
- Consultas de derechos vigentes
- Reportes de asistencias por rango de fechas
- Consultas de pagos por usuario y tipo

**Constraints:**
- Claves foráneas con `ON DELETE` apropiado
- Checks de tipos enumerados (TipoUsuario, EstadoPago, Modalidad)
- Validación de vigencias (vigente_hasta >= vigente_desde)
- Unicidad de documentos y referencias externas

---

### **Migraciones Flyway**

**Ubicación:** `edufeed-backend/src/main/resources/db/migration/`

**Migraciones creadas:**
- `V1__init.sql` - Esquema base (13 tablas + índices + constraints)
- `V2__webauthn_tables.sql` - Tablas para autenticación WebAuthn

**Configuración en `application.yml`:**

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: "0"
    validate-on-migrate: true
```

**Variables de entorno:**
- `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` (para bases existentes)
- `SPRING_FLYWAY_BASELINE_VERSION=0`

**Estado:** ✅ Migraciones aplicadas correctamente

---

### **Scripts SQL Complementarios**

**Ubicación:** `scripts/`

| Script | Propósito | Uso |
|--------|-----------|-----|
| `seed/EduFeed_seed.sql` | Datos de prueba (usuarios, pagos, accesos) | Entorno de desarrollo/QA |
| `reportes/EduFeed_vistas.sql` | Vistas y MV para reportes | Performance de consultas |
| `reportes/EduFeed_consultas.sql` | Consultas SQL preparadas | Referencia para APIs |
| `reportes/run_bogota.sql` | Reporte de asistencias Bogotá | Caso de uso específico |

**Vistas materializadas creadas:**
- `mv_ingresos_diarios` - Resumen de ingresos por día
- `vw_derechos_vigentes_hoy` - Derechos activos hoy
- `vw_asistencias_por_dia` - Asistencias agrupadas por día

---

## 📚 Documentación Técnica

### **README.md**

**Contenido:**
- Descripción del proyecto
- Requisitos funcionales (RF-01 a RF-13)
- Requisitos no funcionales (RNF-01 a RNF-02)
- Tecnologías utilizadas
- Instrucciones de instalación
- Instrucciones de ejecución
- Estructura del proyecto
- Licencia y colaboradores

---

### **SECURITY.md**

**Contenido:**
- Política de seguridad
- Controles implementados:
  - Cifrado de plantillas biométricas (RNF-01)
  - Auditoría de operaciones
  - Gestión de credenciales
- Reporte de vulnerabilidades
- Plan de respuesta a incidentes

---

### **Changelogs**

**Archivos:**
- `docs/registros/V1_changelog.md` - Cambios de la versión 1 (esquema inicial)
- `docs/registros/V2_changelog.md` - Cambios de la versión 2 (WebAuthn)

**Contenido:**
- Fecha de cambios
- Descripción detallada de modificaciones
- Scripts de migración ejecutados
- Impacto en el sistema

---

## 🤖 Automatización y Scripts

### **Script: db-up.ps1**

**Propósito:** Levantar stack de Docker con PostgreSQL y pgAdmin

**Flujo:**
1. Verificar que Docker está corriendo
2. Crear red Docker si no existe
3. Ejecutar `docker compose up -d`
4. Esperar a que PostgreSQL esté disponible (health check)
5. Mostrar URLs de acceso

**Salida:**
```
✅ PostgreSQL disponible en localhost:5432
✅ pgAdmin disponible en http://localhost:5050
   Usuario: admin@edufeed.local
   Password: admin
```

---

### **Script: db-down.ps1**

**Propósito:** Detener y limpiar contenedores Docker

**Flujo:**
1. Ejecutar `docker compose down`
2. Opcionalmente eliminar volúmenes (`-v`)
3. Confirmar eliminación de datos

**Advertencia:** Si se usa `-v`, se eliminan todos los datos de la base de datos.

---

### **Script: setup-dev.ps1**

**Propósito:** Validar entorno de desarrollo completo

**Verificaciones:**
1. ✅ Java 24 instalado
2. ✅ Maven 3.9+ instalado
3. ✅ Docker Desktop corriendo
4. ✅ Docker Compose disponible
5. ✅ Compilación exitosa (`mvn clean package`)
6. ✅ Acceso a base de datos (conexión JDBC)

**Salida:**
```
✅ Entorno de desarrollo validado correctamente
✅ Proyecto compila sin errores
✅ Base de datos accesible
✅ Listo para desarrollo
```

---

## 🔄 Siguiente Fase

### **FASE 1: Capa de Dominio y Persistencia**

**Objetivos:**
- Crear entidades JPA para todas las tablas
- Implementar repositorios Spring Data JPA
- Configurar DTOs y mappers
- Escribir tests de repositorio con BD real
- Validar mapeo con Hibernate (ddl-auto: validate)

**Archivos a crear:**
- `model/*.java` (Usuario, Pago, DerechoUso, Acceso, etc.)
- `repository/*.java` (interfaces JpaRepository)
- `dto/*.java` (DTOs con Bean Validation)
- `mapper/*.java` (conversión entity ↔ DTO)
- `test/**/*Test.java` (tests con @DataJpaTest)

**Criterio de aceptación:**
- [ ] Todas las entidades mapean correctamente al schema V1
- [ ] mvn test ejecuta sin errores
- [ ] Cobertura de tests ≥80% en repositorios
- [ ] Hibernate valida schema sin errores

**Estimación:** 2-3 semanas

---

## ✅ Verificación Final

### **Comandos de validación:**

```powershell
# 1. Verificar compilación
mvn clean package

# 2. Levantar base de datos
.\scripts\db-up.ps1

# 3. Arrancar backend
mvn -f edufeed-backend/pom.xml spring-boot:run

# 4. Verificar endpoints
# Swagger UI: http://localhost:8080/swagger
# Health: http://localhost:8080/actuator/health
# OpenAPI: http://localhost:8080/api-docs
```

**Estado esperado:**
- ✅ Compilación exitosa
- ✅ Backend arranca sin errores
- ✅ Flyway aplica migraciones V1 y V2
- ✅ Swagger UI accesible
- ✅ Health check retorna `{"status": "UP"}`

---

## 📊 Métricas de la Fase

| Métrica | Valor | Objetivo |
|---------|-------|----------|
| Tiempo de desarrollo | 1 semana | ✅ 1 semana |
| Módulos Maven creados | 4 | ✅ 4 |
| Scripts de automatización | 4 | ✅ 4 |
| Documentos técnicos | 6 | ✅ 6 |
| Tablas de BD creadas | 13 | ✅ 13 |
| Migraciones Flyway | 2 | ✅ 2 |
| Cobertura de tests | 0% | ⚠️ N/A (siguiente fase) |

---

## 🎓 Lecciones Aprendidas

1. **Maven detecta JDK incorrecto:** Configurar explícitamente `JAVA_HOME` en tareas de VS Code
2. **Flyway con BD existente:** Usar `baseline-on-migrate=true` para evitar errores
3. **Docker Compose v2:** Usar `docker compose` (sin guion) en lugar de `docker-compose`
4. **Live Share:** Configurar correctamente para trabajo colaborativo remoto
5. **Documentación temprana:** Crear documentación técnica desde el inicio facilita el desarrollo

---

**Fecha de actualización:** 20 de octubre de 2025  
**Responsable:** Equipo EduFeed  
**Estado del proyecto:** ✅ FASE 0 COMPLETADA - Iniciando FASE 1
