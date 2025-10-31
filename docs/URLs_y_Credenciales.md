# URLs de Acceso y Credenciales
## Sistema EduFeed v2.0

**Fecha**: 31 de octubre de 2025  
**Versión**: 2.0.0  
**Entorno**: Producción

⚠️ **DOCUMENTO CONFIDENCIAL** - Restringir acceso solo a personal autorizado

---

## 🌐 URLs de Acceso

### Aplicación Principal

| Servicio | URL | Puerto | Descripción |
|----------|-----|--------|-------------|
| **Backend API** | `http://localhost:8080/api` | 8080 | API REST principal |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | 8080 | Documentación interactiva de API |
| **Aplicación Desktop** | Local (JavaFX) | N/A | EduFeed Desktop App |

### Base de Datos

| Servicio | URL | Puerto | Descripción |
|----------|-----|--------|-------------|
| **PostgreSQL** | `localhost:5432` | 5432 | Base de datos principal |
| **pgAdmin** | `http://localhost:5050` | 5050 | Administrador web de PostgreSQL |

### Observabilidad (Opcional - si están deployadas)

| Servicio | URL | Puerto | Descripción |
|----------|-----|--------|-------------|
| **Prometheus** | `http://localhost:9090` | 9090 | Métricas del sistema |
| **Grafana** | `http://localhost:3000` | 3000 | Dashboards de monitoreo |

---

## 🔑 Credenciales por Rol

### 1. Administrador Principal

**Usuario**: `admin`  
**Contraseña**: `Admin@2025!Edufeed`  
**Rol**: `ADMIN`  
**Email**: `admin@edufeed.com`  

**Permisos**:
- ✅ Gestión completa de usuarios
- ✅ Registro biométrico
- ✅ Configuración del sistema
- ✅ Reportes avanzados
- ✅ Auditoría y logs
- ✅ Gestión de pagos (incluye ajustes excepcionales)
- ✅ Acceso a todos los módulos

**Uso recomendado**: Solo para tareas administrativas críticas

---

### 2. Operador de Caja #1

**Usuario**: `cajero1`  
**Contraseña**: `Cajero@2025!`  
**Rol**: `OPERADOR_CAJA`  
**Email**: `cajero1@edufeed.com`  

**Permisos**:
- ✅ Registro de pagos
- ✅ Generación de comprobantes
- ✅ Reportes de ingresos (solo lectura)
- ❌ NO puede editar usuarios
- ❌ NO puede acceder a configuración
- ❌ NO puede eliminar pagos

**Uso recomendado**: Turno mañana (8AM-2PM)

---

### 3. Operador de Caja #2

**Usuario**: `cajero2`  
**Contraseña**: `Cajero@2025!`  
**Rol**: `OPERADOR_CAJA`  
**Email**: `cajero2@edufeed.com`  

**Permisos**: Idénticos a cajero1

**Uso recomendado**: Turno tarde (2PM-8PM)

---

### 4. Operador de Acceso #1

**Usuario**: `acceso1`  
**Contraseña**: `Acceso@2025!`  
**Rol**: `OPERADOR_ACCESO`  
**Email**: `acceso1@edufeed.com`  

**Permisos**:
- ✅ Verificación biométrica de acceso
- ✅ Consulta de vigencias
- ✅ Registro de accesos (log)
- ❌ NO puede registrar pagos
- ❌ NO puede modificar usuarios
- ❌ NO puede configurar sistema

**Uso recomendado**: Entrada principal

---

### 5. Operador de Acceso #2

**Usuario**: `acceso2`  
**Contraseña**: `Acceso@2025!`  
**Rol**: `OPERADOR_ACCESO`  
**Email**: `acceso2@edufeed.com`  

**Permisos**: Idénticos a acceso1

**Uso recomendado**: Entrada secundaria (si aplica)

---

### 6. Auditor

**Usuario**: `auditor`  
**Contraseña**: `Auditor@2025!`  
**Rol**: `AUDITOR`  
**Email**: `auditor@edufeed.com`  

**Permisos**:
- ✅ Consulta de todos los reportes
- ✅ Acceso a logs de auditoría
- ✅ Exportación de datos
- ✅ Visualización de dashboards
- ❌ NO puede crear/editar/eliminar registros
- ❌ Solo lectura en todo el sistema

