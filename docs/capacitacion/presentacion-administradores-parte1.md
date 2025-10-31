# Capacitación: Administradores y Auditores
## Sistema EduFeed v2.0 - PARTE 1/2

---

## Agenda (3 horas)

**PARTE 1** (1.5 horas):
1. Arquitectura del sistema (20 min)
2. Gestión de usuarios (25 min)
3. Registro biométrico (25 min)
4. **DESCANSO** (10 min)
5. Gestión de pagos (10 min)

**PARTE 2** (1.5 horas):
6. Reportes avanzados (30 min)
7. Auditoría y logs (20 min)
8. Configuración del sistema (15 min)
9. Troubleshooting avanzado (15 min)
10. Ejercicio integrador (10 min)

---

## 1. Arquitectura del Sistema

### Visión general

```
┌─────────────────────────────────────────────┐
│           SISTEMA EDUFEED v2.0              │
├─────────────────────────────────────────────┤
│                                             │
│  ┌──────────┐   ┌──────────┐   ┌─────────┐ │
│  │  DESKTOP │   │ BACKEND  │   │   BASE  │ │
│  │   APP    │◄─►│   API    │◄─►│   DE    │ │
│  │ (JavaFX) │   │ (Spring) │   │  DATOS  │ │
│  └──────────┘   └──────────┘   │(Postgres│ │
│                                 │   SQL)  │ │
│  ┌──────────┐                  └─────────┘ │
│  │BIOMETRIC │                               │
│  │ DEVICES  │                               │
│  │(USB SDK) │                               │
│  └──────────┘                               │
└─────────────────────────────────────────────┘
```

---

### Componentes principales

**1. Frontend (EduFeed Desktop)**
- Tecnología: JavaFX
- Función: Interfaz de usuario para Caja, Acceso, Admin
- Instalación: Windows, Linux, macOS
- Conexión: REST API al backend

**2. Backend (EduFeed API)**
- Tecnología: Spring Boot
- Función: Lógica de negocio, validaciones, seguridad
- Puerto: 8080 (configurable)
- Base de datos: PostgreSQL

---

**3. Base de Datos (PostgreSQL)**
- Versión: 14 o superior
- Tablas principales:
  - `usuarios`: Estudiantes, docentes, admin
  - `pagos`: Registro de transacciones
  - `accesos`: Log de verificaciones biométricas
  - `huellas_dactilares`: Plantillas biométricas
  - `reconocimiento_facial`: Imágenes/vectores
  - `configuracion`: Parámetros del sistema
- Respaldos: Automáticos diarios

---

**4. Biometric SDK**
- Drivers de huellas: DigitalPersona, ZKTeco, etc.
- Drivers de cámara: OpenCV, Face Recognition API
- Algoritmo: Matching 1:1 o 1:N
- Umbral de coincidencia: 0.85 (configurable)

---

### Flujo de datos: Registro de pago

```
1. CAJERO                    2. FRONTEND
   Registra pago    ───►     Valida formulario
                                  │
                                  ↓
                             3. BACKEND API
                              POST /api/pagos
                              - Valida usuario existe
                              - Calcula vigencia
                              - Guarda en DB
                                  │
                                  ↓
                             4. BASE DE DATOS
                              INSERT INTO pagos
                              UPDATE usuarios.vigencia
                                  │
                                  ↓
                             5. RESPUESTA
                              {id, comprobante, vigencia}
                                  │
                                  ↓
                             6. FRONTEND
                              Muestra comprobante
```

---

### Flujo de datos: Verificación de acceso

```
1. USUARIO                   2. DISPOSITIVO
   Coloca huella    ───►     Captura imagen
                                  │
                                  ↓
                             3. FRONTEND
                              Extrae patrón biométrico
                                  │
                                  ↓
                             4. BACKEND API
                              POST /api/accesos/verificar
                              - Busca huella en DB (1:N)
                              - Verifica vigencia
                                  │
                                  ↓
                             5. BASE DE DATOS
                              SELECT FROM huellas WHERE...
                              SELECT FROM pagos WHERE...
                                  │
                                  ↓
                             6. RESPUESTA
                              {match, usuario, vigente}
                                  │
                                  ↓
                             7. FRONTEND
                              Muestra resultado
                              Registra acceso en DB
```

