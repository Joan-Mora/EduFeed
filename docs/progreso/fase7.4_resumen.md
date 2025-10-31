# FASE 7.4 · Pruebas de Aceptación de Usuario (UAT)

**Fecha:** 29 de octubre de 2025  
**Estado:** ✅ Infraestructura lista para ejecución

## Objetivos

- Preparar ambiente de staging con datos seed realistas
- Facilitar acceso a stakeholders y usuarios finales
- Ejecutar casos de prueba predefinidos por rol
- Recopilar feedback y bugs críticos
- Validar criterios de aceptación antes de producción

## Criterios de Éxito

- [ ] ≥90% de casos de prueba exitosos
- [ ] Feedback positivo de usuarios finales (sin bloqueadores)
- [ ] Bugs críticos resueltos antes de producción

---

## Ambiente de Staging

### Infraestructura

**Base de datos:**
- PostgreSQL 16.4 en contenedor Docker
- DB: `edufeed_staging` (puerto 5433)
- pgAdmin: http://localhost:5051
  - User: `uat@local.test`
  - Pass: `admin123`

**Backend:**
- Perfil: `staging`
- Puerto: 8081
- URL API: http://localhost:8081
- Swagger UI: http://localhost:8081/swagger

**Configuración:**
- Archivo: `edufeed-backend/src/main/resources/application-staging.yml`
- Variables: `.env.staging` (generado automáticamente)

---

## Scripts de Staging

### 1. Levantar Base de Datos

**Script:** `scripts/staging-db-up.ps1`

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/staging-db-up.ps1
```

**Acciones:**
- Crea `.env.staging` si no existe
- Levanta PostgreSQL en puerto 5433
- Levanta pgAdmin en puerto 5051
- Valida contenedores arriba con `docker ps`

---

### 2. Sembrar Datos Realistas

**Script:** `scripts/staging-db-seed.ps1`

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/staging-db-seed.ps1
```

**Datos sembrados:**
- 4 usuarios (Ana, Bruno, Carla, Diego) con diferentes tipos
- Plantillas biométricas mock (HUELLA, ROSTRO)
- 15 días de calendario de servicio
- 3 pagos aprobados (DIARIO, MENSUAL, PAQUETE)
- Derechos de uso activos vinculados a pagos
- Accesos de ejemplo (aprobados y denegados)
- Transacciones de caja conciliadas

**Fuente:** `scripts/seed/EduFeed_seed.sql`

---

### 3. Levantar Backend

**Script:** `scripts/staging-backend-run.ps1`

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/staging-backend-run.ps1
```

**Acciones:**
- Carga variables desde `.env.staging`
- Activa perfil `staging`
- Ejecuta `mvn spring-boot:run` en puerto 8081
- Flyway aplica migraciones v1…v6 automáticamente
- DevOperatorSeeder crea operadores por rol:
  - `admin / Admin123$` (ADMIN, OPERADOR_CAJA, OPERADOR_ACCESO, AUDITOR)
  - `caja1 / Caja123$` (OPERADOR_CAJA)
  - `acceso1 / Acceso123$` (OPERADOR_ACCESO)
  - `auditor1 / Auditor123$` (AUDITOR)

---

## Usuarios y Credenciales

### Operadores (Login API)

| Username | Password | Roles |
|----------|----------|-------|
| `admin` | `Admin123$` | ADMIN, OPERADOR_CAJA, OPERADOR_ACCESO, AUDITOR |
| `caja1` | `Caja123$` | OPERADOR_CAJA |
| `acceso1` | `Acceso123$` | OPERADOR_ACCESO |
| `auditor1` | `Auditor123$` | AUDITOR |

**Configuración:** `application-staging.yml`

```yaml
app:
  seed:
    operadores:
      enabled: true
      username: admin
      password: Admin123$
      roles: ROLE_ADMIN,ROLE_OPERADOR_CAJA,ROLE_OPERADOR_ACCESO,ROLE_AUDITOR
      extras: "caja1|Caja123$|ROLE_OPERADOR_CAJA; acceso1|Acceso123$|ROLE_OPERADOR_ACCESO; auditor1|Auditor123$|ROLE_AUDITOR"
