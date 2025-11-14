# V2 — Registro de cambios (08-10-2025 + Actualización Enero 2025)

Este documento detalla TODOS los cambios realizados en el proyecto, con especificación exacta de archivos, objetos creados y modificaciones.

---

## 🎨 ACTUALIZACIÓN ENERO 2025: Rediseño Premium UI de Administración de Usuarios

### Resumen Ejecutivo
Se implementó un **rediseño premium de nivel corporativo** para toda la sección de Administración de Usuarios, inspirado en **Microsoft Fluent Design** y **Material Design 3**. El resultado es una experiencia visual sofisticada, profesional y altamente intuitiva, comparable a dashboards SaaS empresariales modernos.

### Archivos Modificados/Creados

#### Desktop - Vista Principal Rediseñada
- **`edufeed-desktop/src/main/java/co/cellano/edufeed/desktop/admin/UserManagementViewV2.java`**
  - **TOTAL REWRITE**: Implementación de métodos premium
  - Nuevos métodos:
    - `createPremiumHeader()`: Header hero con gradiente corporativo (#667eea → #764ba2)
    - `createPremiumFiltersCard()`: Card glassmorphic con inputs modernos
    - `createPremiumTableCard()`: Tabla elevada con columnas iconizadas
    - `createPremiumActionsBar()`: Botones color-coded con glow effects
    - `createButtonSeparator()`: Helper para separadores visuales
  - Nuevo campo: `statsCount` (Label) para contador hero en header
  - Efectos visuales: DropShadow (radios 12-24px), gradientes lineales, border-radius 10-16px
  - Color-coding de botones:
    - Verde (#10b981→#059669): Crear
    - Azul (#3b82f6→#2563eb): Editar
    - Naranja (#f59e0b→#d97706): Toggle
    - Rojo (#ef4444→#dc2626): Eliminar
    - Púrpura (#8b5cf6→#7c3aed): Biometría
    - Índigo (#6366f1→#4f46e5): Registro rápido
  - Columnas de tabla con emojis: 📄 Documento, 👤 Nombre, 🏷 Tipo, 📧 Email, 📱 Teléfono, ✅ Estado

#### Desktop - Integración Actualizada
- **`edufeed-desktop/src/main/java/co/cellano/edufeed/desktop/admin/UserManagementController.java`**
  - Ya integrado con UserManagementViewV2 (sin cambios adicionales requeridos)
  
- **`edufeed-desktop/src/main/java/co/cellano/edufeed/desktop/admin/UserManagementModule.java`**
  - Ya referenciando UserManagementViewV2 (fix "no cambio nada" completado previamente)

#### Recursos CSS Nuevos
- **`edufeed-desktop/src/main/resources/css/premium-v2.css`** [NUEVO]
  - Estilos hover con microinteracciones
  - Estados premium para inputs (.premium-input:focused)
  - Tabla con hover y alternancia de filas
  - Badges biométricos (.bio-chip-activo/inactivo/pendiente/error)
  - Scrollbars modernos
  - Tooltips con glassmorphism
  - Pagination premium
  - ComboBox y Checkbox modernizados
  - Context menu con sombras
  - Responsive breakpoints (<1280px, <1024px)

#### Documentación
- **`docs/ADMIN_UI_V2_PREMIUM.md`** [NUEVO]
  - Guía completa del rediseño con:
    - Descripción detallada de cada sección (Header, Filtros, Tabla, Acciones)
    - Paleta de colores corporativa completa
    - Sistema de espaciado y border-radius
    - Jerarquía tipográfica
    - Efectos visuales (glassmorphism, gradientes, microinteracciones)
    - Guía de uso para desarrolladores y diseñadores
    - Ventajas UX y estéticas
    - Próximos pasos recomendados

### Características Implementadas

#### 1. Header Hero Gradiente
- Gradiente corporativo dinámico (135deg, #667eea → #764ba2)
- DropShadow: radius 24px, offsetY 4px, color rgba(0,0,0,0.2)
- Título hero: 28px bold, white, con text-shadow
- Stats card glassmorphic incrustado: rgba(255,255,255,0.15)
- Contador hero: 32px bold white
- Padding: 40px vertical, 32px horizontal

#### 2. Filtros Glassmorphic
- Efecto glassmorphism con borde sutil (derive 10%)
- DropShadow: radius 20px, offsetY 6px, color rgba(0,0,0,0.12)
- Inputs premium: border-radius 10px, padding 10x14px
- Labels: font-weight 500, color secundario
- Botón Buscar: gradiente azul con glow (rgba(102,126,234,0.4))
- Botón Limpiar: estilo ghost con borde transparente

#### 3. Tabla Elevada
- Header de tabla con fondo derivado -3%
- Columnas con iconos semánticos (📄📧📱👤🏷✅)
- DropShadow: radius 16px, offsetY 4px, color rgba(0,0,0,0.1)
- Policy: CONSTRAINED_RESIZE_POLICY
- Placeholder amigable: "🔍 No hay usuarios para mostrar"
- Paginación con background derivado -2%, border-radius inferior 16px

#### 4. Acciones Color-Coded
- 6 botones principales con gradientes únicos
- Glow effects individuales (8px spread, colores específicos)
- Padding: 12px vertical, 20px horizontal
- Font-weight: 600, font-size: 13px
- SplitMenuButton para registro rápido (huella, face, voz)
- Status bar biométrico con chips y emojis
- Separadores visuales (1px x 28px)

### Ventajas del Rediseño

#### UX (Experiencia de Usuario)
- ✅ Jerarquía visual clara con header hero prominente
- ✅ Color-coding intuitivo para acciones (verde=crear, rojo=eliminar)
- ✅ Iconos semánticos que reducen carga cognitiva
- ✅ Feedback visual inmediato con glow effects
- ✅ Estado biométrico visible con chips emoji

#### Estética Profesional
- ✅ Diseño corporativo comparable a SaaS empresarial
- ✅ Inspiración en líderes: Fluent Design, Material Design 3
- ✅ Consistencia visual con paleta coherente
- ✅ Detalles refinados: sombras sutiles, bordes redondeados, gradientes suaves

#### Mantenibilidad
- ✅ Métodos premium dedicados con nombres claros
- ✅ Separación de lógica presentacional vs. negocio
- ✅ JavaDoc actualizado con características premium
- ✅ CSS modular (premium-v2.css) para extensiones futuras

### Estado de Implementación
- ✅ Compilación exitosa (0 errores)
- ✅ Ejecución validada (aplicación lanza correctamente)
- ⏳ Testing visual pendiente con diferentes temas
- ⏳ Testing funcional de botones premium pendiente
- ⏳ Validación de chips biométricos dinámicos pendiente

### Próximos Pasos Recomendados
1. Implementar hover effects programáticos (cambio de sombra on hover)
2. Agregar animaciones de transición con AnimationUtils.fadeIn()
3. Loading skeletons durante carga de datos
4. Responsive breakpoints refinados para <1280px
5. Extracción de colores hardcoded a variables CSS
6. Testing E2E con usuario real

---

## 1) Estructura de reportes y consultas SQL

### 1.1 Nuevo: `scripts/reportes/EduFeed_consultas.sql`
Se creó un catálogo de consultas clave, organizado por secciones, listo para uso con parámetros tipo `:desde`, `:hasta`, `:usuario_id`, `:modalidad`, etc.

Secciones principales y propósito:
- Operación diaria:
  - Búsqueda de usuarios por texto/tipo/activo.
  - Derecho vigente ahora (para control de acceso).
  - Paquete disponible con días restantes.
  - Insert de acceso (aprobado/denegado) vinculando derecho actual.
  - Asistencias del día.
- Asistencia/Inasistencias:
  - Asistencias por rango y modalidad.
  - Inasistencias mensualistas (hoy y por rango usando `calendario_servicio`).
  - Inasistencias de prepago (versión simple) y nota de versión precisa.
  - Asistencias por tipo de derecho.
- Pagos e ingresos:
  - Ingresos por periodo (day|month), tipo y método.
  - Ingresos por usuario y mes.
  - Mensualidades por vencer en 7 días.
- Conciliación con caja:
  - Transacciones aprobadas sin pago asociado.
  - Pagos aprobados con referencia sin transacción.
  - Descuadres de montos por referencia.
- Biometría:
  - Cobertura de plantillas por modalidad.
  - Usuarios sin plantilla activa por modalidad.
- Auditoría:
  - Cambios por rango y entidad.
  - Últimos cambios por actor.
- KPIs y analítica:
  - Tasa de asistencia diaria (mensualistas) sobre `calendario_servicio`.
  - Distribución horaria de accesos.
  - Motivos de denegación más frecuentes.
- Gestión/calidad de datos:
  - Duplicidades de plantillas.
  - Documentos atípicos.
  - Pagos aprobados sin derecho.
  - Accesos aprobados sin derecho vinculado.

Optimizaciones realizadas sobre este archivo (para uso de índices/sargabilidad):
- Se reemplazaron comparaciones con `a.fecha_hora::date = ...` por rangos de tiempo, en 4 consultas específicas:
  1) "Asistencias del día" ahora usa:
     - `a.fecha_hora >= date_trunc('day', now()) AND a.fecha_hora < date_trunc('day', now()) + INTERVAL '1 day'`.
  2) "Inasistencias mensualistas hoy" — subconsulta NOT EXISTS ahora filtra por rango del día actual con `date_trunc`.
  3) "Inasistencias mensualistas por rango" — el NOT EXISTS usa `a.fecha_hora >= f.fecha::timestamptz AND a.fecha_hora < (f.fecha::timestamptz + INTERVAL '1 day')`.
  4) "Inasistencias de prepago por rango" — mismo patrón de rango para el NOT EXISTS.

### 1.2 Nuevo: `scripts/reportes/EduFeed_vistas.sql`
Se añadieron objetos de soporte a reportes frecuentes:
- Vista: `vw_derechos_vigentes_hoy`
  - Consulta de derechos activos hoy (vigencias y activo=true).
- Vista materializada: `mv_ingresos_diarios`
  - Agregado de ingresos por día, tipo de pago y método.
  - Creada con `WITH NO DATA` (requiere `REFRESH` para poblar).
  - Índices creados:
    - `uq_mv_ingresos_diarios (dia, tipo_pago, metodo)` (único) para permitir `REFRESH CONCURRENTLY`.
    - `idx_mv_ingresos_diarios_dia (dia DESC)` para lectura ordenada.
- Vista: `vw_asistencias_por_dia`
  - Asistencias por día y tipo de derecho (incluye 'SIN_DERECHO' si no hay vínculo).

Notas de operación incluidas en el archivo:
- REFRESH inicial: `REFRESH MATERIALIZED VIEW mv_ingresos_diarios;`
- REFRESH no bloqueante: `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_ingresos_diarios;`

## 2) Índices compuestos para performance

Se añadieron índices compuestos en ambos esquemas (Flyway y SQL portátil):

Archivos afectados:
- `edufeed-backend/src/main/resources/db/migration/V1__init.sql`
- `EduFeed_DB.sql`

Índices agregados:
- En `accesos`:
  - `CREATE INDEX IF NOT EXISTS idx_accesos_usuario_estado_fecha ON accesos (usuario_id, estado, fecha_hora DESC);`
  - Propósito: acelerar filtros combinados por usuario + estado + rango temporal.
- En `pagos`:
  - `CREATE INDEX IF NOT EXISTS idx_pagos_estado_creado ON pagos (estado_pago, creado_en DESC);`
  - Propósito: acelerar reportes y listados por estado de pago en un rango de tiempo.

(Se mantienen además los índices ya existentes: `idx_accesos_usuario_fecha`, `idx_pagos_usuario_creado`, `idx_derechos_usuario_activo`, `idx_plantillas_usuario`, `idx_accesos_estado_fecha`, `idx_derechos_usuario_vigencias`, `idx_pagos_tipo_creado` y únicos sobre referencias.)

## 3) Semilla de datos para QA

### 3.1 Nuevo: `scripts/seed/EduFeed_seed.sql`
Semilla realista para validar reportes y flujos end-to-end. Incluye:
- Usuarios (4): Niño, Estudiante, Docente, Personal.
- Plantillas biométricas mock (`HUELLA` y `ROSTRO`).
- Calendario de servicio: 15 días (±7 días alrededor de hoy).
- Pagos:
  - Mensualidad activa (Ana) — `APROBADO` con vigencia mes actual.
  - Paquete de 5 días (Bruno) — `APROBADO`, vigencia 30 días.
  - Diario (Carla) — `APROBADO`, vigencia hoy.
- Paquetes:
  - Registro de `paquetes_pago` para Bruno con 5 días.
- Derechos de uso:
  - MENSUAL (Ana), PAQUETE (Bruno), DIARIO (Carla).
- Accesos:
  - Ana: 2 aprobados hoy (distintas horas y modalidades).
  - Bruno: 1 aprobado ayer (para probar inasistencia hoy).
  - Carla: 1 denegado hoy (sin derecho vinculado a propósito).
- Usos de paquete:
  - Consumo de Bruno (ayer), enlazado a su acceso aprobado.
- Transacciones de caja:
  - POS/EFECTIVO (Ana) y GATEWAY/TARJETA (Bruno), ambas `APROBADO` y conciliadas.

Se usan tablas temporales (`TEMP TABLE`) para capturar IDs de inserciones y asociarlas correctamente en cascada.

## 4) Backend — Zona horaria parametrizada

Archivo modificado: `edufeed-backend/src/main/resources/application.yml`
- Se añadió `app.timezone` (por defecto: `America/Bogota`).
- `spring.datasource.hikari.connectionInitSql`: `SET TIME ZONE '${app.timezone}'` (fija TZ por sesión en PostgreSQL).
- `spring.jpa.properties.hibernate.jdbc.time_zone: UTC` (driver consistente).
- `spring.jackson.time-zone: ${app.timezone}` (serialización JSON consistente).

Archivo agregado: `edufeed-backend/src/main/java/co/cellano/edufeed/backend/config/TimeZoneConfig.java`
- Configuración Spring que fija la zona horaria por defecto de la JVM al arrancar según `app.timezone` y la registra en logs.
- Nota: El archivo tuvo ediciones manuales posteriores el mismo día (realizadas por el usuario). La función final permanece: establecer la TZ de la JVM desde configuración.

## 5) Directorios nuevos
- `scripts/reportes/`
- `scripts/seed/`

## 6) Notas de uso y operación
- Para refrescar la MV de ingresos diarios sin bloquear lecturas:
  - `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_ingresos_diarios;`
- Para poblar datos de QA:
  - Ejecutar `scripts/seed/EduFeed_seed.sql` después de tener el esquema creado.
- Para cambiar zona horaria del backend sin recompilar:
  - Variable de entorno `APP_TIMEZONE` (p.ej. `America/Bogota`, `UTC`).

## 7) Impacto
- Reportes más rápidos y consistentes (vistas y MV).
- Consultas sargables que aprovechan índices.
- Mejor experiencia de QA con semilla realista.
- Cortes diarios coherentes con la TZ configurada (DB + JVM + JSON).