---

### Arquitectura de seguridad

**Autenticación**:
- JWT (JSON Web Tokens)
- Expiración: 8 horas
- Roles: `ADMIN`, `OPERADOR_CAJA`, `OPERADOR_ACCESO`, `AUDITOR`

**Autorización**:
- RBAC (Role-Based Access Control)
- Permisos granulares por endpoint

**Ejemplo**:
```
POST /api/usuarios → Solo ADMIN
GET /api/reportes → ADMIN + AUDITOR
POST /api/pagos → ADMIN + OPERADOR_CAJA
```

---

**Encriptación**:
- Contraseñas: BCrypt (salt rounds: 12)
- Datos biométricos: AES-256
- Comunicación: HTTPS (en producción)

**Auditoría**:
- Todas las operaciones quedan registradas
- Log incluye: usuario, acción, timestamp, IP, resultado

---

## 2. Gestión de Usuarios

### Tipos de usuarios

| Tipo | Rol en sistema | Permisos | Casos de uso |
|------|----------------|----------|--------------|
| **ESTUDIANTE** | Usuario final | Solo acceso | Estudiantes de la institución |
| **DOCENTE** | Usuario final | Solo acceso | Profesores, staff académico |
| **ADMINISTRATIVO** | Usuario final | Solo acceso | Personal administrativo |
| **OPERADOR_CAJA** | Operador | Módulo Caja | Cajeros |
| **OPERADOR_ACCESO** | Operador | Módulo Acceso | Control de acceso |
| **ADMIN** | Administrador | Todos | Gestión completa |
| **AUDITOR** | Auditor | Solo lectura | Revisión, informes |

---

### Crear usuario

**Pasos (en módulo Admin)**:
1. Ir a "Gestión de Usuarios" → "Nuevo Usuario"
2. Llenar formulario:
   - **Documento**: Único, obligatorio
   - **Nombre completo**: Obligatorio
   - **Tipo usuario**: ESTUDIANTE/DOCENTE/etc.
   - **Grado/Área**: Opcional (útil para estudiantes)
   - **Email**: Opcional
   - **Teléfono**: Opcional
   - **Estado**: ACTIVO (por defecto)
3. Click "Guardar"
4. Sistema genera ID automáticamente

---

### Formulario de creación

```
┌─────────────────────────────────────────┐
│  CREAR USUARIO                          │
├─────────────────────────────────────────┤
│  Documento*: [1234567890______________] │
│  Nombre*:    [Juan Pérez______________] │
│  Tipo*:      [▼ ESTUDIANTE           ] │
│  Grado:      [11A_____________________] │
│  Email:      [juan@correo.com________] │
│  Teléfono:   [3001234567______________] │
│  Estado:     [● ACTIVO  ○ INACTIVO   ] │
│                                         │
│  [Guardar]  [Cancelar]                 │
└─────────────────────────────────────────┘
```

**Validaciones**:
- Documento ya existe → Error
- Nombre vacío → Error
- Tipo no seleccionado → Error

---

### Editar usuario

**Pasos**:
1. Buscar usuario (por documento o nombre)
2. Click en "Editar"
3. Modificar campos (excepto documento)
4. Click "Guardar cambios"

**Campos editables**:
- Nombre
- Tipo de usuario
- Grado/Área
- Email
- Teléfono
- Estado (Activo/Inactivo)

**Campo NO editable**:
- Documento (es clave primaria)

---

### Desactivar/Reactivar usuario

**Desactivar** (no eliminar):
- Editar usuario
- Cambiar estado a "INACTIVO"
- Guardar

**Efecto**:
- No puede acceder al comedor
- Pagos previos quedan en historial
- Datos biométricos se conservan
- Puede reactivarse después

**Reactivar**:
- Cambiar estado a "ACTIVO"
- Si hay pago vigente previo, verificar fechas

---

### Búsqueda de usuarios