```

### Usuarios del Sistema (Seed)

| Documento | Nombre | Tipo | Email |
|-----------|--------|------|-------|
| 1001 | Ana Niño | NINO | ana@colegio.edu |
| 1002 | Bruno Estudiante | ESTUDIANTE | bruno@colegio.edu |
| 1003 | Carla Docente | DOCENTE | carla@colegio.edu |
| 1004 | Diego Personal | PERSONAL | diego@colegio.edu |

**Nota:** Ana tiene derecho mensual activo, Bruno paquete activo, Carla diario activo.

---

## Casos de Prueba UAT

### Operador de Caja (10 pagos variados)

**Credenciales:** `caja1 / Caja123$`

**Casos:**

1. **Crear pago DIARIO en efectivo**
   - Endpoint: `POST /api/pagos`
   - Body: `{"usuarioId":"<UUID>","monto":12000,"tipoPago":"DIARIO","metodoPago":"EFECTIVO"}`
   - Esperado: 201 CREATED

2. **Crear pago MENSUAL con tarjeta**
   - Body: `{"usuarioId":"<UUID>","monto":120000,"tipoPago":"MENSUAL","metodoPago":"TARJETA","referenciaExterna":"REF-UAT-001"}`
   - Esperado: 201 CREATED

3. **Crear pago PAQUETE 5 días**
   - Body: `{"usuarioId":"<UUID>","monto":50000,"tipoPago":"PAQUETE","metodoPago":"TRANSFERENCIA","diasPaquete":5}`
   - Esperado: 201 CREATED

4. **Aprobar pago**
   - Endpoint: `PUT /api/pagos/{id}/aprobar`
   - Esperado: 200 OK, derecho de uso generado automáticamente

5. **Rechazar pago**
   - Endpoint: `PUT /api/pagos/{id}/rechazar`
   - Esperado: 200 OK, estado=RECHAZADO

6. **Consultar pagos por estado APROBADO**
   - Endpoint: `GET /api/pagos/estado/APROBADO`
   - Esperado: 200 OK, lista de pagos aprobados

7. **Consultar pagos de un usuario**
   - Endpoint: `GET /api/pagos/usuario/{usuarioId}`
   - Esperado: 200 OK

8. **Consultar pagos por tipo MENSUAL**
   - Endpoint: `GET /api/pagos/tipo/MENSUAL`
   - Esperado: 200 OK

9. **Consultar pagos en rango de fechas**
   - Endpoint: `GET /api/pagos/rango?desde=2025-10-01T00:00:00-05:00&hasta=2025-10-31T23:59:59-05:00`
   - Esperado: 200 OK

10. **Actualizar método de pago**
    - Endpoint: `PUT /api/pagos/{id}`
    - Body: `{"metodoPago":"EFECTIVO"}`
    - Esperado: 200 OK

**Validaciones:**
- Todos los pagos se crean con estado PENDIENTE
- Aprobar pago genera derecho de uso automáticamente
- Referencias externas son únicas

---

### Operador de Acceso (20 verificaciones)

**Credenciales:** `acceso1 / Acceso123$`

**Casos:**

1-10. **Verificar acceso APROBADO (10 usuarios con derecho)**
   - Endpoint: `POST /api/accesos/verificar`
   - Body: `{"usuarioId":"<UUID-Ana>","modalidad":"HUELLA"}`
   - Esperado: 200 OK, `permitido:true`, `estado:APROBADO`

11-20. **Verificar acceso DENEGADO (10 usuarios sin derecho)**
   - Body: `{"usuarioId":"<UUID-Diego>","modalidad":"HUELLA"}`
   - Esperado: 200 OK, `permitido:false`, `estado:DENEGADO`, `motivo:SIN_DERECHO_VIGENTE`

**Validaciones:**
- Accesos aprobados se registran vinculados al derecho
- Accesos denegados se registran con motivo claro
- Paquetes consumen 1 día por acceso aprobado

---

### Administrador (5 usuarios + biometría)

**Credenciales:** `admin / Admin123$`

**Casos:**

1. **Crear usuario tipo ESTUDIANTE**
   - Endpoint: `POST /api/usuarios`
   - Body: `{"documento":"UAT-001","nombreCompleto":"Usuario UAT 1","tipoUsuario":"ESTUDIANTE","email":"uat1@test.local","telefono":"3001234567","activo":true}`
   - Esperado: 201 CREATED

2. **Enrolar biometría HUELLA**
   - Endpoint: `POST /api/usuarios/{id}/biometria/enrolar`
   - Body: `{"modalidad":"HUELLA"}`
   - Esperado: 201 CREATED

3. **Enrolar biometría ROSTRO**
   - Body: `{"modalidad":"ROSTRO"}`
   - Esperado: 201 CREATED

4. **Listar plantillas biométricas del usuario**
   - Endpoint: `GET /api/usuarios/{id}/biometria`
   - Esperado: 200 OK, lista con 2 plantillas (HUELLA, ROSTRO)

5. **Desactivar plantilla biométrica**
   - Endpoint: `DELETE /api/usuarios/{id}/biometria/{plantillaId}`
   - Esperado: 204 NO CONTENT

**Validaciones:**
- Usuarios creados están activos por defecto
- Biometría se enrola correctamente (mock en staging)
- Plantillas desactivadas no se usan en verificación

---

### Auditor (Reportes)

**Credenciales:** `auditor1 / Auditor123$`

**Casos:**

1. **Consultar reporte de ingresos**
   - Endpoint: `GET /api/reportes/ingresos`
   - Esperado: 200 OK, lista de ingresos agregados por día/tipo/método

2. **Consultar resumen de ingresos**
   - Endpoint: `GET /api/reportes/ingresos/resumen`
   - Esperado: 200 OK, suma total

3. **Consultar reporte de asistencias**
   - Endpoint: `GET /api/reportes/asistencias`
   - Esperado: 200 OK, lista de asistencias por día

4. **Consultar reporte de rechazos**
   - Endpoint: `GET /api/reportes/rechazos`
   - Esperado: 200 OK, lista de rechazos por día/motivo

5. **Consultar derechos activos**
   - Endpoint: `GET /api/reportes/derechos-activos`
   - Esperado: 200 OK, lista de derechos vigentes

6. **Exportar ingresos a CSV**
   - Endpoint: `GET /api/reportes/ingresos.csv`
   - Esperado: 200 OK, Content-Type: text/csv

**Validaciones:**
- Reportes reflejan datos seed correctamente
- Filtros por fechas funcionan (parámetros `desde` y `hasta`)
- CSV se descarga sin errores

---

## Atajos con REST Client

**Archivo:** `docs/uat.http`

Requiere extensión VS Code: `humao.rest-client`

**Uso:**
1. Abrir `docs/uat.http` en VS Code
2. Ejecutar bloques en orden:
   - Login por rol → tokens guardados en variables
   - Admin: crear usuario → enrolar biometría
   - Caja: crear pago → aprobar pago
   - Acceso: verificar acceso (aprobado y denegado)
   - Auditor: consultar reportes

**Ventajas:**
- Requests prearmados con variables dinámicas
- GUIDs y timestamps aleatorios
- Chaining de responses (usar `id` de respuesta anterior)

**Ejemplo:**
```http
### Login Admin
# @name login_admin
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin123$"
}

