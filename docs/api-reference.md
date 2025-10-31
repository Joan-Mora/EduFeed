# Referencia de API REST - Sistema EduFeed

**Versión**: 2.0  
**Fecha**: 31 de octubre de 2025  
**Base URL**: `http://localhost:8080` (desarrollo) | `https://edufeed.com` (producción)

---

## Índice

1. [Introducción](#introducción)
2. [Autenticación](#autenticación)
3. [Módulo de Usuarios](#módulo-de-usuarios)
4. [Módulo de Pagos](#módulo-de-pagos)
5. [Módulo de Accesos](#módulo-de-accesos)
6. [Módulo de Reportes](#módulo-de-reportes)
7. [Módulo de Webhooks](#módulo-de-webhooks)
8. [Módulo de Auditoría](#módulo-de-auditoría)
9. [WebAuthn (Biometría Web)](#webauthn-biometría-web)
10. [Códigos de estado HTTP](#códigos-de-estado-http)
11. [Manejo de errores](#manejo-de-errores)

---

## Introducción

La API REST de EduFeed permite la gestión completa del sistema de control de acceso y cobro para comedores educativos. Todos los endpoints están documentados con ejemplos funcionales en `curl`.

### Convenciones

- **Formato de datos**: JSON (Content-Type: application/json)
- **Codificación**: UTF-8
- **Fechas**: ISO-8601 con zona horaria (ej. `2025-10-31T14:30:00-05:00`)
- **IDs**: UUID v4 (ej. `550e8400-e29b-41d4-a716-446655440000`)
- **Autenticación**: JWT Bearer token (excepto `/api/auth/login`)

### Variables de entorno

```bash
# Desarrollo
export API_URL="http://localhost:8080"

# Staging
export API_URL="http://staging.edufeed.com:8081"

# Producción
export API_URL="https://edufeed.com"
```

---

## Autenticación

### POST /api/auth/login

Autentica un operador y devuelve tokens JWT.

**Permisos**: Público (no requiere autenticación previa)

**Request**:

```bash
curl -X POST $API_URL/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "Admin123!"
  }'
```

**Response** (200 OK):

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTY5ODc2NTQzMiwiZXhwIjoxNjk4NzY5MDMyfQ.xyz...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInR5cGUiOiJyZWZyZXNoIiwiaWF0IjoxNjk4NzY1NDMyLCJleHAiOjE2OTk0MDU0MzJ9.abc..."
}
```

**Errores comunes**:

- `401 Unauthorized`: Credenciales inválidas
- `423 Locked`: Cuenta bloqueada por intentos fallidos

**Almacenar token**:

```bash
# Guardar token en variable de entorno
export TOKEN=$(curl -s -X POST $API_URL/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123!"}' \
  | jq -r '.accessToken')

echo $TOKEN
```

---

### POST /api/auth/refresh

Renueva el access token usando el refresh token.

**Permisos**: Público

**Request**:

```bash
curl -X POST $API_URL/api/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

**Response** (200 OK):

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.nuevo_token...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.nuevo_refresh..."
}
```

**Errores**:
- `401 Unauthorized`: Refresh token inválido o expirado

---

## Módulo de Usuarios

Base path: `/api/usuarios`  
Permisos requeridos: `ADMIN`

### POST /api/usuarios

Crea un nuevo usuario (estudiante, docente, administrativo).

**Request**:

```bash
curl -X POST $API_URL/api/usuarios \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "documento": "1234567890",
    "tipoDocumento": "CC",
    "nombre": "Juan",
    "apellido": "Pérez",
    "email": "juan.perez@example.com",
    "telefono": "+573001234567",
    "tipo": "ESTUDIANTE",
    "grado": "11A",
    "jornada": "MAÑANA",
    "activo": true
  }'
```

**Response** (201 Created):

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "documento": "1234567890",
  "tipoDocumento": "CC",
  "nombre": "Juan",
  "apellido": "Pérez",
  "nombreCompleto": "Juan Pérez",
  "email": "juan.perez@example.com",
  "telefono": "+573001234567",
  "tipo": "ESTUDIANTE",
  "grado": "11A",
  "jornada": "MAÑANA",
  "activo": true,
  "fechaRegistro": "2025-10-31T10:30:00-05:00",
  "tieneBiometria": false
}
```

**Validaciones**:
- `documento`: Requerido, 6-20 caracteres alfanuméricos
- `nombre`, `apellido`: Requeridos, 2-100 caracteres
- `email`: Formato válido (RFC 5322)
- `tipo`: Enum [ESTUDIANTE, DOCENTE, ADMINISTRATIVO]
- `jornada`: Enum [MAÑANA, TARDE, NOCHE, UNICA]

**Errores**:
- `400 Bad Request`: Validación fallida
- `409 Conflict`: Documento ya registrado

---

### GET /api/usuarios

Lista todos los usuarios.

**Query params**:
- `soloActivos` (boolean, opcional): Filtrar solo usuarios activos

**Request**:

```bash
# Listar todos
curl -X GET $API_URL/api/usuarios \
  -H "Authorization: Bearer $TOKEN"

# Solo activos
curl -X GET "$API_URL/api/usuarios?soloActivos=true" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK):

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "documento": "1234567890",
    "nombreCompleto": "Juan Pérez",
    "tipo": "ESTUDIANTE",
    "activo": true,
    "tieneBiometria": true
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "documento": "0987654321",
    "nombreCompleto": "María García",
    "tipo": "DOCENTE",
    "activo": true,
    "tieneBiometria": false
  }
]
```

---

### GET /api/usuarios (paginado)

Lista usuarios con paginación.

**Query params**:
- `page` (int, default: 0): Número de página
- `size` (int, default: 20): Tamaño de página

**Request**:

```bash
curl -X GET "$API_URL/api/usuarios?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK):

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "documento": "1234567890",
      "nombreCompleto": "Juan Pérez",
      "tipo": "ESTUDIANTE",
      "activo": true
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0,
    "paged": true
  },
  "totalPages": 5,
  "totalElements": 50,
  "last": false,
  "first": true,
  "numberOfElements": 10
}
```

---

### GET /api/usuarios/{id}

Obtiene un usuario por ID.

**Request**:

```bash
curl -X GET $API_URL/api/usuarios/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Igual al de POST /api/usuarios

**Errores**:
- `404 Not Found`: Usuario no encontrado

---

### GET /api/usuarios/buscar/documento/{documento}

Busca usuario por número de documento.

**Request**:

```bash
curl -X GET $API_URL/api/usuarios/buscar/documento/1234567890 \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Usuario encontrado (mismo formato que GET por ID)

**Errores**:
- `404 Not Found`: Documento no registrado

---

### GET /api/usuarios/buscar/nombre

Busca usuarios por nombre o apellido (búsqueda parcial).

**Query params**:
- `q` (string, requerido): Texto a buscar

**Request**:

```bash
curl -X GET "$API_URL/api/usuarios/buscar/nombre?q=Juan" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Array de usuarios que coinciden

---

### GET /api/usuarios/buscar/tipo/{tipo}

Filtra usuarios por tipo.

**Path params**:
- `tipo`: ESTUDIANTE | DOCENTE | ADMINISTRATIVO

**Request**:

```bash
curl -X GET $API_URL/api/usuarios/buscar/tipo/ESTUDIANTE \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Array de usuarios del tipo especificado

---

### PUT /api/usuarios/{id}

Actualiza datos de un usuario.

**Request**:

```bash
curl -X PUT $API_URL/api/usuarios/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "documento": "1234567890",
    "nombre": "Juan Carlos",
    "apellido": "Pérez López",
    "email": "juan.perez.nuevo@example.com",
    "telefono": "+573009876543",
    "tipo": "ESTUDIANTE",
    "grado": "11B",
    "jornada": "TARDE",
    "activo": true
  }'
```

**Response** (200 OK): Usuario actualizado

**Errores**:
- `404 Not Found`: Usuario no existe
- `409 Conflict`: Documento duplicado (si se cambió)

---

### DELETE /api/usuarios/{id}

Desactiva un usuario (soft delete).

**Request**:

```bash
curl -X DELETE $API_URL/api/usuarios/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (204 No Content)

**Errores**:
- `404 Not Found`: Usuario no existe

---

### POST /api/usuarios/{id}/reactivar

Reactiva un usuario previamente desactivado.

**Request**:

```bash
curl -X POST $API_URL/api/usuarios/550e8400-e29b-41d4-a716-446655440000/reactivar \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK)

---

### POST /api/usuarios/{id}/biometria/enrolar

Registra datos biométricos para un usuario.

**Request**:

```bash
curl -X POST $API_URL/api/usuarios/550e8400-e29b-41d4-a716-446655440000/biometria/enrolar \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "modalidad": "HUELLA",
    "template": "BASE64_ENCODED_FINGERPRINT_TEMPLATE...",
    "metadatos": {
      "dedo": "INDICE_DERECHO",
      "calidad": 95,
      "sensor": "DigitalPersona U.are.U 4500"
    }
  }'
```

**Modalidades soportadas**:
- `HUELLA`: Huella dactilar
- `ROSTRO`: Reconocimiento facial (OpenCV)
- `VOZ`: Reconocimiento de voz

**Response** (201 Created):

```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
  "modalidad": "HUELLA",
  "fechaCreacion": "2025-10-31T11:00:00-05:00",
  "metadatos": {
    "dedo": "INDICE_DERECHO",
    "calidad": 95,
    "sensor": "DigitalPersona U.are.U 4500"
  }
}
```

**Errores**:
- `400 Bad Request`: Template inválido o modalidad no soportada
- `404 Not Found`: Usuario no existe
- `409 Conflict`: Ya existe plantilla de esa modalidad

---

### GET /api/usuarios/{id}/biometria

Lista plantillas biométricas de un usuario.

**Request**:

```bash
curl -X GET $API_URL/api/usuarios/550e8400-e29b-41d4-a716-446655440000/biometria \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK):

```json
[
  {
    "id": "770e8400-e29b-41d4-a716-446655440002",
    "modalidad": "HUELLA",
    "fechaCreacion": "2025-10-31T11:00:00-05:00",
    "metadatos": {
      "dedo": "INDICE_DERECHO"
    }
  },
  {
    "id": "880e8400-e29b-41d4-a716-446655440003",
    "modalidad": "ROSTRO",
    "fechaCreacion": "2025-10-31T11:05:00-05:00",
    "metadatos": {
      "resolucion": "640x480"
    }
  }
]
```

---

### DELETE /api/usuarios/{id}/biometria/{plantillaId}

Elimina una plantilla biométrica.

**Request**:

```bash
curl -X DELETE $API_URL/api/usuarios/550e8400-e29b-41d4-a716-446655440000/biometria/770e8400-e29b-41d4-a716-446655440002 \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (204 No Content)

**Errores**:
- `404 Not Found`: Usuario o plantilla no encontrada

---

## Módulo de Pagos

Base path: `/api/pagos`  
Permisos: `ADMIN`, `OPERADOR_CAJA`

### POST /api/pagos

Registra un nuevo pago y calcula vigencia automáticamente.

**Request**:

```bash
curl -X POST $API_URL/api/pagos \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "monto": 50000.00,
    "tipo": "MENSUAL",
    "metodoPago": "EFECTIVO",
    "referencia": "PAGO-2025-001",
    "cajeroId": "990e8400-e29b-41d4-a716-446655440010",
    "observaciones": "Pago completo mes de noviembre"
  }'
```

**Tipos de pago**:
- `DIARIO`: 1 día de vigencia
- `SEMANAL`: 7 días
- `QUINCENAL`: 15 días
- `MENSUAL`: 30 días
- `TRIMESTRAL`: 90 días
- `SEMESTRAL`: 180 días
- `ANUAL`: 365 días

**Métodos de pago**:
- `EFECTIVO`
- `TARJETA`
- `TRANSFERENCIA`
- `PSE`
- `QR_BANCOLOMBIA`
- `NEQUI`

**Response** (201 Created):

```json
{
  "id": "aa0e8400-e29b-41d4-a716-446655440020",
  "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
  "monto": 50000.00,
  "tipo": "MENSUAL",
  "metodoPago": "EFECTIVO",
  "estado": "APROBADO",
  "referencia": "PAGO-2025-001",
  "cajeroId": "990e8400-e29b-41d4-a716-446655440010",
  "fechaPago": "2025-10-31T12:00:00-05:00",
  "vigenciaInicio": "2025-10-31T00:00:00-05:00",
  "vigenciaFin": "2025-11-30T23:59:59-05:00",
  "observaciones": "Pago completo mes de noviembre"
}
```

**Validaciones**:
- `monto`: > 0, máximo 2 decimales
- `tipo`: Enum válido
- `usuarioId`: Usuario debe existir y estar activo
- `cajeroId`: Operador debe existir

---

### GET /api/pagos

Lista todos los pagos.

**Request**:

```bash
curl -X GET $API_URL/api/pagos \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Array de pagos

---

### GET /api/pagos/{id}

Obtiene un pago por ID.

**Request**:

```bash
curl -X GET $API_URL/api/pagos/aa0e8400-e29b-41d4-a716-446655440020 \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Pago encontrado

---

### GET /api/pagos/usuario/{usuarioId}

Lista pagos de un usuario.

**Request**:

```bash
curl -X GET $API_URL/api/pagos/usuario/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Array de pagos del usuario

---

### GET /api/pagos/tipo/{tipo}

Filtra pagos por tipo.

**Request**:

```bash
curl -X GET $API_URL/api/pagos/tipo/MENSUAL \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Array de pagos del tipo especificado

---

### GET /api/pagos/estado/{estado}

Filtra pagos por estado.

**Estados**:
- `APROBADO`
- `PENDIENTE`
- `RECHAZADO`

**Request**:

```bash
curl -X GET $API_URL/api/pagos/estado/APROBADO \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Array de pagos con ese estado

---

### GET /api/pagos/rango

Filtra pagos por rango de fechas.

**Query params**:
- `inicio` (ISO-8601, requerido)
- `fin` (ISO-8601, requerido)

**Request**:

```bash
curl -X GET "$API_URL/api/pagos/rango?inicio=2025-10-01T00:00:00-05:00&fin=2025-10-31T23:59:59-05:00" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Array de pagos en el rango

---

### PUT /api/pagos/{id}

Actualiza un pago existente.

**Request**:

```bash
curl -X PUT $API_URL/api/pagos/aa0e8400-e29b-41d4-a716-446655440020 \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "metodoPago": "TRANSFERENCIA",
    "referencia": "PAGO-2025-001-UPD",
    "observaciones": "Actualizado a transferencia"
  }'
```

**Response** (200 OK): Pago actualizado

**Nota**: Solo se pueden actualizar ciertos campos (método, referencia, observaciones).

---

### PUT /api/pagos/{id}/aprobar

Aprueba un pago pendiente.

**Request**:

```bash
curl -X PUT $API_URL/api/pagos/aa0e8400-e29b-41d4-a716-446655440020/aprobar \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "aprobadorId": "990e8400-e29b-41d4-a716-446655440010",
    "observaciones": "Pago verificado y aprobado"
  }'
```

**Response** (200 OK): Pago aprobado

---

### PUT /api/pagos/{id}/rechazar

Rechaza un pago pendiente.

**Request**:

```bash
curl -X PUT $API_URL/api/pagos/aa0e8400-e29b-41d4-a716-446655440020/rechazar \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "rechazadorId": "990e8400-e29b-41d4-a716-446655440010",
    "motivoRechazo": "Pago duplicado"
  }'
```

**Response** (200 OK): Pago rechazado

---

## Módulo de Accesos

Base path: `/api/accesos`  
Permisos: `OPERADOR_ACCESO`, `SUPERVISOR`, `ADMIN`

### POST /api/accesos/verificar

Verifica derecho de acceso y registra el intento.

**Request**:

```bash
curl -X POST $API_URL/api/accesos/verificar \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "modalidadBiometrica": "HUELLA"
  }'
```

**Response** (200 OK - PERMITIDO):

```json
{
  "permitido": true,
  "estado": "PERMITIDO",
  "mensaje": "Acceso concedido. Bienvenido Juan Pérez.",
  "usuario": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "nombreCompleto": "Juan Pérez",
    "tipo": "ESTUDIANTE",
    "foto": "https://..."
  },
  "derecho": {
    "tipo": "MENSUAL",
    "vigenciaFin": "2025-11-30T23:59:59-05:00",
    "diasRestantes": 30
  },
  "accesoId": "bb0e8400-e29b-41d4-a716-446655440030"
}
```

**Response** (200 OK - DENEGADO):

```json
{
  "permitido": false,
  "estado": "DENEGADO",
  "mensaje": "Acceso denegado. No tiene derecho vigente.",
  "razon": "SIN_DERECHO",
  "orientacionCaja": true,
  "usuario": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "nombreCompleto": "Juan Pérez",
    "tipo": "ESTUDIANTE"
  },
  "accesoId": "cc0e8400-e29b-41d4-a716-446655440031"
}
```

**Razones de denegación**:
- `SIN_DERECHO`: No tiene pago vigente
- `DERECHO_EXPIRADO`: Vigencia caducada
- `USUARIO_INACTIVO`: Usuario desactivado
- `BIOMETRIA_NO_COINCIDE`: Template biométrico no validado

---

### GET /api/accesos/historial

Consulta historial de accesos con filtros.

**Query params**:
- `usuarioId` (UUID, opcional)
- `inicio` (ISO-8601, opcional)
- `fin` (ISO-8601, opcional)
- `estado` (PERMITIDO|DENEGADO, opcional)
- `page` (int, default: 0)
- `size` (int, default: 20)
- `sort` (string, default: "fechaHora,desc")

**Request**:

```bash
# Historial completo (últimos 20)
curl -X GET "$API_URL/api/accesos/historial" \
  -H "Authorization: Bearer $TOKEN"

# Filtrar por usuario y fecha
curl -X GET "$API_URL/api/accesos/historial?usuarioId=550e8400-e29b-41d4-a716-446655440000&inicio=2025-10-01T00:00:00-05:00&fin=2025-10-31T23:59:59-05:00" \
  -H "Authorization: Bearer $TOKEN"

# Solo accesos denegados
curl -X GET "$API_URL/api/accesos/historial?estado=DENEGADO&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK):

```json
{
  "content": [
    {
      "id": "bb0e8400-e29b-41d4-a716-446655440030",
      "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
      "nombreUsuario": "Juan Pérez",
      "estado": "PERMITIDO",
      "fechaHora": "2025-10-31T13:00:00-05:00",
      "modalidadBiometrica": "HUELLA"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "size": 20,
  "number": 0
}
```

---

### GET /api/accesos/usuario/{usuarioId}/dia

Obtiene accesos de un usuario en un día específico.

**Query params**:
- `fecha` (ISO-8601 date, opcional, default: hoy)

**Request**:

```bash
# Accesos de hoy
curl -X GET $API_URL/api/accesos/usuario/550e8400-e29b-41d4-a716-446655440000/dia \
  -H "Authorization: Bearer $TOKEN"

# Accesos de fecha específica
curl -X GET "$API_URL/api/accesos/usuario/550e8400-e29b-41d4-a716-446655440000/dia?fecha=2025-10-30" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK):

```json
[
  {
    "id": "bb0e8400-e29b-41d4-a716-446655440030",
    "estado": "PERMITIDO",
    "fechaHora": "2025-10-31T13:00:00-05:00",
    "modalidadBiometrica": "HUELLA"
  },
  {
    "id": "cc0e8400-e29b-41d4-a716-446655440031",
    "estado": "PERMITIDO",
    "fechaHora": "2025-10-31T18:30:00-05:00",
    "modalidadBiometrica": "ROSTRO"
  }
]
```

---

## Módulo de Reportes

Base path: `/api/reportes`  
Permisos: `SUPERVISOR`, `ADMIN`

### GET /api/reportes/ingresos

Reporte de ingresos por pagos.

**Query params**:
- `inicio` (ISO-8601, requerido)
- `fin` (ISO-8601, requerido)

**Request**:

```bash
curl -X GET "$API_URL/api/reportes/ingresos?inicio=2025-10-01T00:00:00-05:00&fin=2025-10-31T23:59:59-05:00" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK):

```json
[
  {
    "id": "aa0e8400-e29b-41d4-a716-446655440020",
    "usuario": "Juan Pérez",
    "monto": 50000.00,
    "tipo": "MENSUAL",
    "metodoPago": "EFECTIVO",
    "fechaPago": "2025-10-15T12:00:00-05:00",
    "cajero": "María González"
  }
]
```

---

### GET /api/reportes/ingresos/resumen

Resumen agregado de ingresos.

**Query params**:
- `inicio` (ISO-8601, requerido)
- `fin` (ISO-8601, requerido)

**Request**:

```bash
curl -X GET "$API_URL/api/reportes/ingresos/resumen?inicio=2025-10-01T00:00:00-05:00&fin=2025-10-31T23:59:59-05:00" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK):

```json
{
  "totalIngresos": 2500000.00,
  "cantidadPagos": 50,
  "porMetodoPago": {
    "EFECTIVO": 1500000.00,
    "TRANSFERENCIA": 800000.00,
    "TARJETA": 200000.00
  },
  "porTipo": {
    "DIARIO": 300000.00,
    "MENSUAL": 2000000.00,
    "TRIMESTRAL": 200000.00
  },
  "periodo": {
    "inicio": "2025-10-01T00:00:00-05:00",
    "fin": "2025-10-31T23:59:59-05:00"
  }
}
```

---

### GET /api/reportes/ingresos.csv

Exporta reporte de ingresos en formato CSV.

**Query params**:
- `inicio` (ISO-8601, requerido)
- `fin` (ISO-8601, requerido)

**Request**:

```bash
curl -X GET "$API_URL/api/reportes/ingresos.csv?inicio=2025-10-01T00:00:00-05:00&fin=2025-10-31T23:59:59-05:00" \
  -H "Authorization: Bearer $TOKEN" \
  --output ingresos_octubre_2025.csv
```

**Response** (200 OK, Content-Type: text/csv):

```csv
ID,Usuario,Documento,Monto,Tipo,Metodo,Fecha,Cajero
aa0e8400-e29b-41d4-a716-446655440020,Juan Pérez,1234567890,50000.00,MENSUAL,EFECTIVO,2025-10-15T12:00:00-05:00,María González
```

---

### GET /api/reportes/asistencias

Reporte de asistencias (accesos permitidos).

**Query params**:
- `inicio` (ISO-8601, requerido)
- `fin` (ISO-8601, requerido)

**Request**:

```bash
curl -X GET "$API_URL/api/reportes/asistencias?inicio=2025-10-01T00:00:00-05:00&fin=2025-10-31T23:59:59-05:00" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK):

```json
[
  {
    "usuario": "Juan Pérez",
    "documento": "1234567890",
    "tipo": "ESTUDIANTE",
    "grado": "11A",
    "totalAccesos": 22,
    "accesos": [
      {
        "fecha": "2025-10-01",
        "hora": "13:00:00",
        "modalidad": "HUELLA"
      },
      {
        "fecha": "2025-10-02",
        "hora": "13:05:00",
        "modalidad": "ROSTRO"
      }
    ]
  }
]
```

---

### GET /api/reportes/asistencias.csv

Exporta asistencias en CSV.

**Request**:

```bash
curl -X GET "$API_URL/api/reportes/asistencias.csv?inicio=2025-10-01T00:00:00-05:00&fin=2025-10-31T23:59:59-05:00" \
  -H "Authorization: Bearer $TOKEN" \
  --output asistencias_octubre_2025.csv
```

**Response** (200 OK, text/csv)

---

### GET /api/reportes/rechazos

Reporte de accesos denegados.

**Query params**:
- `inicio` (ISO-8601, requerido)
- `fin` (ISO-8601, requerido)

**Request**:

```bash
curl -X GET "$API_URL/api/reportes/rechazos?inicio=2025-10-01T00:00:00-05:00&fin=2025-10-31T23:59:59-05:00" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK):

```json
[
  {
    "usuario": "Pedro Martínez",
    "documento": "5555555555",
    "razon": "SIN_DERECHO",
    "fecha": "2025-10-15T13:00:00-05:00"
  }
]
```

---

### GET /api/reportes/derechos-activos

Lista usuarios con derecho vigente actual.

**Request**:

```bash
curl -X GET $API_URL/api/reportes/derechos-activos \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK):

```json
[
  {
    "usuario": "Juan Pérez",
    "documento": "1234567890",
    "tipo": "ESTUDIANTE",
    "tipoDerecho": "MENSUAL",
    "vigenciaFin": "2025-11-30T23:59:59-05:00",
    "diasRestantes": 30
  }
]
```

---

## Módulo de Webhooks

Base path: `/api`  
Permisos: `ADMIN`, `OPERADOR_CAJA`

### POST /api/webhooks/pagos

Endpoint para recibir notificaciones de pasarelas de pago (PSE, Nequi, Bancolombia).

**Request** (ejemplo PSE):

```bash
curl -X POST $API_URL/api/webhooks/pagos \
  -H 'Content-Type: application/json' \
  -H 'X-Webhook-Signature: sha256=...' \
  -d '{
    "proveedor": "PSE",
    "transaccionExterna": "PSE-2025-10-31-001",
    "estado": "APROBADA",
    "monto": 50000.00,
    "referencia": "EDUFEED-PAGO-123",
    "metadatos": {
      "banco": "Bancolombia",
      "cus": "1234567890"
    }
  }'
```

**Response** (200 OK):

```json
{
  "recibido": true,
  "transaccionId": "dd0e8400-e29b-41d4-a716-446655440040",
  "mensaje": "Webhook procesado exitosamente"
}
```

**Nota**: Este endpoint es público pero debe validar firma HMAC en header `X-Webhook-Signature`.

---

### GET /api/transacciones

Lista todas las transacciones de webhooks.

**Request**:

```bash
curl -X GET $API_URL/api/transacciones \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Array de transacciones

---

### GET /api/transacciones/{id}

Obtiene transacción por ID.

**Request**:

```bash
curl -X GET $API_URL/api/transacciones/dd0e8400-e29b-41d4-a716-446655440040 \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Transacción

---

### GET /api/transacciones/no-conciliadas

Lista transacciones sin conciliar (no vinculadas a pago).

**Request**:

```bash
curl -X GET $API_URL/api/transacciones/no-conciliadas \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Array de transacciones pendientes

---

### PUT /api/transacciones/{id}/conciliar

Vincula transacción a un pago.

**Request**:

```bash
curl -X PUT $API_URL/api/transacciones/dd0e8400-e29b-41d4-a716-446655440040/conciliar \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "pagoId": "aa0e8400-e29b-41d4-a716-446655440020"
  }'
```

**Response** (200 OK): Transacción conciliada

---

## Módulo de Auditoría

Base path: `/api/auditoria`  
Permisos: `ADMIN`

### GET /api/auditoria

Lista eventos de auditoría.

**Query params**:
- `page` (int, default: 0)
- `size` (int, default: 50)

**Request**:

```bash
curl -X GET "$API_URL/api/auditoria?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK):

```json
{
  "content": [
    {
      "id": "ee0e8400-e29b-41d4-a716-446655440050",
      "entidad": "Usuario",
      "operacion": "CREATE",
      "operadorId": "990e8400-e29b-41d4-a716-446655440010",
      "operadorNombre": "Admin Principal",
      "fecha": "2025-10-31T10:30:00-05:00",
      "detalles": {
        "usuarioCreado": "Juan Pérez",
        "documento": "1234567890"
      }
    }
  ],
  "totalElements": 500,
  "totalPages": 25
}
```

---

### GET /api/auditoria/{id}

Obtiene evento de auditoría por ID.

**Request**:

```bash
curl -X GET $API_URL/api/auditoria/ee0e8400-e29b-41d4-a716-446655440050 \
  -H "Authorization: Bearer $TOKEN"
```

**Response** (200 OK): Evento de auditoría

---

## WebAuthn (Biometría Web)

Base path: `/api/webauthn`  
Permisos: Varía según endpoint

### POST /api/webauthn/registro/iniciar

Inicia registro de credencial biométrica (ej. Windows Hello).

**Request**:

```bash
curl -X POST $API_URL/api/webauthn/registro/iniciar \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
    "nombreDispositivo": "Windows Hello - PC Principal"
  }'
```

**Response** (200 OK):

```json
{
  "sesionId": "ff0e8400-e29b-41d4-a716-446655440060",
  "challenge": "BASE64_CHALLENGE...",
  "opciones": {
    "rp": {
      "name": "EduFeed",
      "id": "edufeed.com"
    },
    "user": {
      "id": "BASE64_USER_ID",
      "name": "juan.perez@example.com",
      "displayName": "Juan Pérez"
    },
    "pubKeyCredParams": [
      { "type": "public-key", "alg": -7 }
    ],
    "timeout": 60000
  }
}
```

---

### POST /api/webauthn/registro/{sesionId}/completar

Completa registro enviando credencial firmada.

**Request**:

```bash
curl -X POST $API_URL/api/webauthn/registro/ff0e8400-e29b-41d4-a716-446655440060/completar \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "credencialId": "BASE64_CREDENTIAL_ID",
    "clientDataJSON": "BASE64_CLIENT_DATA",
    "attestationObject": "BASE64_ATTESTATION"
  }'
```

**Response** (200 OK):

```json
{
  "registrado": true,
  "credencialId": "ff0e8400-e29b-41d4-a716-446655440061"
}
```

---

## Códigos de estado HTTP

| Código | Significado | Uso típico |
|--------|-------------|-----------|
| **200 OK** | Solicitud exitosa | GET, PUT exitosos |
| **201 Created** | Recurso creado | POST exitoso |
| **204 No Content** | Éxito sin contenido | DELETE exitoso |
| **400 Bad Request** | Datos inválidos | Validación fallida |
| **401 Unauthorized** | No autenticado | Token inválido/ausente |
| **403 Forbidden** | Sin permisos | Rol insuficiente |
| **404 Not Found** | Recurso no existe | GET de ID inexistente |
| **409 Conflict** | Conflicto de estado | Duplicado (documento, email) |
| **422 Unprocessable Entity** | Lógica de negocio | Usuario inactivo, derecho expirado |
| **500 Internal Server Error** | Error del servidor | Error no manejado |
| **503 Service Unavailable** | Servicio no disponible | DB caída, mantenimiento |

---

## Manejo de errores

Todos los endpoints devuelven errores en formato JSON estandarizado:

**Formato de error**:

```json
{
  "timestamp": "2025-10-31T14:30:00-05:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validación fallida",
  "path": "/api/usuarios",
  "detalles": [
    {
      "campo": "documento",
      "mensaje": "El documento debe tener entre 6 y 20 caracteres"
    },
    {
      "campo": "email",
      "mensaje": "El email no es válido"
    }
  ]
}
```

**Ejemplo con curl**:

```bash
curl -X POST $API_URL/api/usuarios \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "documento": "123",
    "email": "correo-invalido"
  }'
```

**Response** (400 Bad Request):

```json
{
  "timestamp": "2025-10-31T14:30:00-05:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Errores de validación",
  "detalles": [
    {
      "campo": "documento",
      "mensaje": "size must be between 6 and 20"
    },
    {
      "campo": "nombre",
      "mensaje": "must not be blank"
    },
    {
      "campo": "email",
      "mensaje": "must be a well-formed email address"
    }
  ]
}
```

---

## Paginación

Endpoints que retornan listas grandes soportan paginación:

**Parámetros**:
- `page`: Número de página (0-indexed)
- `size`: Elementos por página
- `sort`: Campo y dirección (ej. `nombre,asc` o `fechaCreacion,desc`)

**Ejemplo**:

```bash
curl -X GET "$API_URL/api/usuarios?page=1&size=10&sort=nombre,asc" \
  -H "Authorization: Bearer $TOKEN"
```

**Response**:

```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 1,
    "pageSize": 10,
    "offset": 10
  },
  "totalPages": 5,
  "totalElements": 50,
  "last": false,
  "first": false,
  "size": 10,
  "number": 1
}
```

---

## Testing de API

### Postman Collection

Importar colección JSON:

```bash
# Descargar colección (si está disponible)
curl -o edufeed-api.postman_collection.json \
  https://raw.githubusercontent.com/Joan-Mora/EduFeed/main/docs/postman/edufeed-api.json
```

### Script de pruebas completo

```bash
#!/bin/bash
# test-api.sh - Script de pruebas de API

API_URL="http://localhost:8080"

# 1. Login
echo "=== 1. Login ==="
TOKEN=$(curl -s -X POST $API_URL/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123!"}' \
  | jq -r '.accessToken')

echo "Token: ${TOKEN:0:50}..."

# 2. Crear usuario
echo -e "\n=== 2. Crear usuario ==="
USUARIO_ID=$(curl -s -X POST $API_URL/api/usuarios \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "documento": "9999999999",
    "tipoDocumento": "CC",
    "nombre": "Test",
    "apellido": "API",
    "email": "test.api@example.com",
    "tipo": "ESTUDIANTE",
    "jornada": "MAÑANA",
    "activo": true
  }' | jq -r '.id')

echo "Usuario creado: $USUARIO_ID"

# 3. Registrar pago
echo -e "\n=== 3. Registrar pago ==="
PAGO_ID=$(curl -s -X POST $API_URL/api/pagos \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{
    \"usuarioId\": \"$USUARIO_ID\",
    \"monto\": 30000.00,
    \"tipo\": \"MENSUAL\",
    \"metodoPago\": \"EFECTIVO\"
  }" | jq -r '.id')

echo "Pago registrado: $PAGO_ID"

# 4. Verificar acceso
echo -e "\n=== 4. Verificar acceso ==="
curl -s -X POST $API_URL/api/accesos/verificar \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{
    \"usuarioId\": \"$USUARIO_ID\",
    \"modalidadBiometrica\": \"HUELLA\"
  }" | jq .

# 5. Reporte de ingresos
echo -e "\n=== 5. Reporte ingresos (hoy) ==="
curl -s -X GET "$API_URL/api/reportes/ingresos/resumen?inicio=$(date -u +%Y-%m-01T00:00:00Z)&fin=$(date -u +%Y-%m-%dT23:59:59Z)" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo -e "\n=== Pruebas completadas ==="
```

**Ejecutar**:

```bash
chmod +x test-api.sh
./test-api.sh
```

---

**Última actualización**: 31 de octubre de 2025  
**Versión de la API**: 2.0  
**Mantenido por**: Equipo EduFeed

Para soporte: api@edufeed.com