**Filtros disponibles**:
- Por documento (exacto)
- Por nombre (parcial, ej. "Juan" encuentra "Juan Pérez")
- Por tipo (ESTUDIANTE, DOCENTE, etc.)
- Por grado (ej. "11A")
- Por estado (ACTIVO, INACTIVO)

**Ordenamiento**:
- Alfabético por nombre
- Por fecha de creación
- Por tipo

---

### Importación masiva (CSV)

**Formato esperado**:
```csv
documento,nombre,tipo,grado,email,telefono
1234567890,Juan Pérez,ESTUDIANTE,11A,juan@mail.com,3001234567
9876543210,Ana López,DOCENTE,Matemáticas,ana@mail.com,3009876543
```

**Pasos**:
1. Preparar archivo CSV
2. Ir a "Gestión de Usuarios" → "Importar CSV"
3. Seleccionar archivo
4. Sistema valida y muestra preview
5. Confirmar importación

**Manejo de errores**:
- Documento duplicado → Fila se omite
- Campos obligatorios vacíos → Fila se omite
- Se genera reporte de errores

---

## 3. Registro Biométrico

### ¿Por qué registrar biometría?

**Sin registro biométrico**:
- Usuario existe en sistema
- Puede pagar
- **NO puede acceder** (no hay huella/rostro registrado)

**Con registro biométrico**:
- Usuario puede verificarse en módulo de Acceso
- Sistema compara huella/rostro con plantilla almacenada

---

### Flujo de registro

```
1. Usuario debe estar creado en sistema
   ↓
2. Admin abre "Registro Biométrico"
   ↓
3. Busca usuario por documento
   ↓
4. Captura huella (preferible) o rostro
   ↓
5. Sistema guarda plantilla en DB
   ↓
6. Usuario ya puede usar módulo de Acceso
```

---

### Registrar huella dactilar

**Paso a paso**:
1. Conectar lector de huellas
2. Ir a "Registro Biométrico" → "Nueva Huella"
3. Buscar usuario
4. Solicitar al usuario que coloque el dedo (índice derecho preferido)
5. Capturar 3 muestras del mismo dedo (para calidad)
6. Sistema genera plantilla y guarda
7. Confirmar: "Huella registrada exitosamente"

---

**Pantalla de captura**:
```
┌────────────────────────────────────────┐
│  REGISTRO DE HUELLA                    │
├────────────────────────────────────────┤
│  Usuario: Juan Pérez (1234567890)     │
│  Dedo: [▼ Índice derecho            ] │
│                                        │
│  Instrucciones:                        │
│  1. Coloca tu dedo en el sensor        │
│  2. Mantén quieto hasta el "bip"       │
│  3. Repite 3 veces                     │
│                                        │
│  Muestras capturadas:                  │
│  [✓ 1/3]  [✓ 2/3]  [ ] 3/3            │
│                                        │
│  Calidad: ██████░░░░ 60%              │
│                                        │
│  [Capturar]  [Reiniciar]  [Cancelar]  │
└────────────────────────────────────────┘
```

---

### Mejores prácticas: Registro de huellas

**DO** ✅:
- Registrar índice derecho como principal
- Opcionalmente registrar índice izquierdo (redundancia)
- Verificar calidad ≥70% antes de guardar
- Limpiar sensor entre usuarios
- Pedir al usuario que lave/seque las manos si están sucias

**DON'T** ❌:
- Registrar con huella de baja calidad (<60%)
- Usar dedos lastimados o con cortes
- Registrar múltiples veces el mismo dedo (crea duplicados)

---

### Registrar rostro

**Paso a paso**:
1. Conectar cámara web (720p mínimo)
2. Verificar iluminación adecuada
3. Ir a "Registro Biométrico" → "Nuevo Rostro"
4. Buscar usuario
5. Posicionar usuario frente a cámara (30-50 cm)
6. Capturar 3-5 fotos desde diferentes ángulos leves
7. Sistema genera vectores faciales y guarda
8. Confirmar: "Rostro registrado exitosamente"

---