###
@adminToken = Bearer {{login_admin.response.body.$.accessToken}}

### Crear usuario
# @name admin_create_user
POST http://localhost:8081/api/usuarios
Authorization: {{adminToken}}
Content-Type: application/json

{
  "documento": "UAT{{$guid}}",
  "nombreCompleto": "Usuario UAT {{$guid}}",
  "tipoUsuario": "ESTUDIANTE",
  "email": "uat{{$randomInt 1000 9999}}@test.local",
  "telefono": "300{{$randomInt 1000000 9999999}}",
  "activo": true
}

###
@userId = {{admin_create_user.response.body.$.id}}
```

---

## Registro de Resultados

**Archivo:** `docs/uat_resultados.md`

**Formato:**

| Fecha | Caso | Rol | Paso | Resultado | Evidencia | Observaciones | Severidad | Estatus |
|-------|------|-----|------|-----------|-----------|---------------|-----------|---------|
| 2025-10-29 | Pagos variados | Operador Caja | Crear DIARIO EFECTIVO | OK | captura1.png | - | - | Cerrado |

**Evidencias:**
- Adjuntar capturas en `docs/uat-evidencias/`
- Referenciar en columna "Evidencia"

**Estatus:**
- Abierto
- En Progreso
- Cerrado

**Severidad (para bugs):**
- Crítica: bloquea flujo principal
- Mayor: impacta funcionalidad importante
- Menor: defecto cosmético o edge case

---

## Flujo de Ejecución UAT

### Preparación (una sola vez)

1. **Levantar infraestructura:**
   ```powershell
   # DB + pgAdmin
   ./scripts/staging-db-up.ps1
   
   # Seed datos realistas
   ./scripts/staging-db-seed.ps1
   
   # Backend (en terminal separada)
   ./scripts/staging-backend-run.ps1
   ```

2. **Validar servicios:**
   - pgAdmin: http://localhost:5051
   - Backend: http://localhost:8081/actuator/health
   - Swagger: http://localhost:8081/swagger

### Ejecución de casos (por rol)

3. **Compartir credenciales con stakeholders:**
   - Operador caja: `caja1 / Caja123$`
   - Operador acceso: `acceso1 / Acceso123$`
   - Administrador: `admin / Admin123$`
   - Auditor: `auditor1 / Auditor123$`

4. **Ejecutar casos de prueba:**
   - Opción 1: Usar Swagger UI (http://localhost:8081/swagger)
   - Opción 2: Usar REST Client (`docs/uat.http`)
   - Opción 3: Usar Postman/Insomnia (importar OpenAPI spec)

5. **Registrar resultados:**
   - Llenar tabla en `docs/uat_resultados.md`
   - Adjuntar capturas en `docs/uat-evidencias/`
   - Documentar cualquier bug encontrado

### Cierre

6. **Revisión de resultados:**
   - Contar casos OK vs FAIL
   - Calcular % de éxito (meta: ≥90%)
   - Priorizar bugs críticos

7. **Reunión de feedback:**
   - Recopilar observaciones de stakeholders
   - Identificar mejoras de UX
   - Validar aceptación del sistema

---

## Troubleshooting

### Problema: 401 Unauthorized

**Causa:** Token JWT no válido o expirado.

**Solución:**
1. Hacer login nuevamente: `POST /api/auth/login`
2. Copiar `accessToken` de la respuesta
3. Usar como header: `Authorization: Bearer <token>`

---

### Problema: 403 Forbidden

**Causa:** Usuario no tiene el rol requerido.

**Solución:**
- Verificar roles del usuario en tabla `operadores`
- Usar usuario `admin` para endpoints que requieren `ROLE_ADMIN`

---

### Problema: 404 Not Found en usuarios seed

**Causa:** Seed no se aplicó correctamente.

**Solución:**
```powershell
# Reiniciar BD staging y aplicar seed
./scripts/staging-db-up.ps1
./scripts/staging-db-seed.ps1
```

---

### Problema: Backend no arranca (puerto 8081 en uso)

**Causa:** Otra instancia del backend corriendo.

**Solución:**
```powershell
# Detener proceso en puerto 8081
Get-Process -Name java | Stop-Process -Force

