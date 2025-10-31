# Manual de Usuario - Sistema EduFeed

**Versión**: 2.0  
**Fecha**: 31 de octubre de 2025  
**Audiencia**: Administradores, Cajeros y Operadores de Acceso

---

## Índice

1. [Introducción](#introducción)
2. [Acceso al sistema](#acceso-al-sistema)
3. [Módulo de Administración](#módulo-de-administración)
4. [Módulo de Caja](#módulo-de-caja)
5. [Módulo de Acceso](#módulo-de-acceso)
6. [Reportes](#reportes)
7. [Preguntas frecuentes](#preguntas-frecuentes)
8. [Glosario](#glosario)

---

## Introducción

### ¿Qué es EduFeed?

EduFeed es un sistema integral para la gestión del restaurante escolar que permite:

- ✅ Registro y gestión de usuarios (estudiantes, docentes)
- ✅ Validación biométrica de acceso (huella, rostro, voz)
- ✅ Control de derechos adquiridos por pago (diario, mensual, paquetes)
- ✅ Registro de transacciones en caja
- ✅ Generación de reportes de asistencia y pagos
- ✅ Auditoría completa de operaciones

### Roles del sistema

| Rol | Descripción | Módulos accesibles |
|-----|-------------|-------------------|
| **Administrador** | Gestiona usuarios, configura sistema, visualiza todos los reportes | Todos |
| **Cajero** | Registra pagos, emite recibos, consulta estado de cuenta | Caja, Reportes de caja |
| **Operador de Acceso** | Valida ingreso al restaurante mediante biometría | Acceso, Reportes de asistencia |

### Arquitectura del sistema

```
┌─────────────────┐         ┌─────────────────┐
│  Backend API    │◀────────│  Aplicación     │
│  (Spring Boot)  │         │  Escritorio     │
│  Puerto: 8080   │         │  (JavaFX)       │
└────────┬────────┘         └─────────────────┘
         │
         ▼
┌─────────────────┐
│   PostgreSQL    │
│   Base de Datos │
└─────────────────┘
```

---

## Acceso al sistema

### 1. Iniciar sesión

**Aplicación de escritorio**:

1. Abrir **EduFeed Desktop** desde el acceso directo
2. En la pantalla de inicio, ingresar:
   - **Usuario**: Tu nombre de usuario asignado (ej. `admin`, `cajero01`, `operador01`)
   - **Contraseña**: Tu contraseña personal

3. Hacer clic en **"Iniciar sesión"**
4. El sistema validará tus credenciales y mostrará el menú principal según tu rol

**Notas de seguridad**:
- ⚠️ No compartir tu contraseña con nadie
- ⚠️ Cerrar sesión al terminar tu jornada
- ⚠️ La sesión caduca automáticamente después de 30 minutos de inactividad

### 2. Menú principal

Después del login, verás el menú principal con opciones según tu rol:

**Administrador** ve:
- 👥 Gestión de Usuarios
- 💳 Gestión de Pagos
- 🚪 Validación de Acceso
- 📊 Reportes

**Cajero** ve:
- 💳 Registro de Pagos
- 📄 Consultar Estado de Cuenta
- 📊 Reportes de Caja

**Operador de Acceso** ve:
- 🚪 Validación de Acceso
- 📊 Reportes de Asistencia

---

## Módulo de Administración

**Rol requerido**: Administrador

### 3.1 Gestión de Usuarios

#### Registrar nuevo usuario

1. En el menú principal, hacer clic en **"Gestión de Usuarios"**
2. Hacer clic en el botón **"+ Nuevo Usuario"**
3. Completar el formulario:

| Campo | Descripción | Ejemplo | Obligatorio |
|-------|-------------|---------|-------------|
| **Tipo de Documento** | CC, TI, CE, Pasaporte | CC | Sí |
| **Número de Documento** | Identificación única | 1234567890 | Sí |
| **Nombres** | Nombre(s) completo(s) | Juan Carlos | Sí |
| **Apellidos** | Apellido(s) completo(s) | Pérez Gómez | Sí |
| **Fecha de Nacimiento** | Formato DD/MM/AAAA | 15/03/2010 | Sí |
| **Tipo de Usuario** | Niño, Estudiante, Docente | Estudiante | Sí |
| **Grado** | Si es estudiante | 8° | No |
| **Email** | Correo electrónico | jperez@colegio.edu.co | No |
| **Teléfono** | Número de contacto | 3001234567 | No |
| **Dirección** | Dirección de residencia | Calle 123 #45-67 | No |
| **Acudiente** | Nombre del acudiente | María Gómez | No (requerido si es Niño) |

4. Hacer clic en **"Capturar Biometría"**

#### Captura biométrica

El sistema soporta tres modalidades de biometría:

**A. Captura de huella dactilar**

1. Conectar el lector de huella USB
2. Seleccionar **"Huella"** en el selector de modalidad
3. Hacer clic en **"Iniciar Captura"**
4. Indicar al usuario que coloque el dedo índice en el lector
5. El sistema capturará automáticamente 3 muestras
6. Hacer clic en **"Confirmar"** cuando veas ✅ "Captura exitosa"

**B. Captura de rostro**

1. Conectar la cámara web
2. Seleccionar **"Rostro"** en el selector de modalidad
3. Hacer clic en **"Iniciar Captura"**
4. La vista previa mostrará el rostro detectado con un recuadro verde
5. Ajustar parámetros si es necesario:
   - **Fuente**: Seleccionar cámara si hay varias
   - **Escala**: 1.1 (detección más sensible) a 1.5 (menos sensible)
   - **Vecinos mínimos**: 3-6 (filtro de falsos positivos)
6. Pedir al usuario que mire a la cámara con expresión neutral
7. Hacer clic en **"Capturar"** cuando el rostro esté centrado
8. El sistema guardará la imagen y los vectores faciales

**C. Captura de voz**

1. Conectar el micrófono
2. Seleccionar **"Voz"** en el selector de modalidad
3. Hacer clic en **"Iniciar Captura"**
4. La vista de forma de onda mostrará el audio en tiempo real
5. Pedir al usuario que lea la frase mostrada en pantalla:
   - Ejemplo: *"Mi nombre es Juan Carlos Pérez"*
6. Hacer clic en **"Detener"** después de 3-5 segundos
7. Reproducir el audio capturado para verificar calidad
8. Hacer clic en **"Confirmar"** si el audio es claro

**Recomendaciones**:
- ✅ Capturar al menos 2 modalidades (ej. huella + rostro)
- ✅ Asegurar buena iluminación para captura de rostro
- ✅ Usar micrófono de calidad para voz
- ⚠️ No forzar el dedo en el lector (puede causar rechazo)

5. Revisar datos ingresados
6. Hacer clic en **"Guardar Usuario"**
7. El sistema asigna un ID único y muestra confirmación

#### Editar usuario existente

1. En **"Gestión de Usuarios"**, buscar por nombre o documento
2. Hacer doble clic en el usuario en la tabla
3. Modificar campos necesarios
4. Si necesitas recapturar biometría:
   - Hacer clic en **"Recapturar Biometría"**
   - Seleccionar modalidad (huella/rostro/voz)
   - Seguir proceso de captura
5. Hacer clic en **"Actualizar"**

#### Eliminar usuario

1. Buscar el usuario en la lista
2. Hacer clic derecho → **"Eliminar"**
3. Confirmar la acción (⚠️ No reversible)

**Nota**: Solo usuarios sin pagos pendientes o accesos recientes pueden eliminarse.

### 3.2 Gestión de Operadores

Los operadores son usuarios del sistema (administradores, cajeros, operadores de acceso).

#### Crear operador

1. Ir a **"Gestión de Operadores"**
2. Hacer clic en **"+ Nuevo Operador"**
3. Completar formulario:
   - **Nombre de Usuario**: `operador01` (único, sin espacios)
   - **Contraseña**: Mínimo 8 caracteres, una mayúscula, un número
   - **Rol**: Administrador, Cajero o Operador de Acceso
   - **Nombre Completo**: Nombre y apellido
   - **Email**: Para notificaciones
4. Hacer clic en **"Crear"**

#### Cambiar contraseña de operador

1. En la lista de operadores, seleccionar el usuario
2. Hacer clic en **"Cambiar Contraseña"**
3. Ingresar nueva contraseña (2 veces)
4. Hacer clic en **"Actualizar"**

**Auto-servicio**: Los operadores pueden cambiar su propia contraseña desde **Menú → Mi Perfil → Cambiar Contraseña**.

---

## Módulo de Caja

**Roles permitidos**: Administrador, Cajero

### 4.1 Registrar pago

#### Flujo completo de pago

1. El usuario (estudiante/docente) se acerca a caja
2. Cajero abre **"Registro de Pagos"**
3. Buscar usuario por:
   - **Documento**: Ingresar número y presionar Enter
   - **Nombre**: Escribir nombre/apellido (búsqueda parcial)
4. Sistema muestra datos del usuario y estado de cuenta actual

5. Seleccionar **Tipo de Pago**:

| Tipo | Descripción | Duración | Precio base |
|------|-------------|----------|-------------|
| **Diario** | Un solo acceso el día de la compra | 1 día | $5.000 |
| **Mensual** | Acceso todos los días hábiles del mes | 30 días | $80.000 |
| **Paquete** | N días a elección (ej. 10 días) | Configurable | $4.500/día |

6. Ingresar detalles:
   - **Monto**: Se auto-completa según tipo (editable)
   - **Método de Pago**: Efectivo, Tarjeta, Transferencia
   - **Observaciones**: Opcional (ej. "Descuento hermano")

7. Hacer clic en **"Procesar Pago"**

8. Sistema genera recibo con número de transacción

9. Imprimir recibo (Ctrl+P) o enviar por email

### 4.2 Consultar estado de cuenta

1. Ir a **"Consultar Estado de Cuenta"**
2. Buscar usuario (documento o nombre)
3. Sistema muestra:
   - **Derechos activos**: Tipo, fecha inicio/fin, días restantes
   - **Historial de pagos**: Últimos 12 meses
   - **Historial de accesos**: Últimas 30 asistencias
   - **Balance**: Días prepagados no usados

### 4.3 Anular pago

**Rol requerido**: Administrador (con aprobación)

1. Ir a **"Gestión de Pagos"** → **"Buscar Transacción"**
2. Ingresar número de recibo o buscar por usuario/fecha
3. Hacer clic en **"Anular"**
4. Ingresar motivo (obligatorio): "Error de digitación", "Duplicado", etc.
5. Ingresar contraseña de administrador para confirmar
6. Sistema marca pago como anulado y revierte derechos

**Nota**: Solo se pueden anular pagos de los últimos 7 días.

---

## Módulo de Acceso

**Roles permitidos**: Administrador, Operador de Acceso

### 5.1 Validar ingreso al restaurante

Este es el flujo principal del sistema, usado cientos de veces al día.

#### Proceso completo

1. Abrir **"Validación de Acceso"**
2. La pantalla muestra vista previa de biometría activa (cámara/lector/micrófono)

3. **Usuario se acerca al punto de acceso**

4. **Seleccionar modalidad de verificación**:
   - Por defecto: Huella (más rápido)
   - Alternativa: Rostro (si falla huella o usuario no tiene huella)
   - Alternativa: Voz (en casos especiales)

5. **Capturar biometría en vivo**:
   - **Huella**: Usuario coloca dedo en lector, sistema busca coincidencia
   - **Rostro**: Usuario mira a cámara, sistema detecta y compara
   - **Voz**: Usuario dice frase de desafío

6. **Sistema valida derecho adquirido**

7. **Resultado**:

   **Caso 1: Acceso concedido** ✅
   - Pantalla verde
   - Sonido de confirmación
   - Registro en BD con timestamp

   **Caso 2: Acceso denegado** ❌
   - Pantalla roja
   - Sonido de rechazo
   - Operador orienta al usuario a caja

   **Caso 3: Usuario no reconocido** ⚠️
   - Opciones: intentar otra modalidad, búsqueda manual o registrar

### 5.2 Búsqueda manual (fallback)

Si la biometría falla repetidamente:

1. Hacer clic en **"Búsqueda Manual"**
2. Ingresar documento del usuario
3. Sistema muestra foto y datos
4. Verificar identidad visualmente
5. Operador hace clic en **"Conceder Acceso"** o **"Denegar"**
6. Sistema registra acceso manual con nota

**Nota**: Los accesos manuales se reportan para auditoría.

### 5.3 Ajustar sensibilidad de detección facial

Si la cámara no detecta rostros o detecta falsos positivos:

1. En pantalla de validación, hacer clic en **"⚙️ Configuración"**
2. Ajustar parámetros en tiempo real:
   - **Escala**: 1.1 (más sensible) - 1.5 (menos sensible)
   - **Vecinos mínimos**: 3 (menos filtro) - 6 (más filtro)
   - **Tamaño mínimo**: 30×30 px (detecta rostros pequeños/lejanos)
3. Hacer clic en **"Aplicar"**
4. Probar con usuario real

**Valores recomendados**:
- Iluminación buena: Escala 1.2, Vecinos 4
- Iluminación baja: Escala 1.3, Vecinos 3
- Muchos falsos positivos: Escala 1.2, Vecinos 5

---

## Reportes

**Roles permitidos**: Todos (con restricciones según rol)

### 6.1 Reporte de asistencia diaria

**Propósito**: Ver quiénes ingresaron hoy al restaurante.

1. Ir a **"Reportes"** → **"Asistencia Diaria"**
2. Seleccionar fecha (por defecto: hoy)
3. Hacer clic en **"Generar"**

**Exportar**: PDF, Excel, CSV

### 6.2 Reporte de inasistencias

**Propósito**: Identificar usuarios con derecho mensual que no asistieron.

1. Ir a **"Reportes"** → **"Inasistencias"**
2. Seleccionar rango de fechas
3. Filtrar por tipo de derecho (Mensualidad, Paquete)
4. Hacer clic en **"Generar"**

### 6.3 Reporte de ingresos

**Rol requerido**: Administrador, Cajero

1. Ir a **"Reportes"** → **"Ingresos"**
2. Seleccionar período (día, semana, mes, personalizado)
3. Agrupar por: Tipo de pago, Método de pago, Cajero
4. Hacer clic en **"Generar"**

### 6.4 Reporte de usuarios sin derecho

**Propósito**: Campañas de renovación de mensualidades.

1. Ir a **"Reportes"** → **"Usuarios sin Derecho"**
2. Filtrar por tipo de usuario (Estudiante, Docente)
3. Hacer clic en **"Generar"**

### 6.5 Auditoría de accesos manuales

**Rol requerido**: Administrador

**Propósito**: Revisar accesos concedidos sin biometría (seguridad).

1. Ir a **"Reportes"** → **"Auditoría"** → **"Accesos Manuales"**
2. Seleccionar rango de fechas
3. Hacer clic en **"Generar"**

---

## Preguntas frecuentes

### Sobre usuarios

**P: ¿Puedo cambiar el tipo de usuario de Niño a Estudiante?**  
R: Sí, editando el usuario desde Gestión de Usuarios. El cambio no afecta pagos o biometría.

**P: ¿Qué pasa si un usuario pierde su huella (accidente)?**  
R: Recapturar otra huella (ej. pulgar en vez de índice) o usar rostro/voz como modalidad principal.

### Sobre pagos

**P: ¿Se puede pagar una mensualidad anticipada (ej. diciembre en noviembre)?**  
R: Sí, seleccionando las fechas de inicio/fin manualmente al registrar el pago.

**P: ¿Qué pasa si un usuario paga dos veces el mismo mes por error?**  
R: Contactar al administrador para anular el pago duplicado o aplicar saldo a favor.

### Sobre acceso

**P: ¿Qué tan rápido es el reconocimiento?**  
R: Huella: < 2 segundos. Rostro: < 3 segundos. Voz: < 5 segundos.

**P: ¿Funciona el reconocimiento facial con mascarilla?**  
R: No. Usar huella o voz en ese caso.

### Soporte técnico

**P: ¿A quién contacto si tengo un problema técnico?**  
R: Email: soporte@edufeed.com | Teléfono: +57 (1) 234-5678

**P: ¿Dónde reporto un bug?**  
R: GitHub Issues: https://github.com/Joan-Mora/EduFeed/issues

---

## Glosario

| Término | Definición |
|---------|------------|
| **Biometría** | Medida de características físicas únicas (huella, rostro, voz) para identificación |
| **Derecho adquirido** | Permiso de acceso al restaurante obtenido mediante pago |
| **Mensualidad** | Tipo de pago que otorga acceso ilimitado durante un mes |
| **Paquete** | Conjunto de N días prepagados (ej. 10 días) |
| **Modalidad** | Tipo de biometría utilizada (huella, rostro o voz) |
| **Operador** | Usuario del sistema (administrador, cajero, operador de acceso) |
| **Acceso manual** | Ingreso autorizado sin verificación biométrica exitosa |

---

**Última actualización**: 31 de octubre de 2025  
**Versión del sistema**: 2.0  
**Manual elaborado por**: Equipo EduFeed

Para sugerencias de mejora: docs@edufeed.com