**Pantalla de captura**:
```
┌────────────────────────────────────────┐
│  REGISTRO DE ROSTRO                    │
├────────────────────────────────────────┤
│  Usuario: Ana López (9876543210)      │
│                                        │
│  ┌──────────────────────────────────┐ │
│  │                                  │ │
│  │       📷   [VISTA PREVIA]        │ │
│  │           (Cara detectada ✓)     │ │
│  │                                  │ │
│  └──────────────────────────────────┘ │
│                                        │
│  Capturas: [✓][✓][✓][ ][ ]            │
│  Requerido: mínimo 3                   │
│                                        │
│  [Capturar foto]  [Reiniciar]  [OK]   │
└────────────────────────────────────────┘
```

---

### Mejores prácticas: Registro de rostro

**DO** ✅:
- Buena iluminación (luz frontal, no contraluz)
- Usuario sin gorra, lentes oscuros
- Expresión neutra
- Capturar ligeras variaciones de ángulo
- Fondo neutro

**DON'T** ❌:
- Fotos borrosas
- Sombras fuertes
- Usuario muy lejos o muy cerca
- Múltiples caras en encuadre

---

### Re-registro (actualizar biometría)

**Cuándo es necesario**:
- Huella no reconoce consistentemente
- Usuario reporta cambios físicos (quemaduras, cicatrices)
- Cambio significativo en apariencia (rostro)

**Proceso**:
1. Buscar usuario
2. Ir a "Registro Biométrico" → "Actualizar Huella/Rostro"
3. Seguir mismo proceso de registro inicial
4. Sistema reemplaza plantilla anterior

---

### Eliminar registro biométrico

**Cuándo**:
- Usuario se gradúa/deja institución (antes de desactivar cuenta)
- Por solicitud expresa del usuario (privacidad)

**Proceso**:
1. Buscar usuario
2. Ir a "Registro Biométrico" → "Eliminar Datos Biométricos"
3. Confirmar acción (irreversible)
4. Sistema elimina huellas y rostros de DB

**Advertencia**: Una vez eliminado, el usuario NO podrá acceder hasta que se registre nuevamente.

---

## 4. DESCANSO (10 minutos)

☕ Estira las piernas  
💧 Toma agua  
🚻 Usa el baño  

**Continuamos en 10 minutos con Gestión de Pagos**

---

## 5. Gestión de Pagos (Módulo Admin)

### Diferencia: Caja vs Admin

**Módulo Caja**:
- Registro rutinario de pagos
- Solo vigencias futuras (desde hoy)
- Métodos de pago estándar

**Módulo Admin**:
- **Ajustes excepcionales**
- Puede editar vigencias (backdating si es necesario)
- Puede marcar pagos como rechazados/anulados
- Puede generar reportes complejos

---

### Ajustar vigencia de pago

**Escenario**: Usuario pagó pero por error no se registró a tiempo

**Solución**:
1. Buscar pago en historial
2. Click "Editar"
3. Modificar fecha de inicio (ej. hace 1 semana)
4. Sistema recalcula fin automáticamente
5. Guardar
6. Queda registro en log de auditoría

**Precaución**: Solo hacer esto con autorización y documentación de respaldo (comprobante físico).

---

### Marcar pago como rechazado

**Escenario**: Pago con tarjeta que fue revertido por el banco

**Solución**:
1. Buscar pago
2. Click "Marcar como rechazado"
3. Ingresar motivo: "Reversión bancaria"
4. Confirmar
5. Pago ya NO cuenta para vigencia
6. Usuario debe pagar nuevamente

---

### Reporte de conciliación

**Propósito**: Comparar pagos en sistema vs extractos bancarios

**Pasos**:
1. Ir a "Reportes" → "Conciliación Bancaria"
2. Filtros:
   - Fecha inicio
   - Fecha fin
   - Método de pago (TRANSFERENCIA, PSE, etc.)
3. Exportar a CSV
4. Comparar columna "Referencia" con extracto
5. Marcar como conciliados los que coinciden
6. Investigar discrepancias

---

## Continuación en PARTE 2...

**En la segunda parte veremos**:
- Reportes avanzados (30 min)
- Auditoría y logs (20 min)
- Configuración del sistema (15 min)
- Troubleshooting avanzado (15 min)
- Ejercicio integrador (10 min)

---

**DESCANSO DE 5 MINUTOS** antes de continuar con Parte 2