# O cambiar puerto en .env.staging
PORT=8082
```

---

## Métricas de UAT

### Casos de Prueba

| Rol | Casos Planeados | Casos Ejecutados | OK | FAIL | % Éxito |
|-----|-----------------|------------------|----|----|---------|
| Operador Caja | 10 | - | - | - | - |
| Operador Acceso | 20 | - | - | - | - |
| Administrador | 5 | - | - | - | - |
| Auditor | 6 | - | - | - | - |
| **Total** | **41** | - | - | - | - |

### Bugs Reportados

| Severidad | Cantidad | Resueltos | Pendientes |
|-----------|----------|-----------|------------|
| Crítica | - | - | - |
| Mayor | - | - | - |
| Menor | - | - | - |

---

## Archivos Clave

```
scripts/
├── staging-db-up.ps1              # Levantar DB staging
├── staging-db-seed.ps1            # Sembrar datos
├── staging-backend-run.ps1        # Levantar backend staging
└── seed/
    └── EduFeed_seed.sql           # Datos realistas

edufeed-backend/src/main/resources/
├── application-staging.yml        # Perfil staging
└── db/migration/
    └── V1__init.sql...V6          # Migraciones Flyway

docs/
├── uat.md                         # Guía UAT (este archivo)
├── uat.http                       # REST Client requests
├── uat_resultados.md              # Registro de casos
└── uat-evidencias/                # Capturas/logs
    └── .gitkeep

.env.staging                       # Variables de entorno (auto-generado)
```

---

## Criterios de Salida (Done)

✅ Ambiente staging funcional con:
- Base de datos con datos seed realistas
- Backend en puerto 8081 con operadores por rol
- Documentación y atajos preparados

⏳ Pendiente de validación:
- [ ] ≥90% de casos de prueba exitosos
- [ ] Feedback positivo de usuarios finales
- [ ] Bugs críticos resueltos

---

## Próximos Pasos

1. **Ejecutar UAT** con stakeholders (coordinador de caso de uso)
2. **Recopilar resultados** en `docs/uat_resultados.md`
3. **Priorizar y resolver bugs críticos** encontrados
4. **Validar criterios de aceptación**
5. Si todo OK → **Aprobación para producción**

---

**Responsable:** Product Owner  
**Ejecutores:** Stakeholders + Equipo QA  
**Soporte Técnico:** Equipo de Desarrollo  
**Estado:** ✅ Listo para ejecutar