**Uso recomendado**: Revisiones periódicas, auditorías contables

---

## 🗄️ Base de Datos

### PostgreSQL

**Host**: `localhost` (o IP del servidor)  
**Puerto**: `5432`  
**Base de datos**: `edufeed_db`  
**Usuario**: `edufeed_user`  
**Contraseña**: `EduFeed_DB_P@ssw0rd!2025`  

**String de conexión**:
```
jdbc:postgresql://localhost:5432/edufeed_db?user=edufeed_user&password=EduFeed_DB_P@ssw0rd!2025
```

---

### pgAdmin

**URL**: `http://localhost:5050`  
**Email**: `admin@edufeed.com`  
**Contraseña**: `PgAdmin@2025!`  

**Configuración de servidor** (dentro de pgAdmin):
- **Nombre**: EduFeed Production
- **Host**: `postgres` (si usa Docker) o `localhost`
- **Puerto**: `5432`
- **Usuario DB**: `edufeed_user`
- **Contraseña DB**: `EduFeed_DB_P@ssw0rd!2025`

---

## 📊 Observabilidad (Opcional)

### Prometheus

**URL**: `http://localhost:9090`  
**Usuario**: N/A (sin autenticación por defecto)  
**Contraseña**: N/A  

**Endpoints monitoreados**:
- `/actuator/prometheus` (Backend)

---

### Grafana

**URL**: `http://localhost:3000`  
**Usuario**: `admin`  
**Contraseña**: `Grafana@2025!`  

**Dashboards configurados**:
- Sistema EduFeed Overview
- Performance Metrics
- Database Metrics
- Biometric Operations

---

## 🔐 Matriz de Permisos

| Funcionalidad | ADMIN | OPERADOR_CAJA | OPERADOR_ACCESO | AUDITOR |
|---------------|-------|---------------|-----------------|---------|
| **Usuarios** |
| Crear usuario | ✅ | ❌ | ❌ | ❌ |
| Editar usuario | ✅ | ❌ | ❌ | ❌ |
| Desactivar usuario | ✅ | ❌ | ❌ | ❌ |
| Ver usuarios | ✅ | ❌ | ❌ | ✅ |
| **Biometría** |
| Registrar huella | ✅ | ❌ | ❌ | ❌ |
| Registrar rostro | ✅ | ❌ | ❌ | ❌ |
| Verificar acceso | ✅ | ❌ | ✅ | ❌ |
| **Pagos** |
| Registrar pago | ✅ | ✅ | ❌ | ❌ |
| Editar vigencia | ✅ | ❌ | ❌ | ❌ |
| Anular pago | ✅ | ❌ | ❌ | ❌ |
| Ver historial | ✅ | ✅ | ❌ | ✅ |
| **Reportes** |
| Ingresos diarios | ✅ | ✅ | ❌ | ✅ |
| Reportes avanzados | ✅ | ❌ | ❌ | ✅ |
| Logs de auditoría | ✅ | ❌ | ❌ | ✅ |
| **Configuración** |
| Cambiar tarifas | ✅ | ❌ | ❌ | ❌ |
| Configurar sistema | ✅ | ❌ | ❌ | ❌ |
| Gestionar roles | ✅ | ❌ | ❌ | ❌ |

---

## 🔄 Procedimiento de Cambio de Contraseñas

### Para Usuarios del Sistema

1. Login como `admin`
2. Ir a "Gestión de Usuarios"
3. Buscar el usuario
4. Click en "Editar" → "Cambiar contraseña"
5. Ingresar nueva contraseña (debe cumplir requisitos)
6. Confirmar

**Requisitos de contraseña**:
- Mínimo 8 caracteres
- Al menos 1 mayúscula
- Al menos 1 minúscula
- Al menos 1 número
- Al menos 1 símbolo especial (@, !, #, $, %, etc.)

---

### Para Base de Datos

**PostgreSQL**:
```sql
-- Conectar como superusuario
psql -U postgres

-- Cambiar contraseña
ALTER USER edufeed_user WITH PASSWORD 'NuevaContraseña!2025';
```

**Actualizar en aplicación**:
Editar `edufeed-backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    password: NuevaContraseña!2025
```

---

### Para pgAdmin

1. Abrir pgAdmin
2. Click derecho en servidor → "Properties"
3. Tab "Connection"
4. Actualizar contraseña
5. Guardar

---

## 📞 Contactos de Soporte

| Rol | Nombre | Email | Teléfono | Disponibilidad |
|-----|--------|-------|----------|----------------|
| **Soporte Técnico** | [Nombre] | soporte@edufeed.com | [Número] | Lun-Vie 8AM-6PM |
| **Administrador BD** | [Nombre] | dba@edufeed.com | [Número] | Lun-Vie 9AM-5PM |
| **Líder de Proyecto** | [Nombre] | lider@edufeed.com | [Número] | Lun-Vie 8AM-6PM |
| **Emergencias 24/7** | Guardia | emergencias@edufeed.com | [Número] | 24/7 |

---

## ⚠️ Seguridad

### Buenas Prácticas

✅ **DO (Hacer)**:
- Cambiar contraseñas por defecto inmediatamente
- Usar contraseñas únicas para cada usuario
- NO compartir credenciales entre usuarios
- Cerrar sesión al terminar el turno
- Habilitar HTTPS en producción
- Realizar backups de credenciales en lugar seguro
- Rotar contraseñas cada 90 días

❌ **DON'T (No hacer)**:
- Escribir contraseñas en papel
- Enviar contraseñas por email sin cifrar
- Usar contraseñas débiles (ej. "12345", "password")
- Dejar sesiones abiertas sin supervisión
- Compartir cuenta de admin con múltiples personas

---

### Acceso Remoto (si aplica)

Si se requiere acceso remoto:

1. **Configurar VPN**:
   - Usar VPN institucional
   - NO exponer puertos directamente a internet

2. **Configurar SSH (para servidores)**:
   - Desactivar login root
   - Usar autenticación por llave pública
   - Cambiar puerto por defecto (22 → otro)

3. **Configurar Firewall**:
   - Permitir solo IPs conocidas
   - Bloquear todo lo demás por defecto

---

## 🔍 Verificación de Acceso

### Checklist de Prueba

Después de configurar, verificar que:

- [ ] Admin puede login correctamente
- [ ] Cajero1 puede registrar pagos
- [ ] Cajero2 puede registrar pagos
- [ ] Acceso1 puede verificar biometría
- [ ] Acceso2 puede verificar biometría
- [ ] Auditor puede ver reportes (solo lectura)
- [ ] Auditor NO puede editar nada
- [ ] pgAdmin se conecta a PostgreSQL
- [ ] Swagger UI muestra endpoints correctamente
- [ ] Prometheus recibe métricas (si aplica)
- [ ] Grafana muestra dashboards (si aplica)

---

## 📝 Historial de Cambios

| Fecha | Cambio | Responsable |
|-------|--------|-------------|
| 31/10/2025 | Creación inicial del documento | [Nombre] |
| ___/___/___ | Cambio de contraseña de admin | [Nombre] |
| ___/___/___ | Creación de usuario adicional | [Nombre] |

---

## 📄 Anexos

### A. Ejemplo de Configuración application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/edufeed_db
    username: edufeed_user
    password: EduFeed_DB_P@ssw0rd!2025
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    
server:
  port: 8080
  
jwt:
  secret: EduFeed_JWT_Secret_Key_2025_V2.0_Production
  expiration: 28800000 # 8 horas en milisegundos
```

---

### B. Ejemplo de docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: edufeed_db
      POSTGRES_USER: edufeed_user
      POSTGRES_PASSWORD: EduFeed_DB_P@ssw0rd!2025
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  pgadmin:
    image: dpage/pgadmin4
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@edufeed.com
      PGADMIN_DEFAULT_PASSWORD: PgAdmin@2025!
    ports:
      - "5050:80"
    depends_on:
      - postgres

volumes:
  postgres_data:
```

---

**Última actualización**: 31 de octubre de 2025  
**Próxima revisión**: Después de primer despliegue en producción  
**Confidencialidad**: 🔒 RESTRINGIDO
