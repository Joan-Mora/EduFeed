# 📋 Criterios de Aceptación SMART - Sistema EduFeed

> **Fecha de creación**: 20 de octubre de 2025  
> **Propósito**: Definir criterios medibles, específicos y verificables para cada requisito funcional y no funcional del sistema EduFeed.

---

## 🎯 Metodología SMART

Cada criterio debe ser:
- **S**pecific (Específico): Claro y sin ambigüedades
- **M**easurable (Medible): Cuantificable con métricas concretas
- **A**chievable (Alcanzable): Realista con los recursos disponibles
- **R**elevant (Relevante): Alineado con los objetivos del proyecto
- **T**ime-bound (Limitado en tiempo): Con plazos definidos

---

## 📊 RF-01: Registro de Usuarios

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-01.1 | Formulario completo de registro | Campos obligatorios validados | 100% de campos con validación client-side y server-side |
| RF-01.2 | Validación de documento único | Detección de duplicados | 0 duplicados permitidos en BD |
| RF-01.3 | Validación de formato email | Regex validación | 100% emails con formato válido (RFC 5322) |
| RF-01.4 | Validación de teléfono | Regex validación | 100% teléfonos con formato +57\d{10} |
| RF-01.5 | Tiempo de respuesta registro | Latencia | ≤1 segundo (p95) |
| RF-01.6 | Registro persistente | Integridad de datos | 100% registros correctamente almacenados en BD |

### Casos de Prueba

**Caso 1**: Registro exitoso
- **Dado** un usuario nuevo con datos válidos
- **Cuando** se envía el formulario de registro
- **Entonces** el sistema crea el usuario, retorna código 201, y almacena correctamente en BD

**Caso 2**: Documento duplicado
- **Dado** un documento que ya existe en el sistema
- **Cuando** se intenta registrar con ese documento
- **Entonces** el sistema retorna 409 Conflict con mensaje "Documento ya registrado"

**Caso 3**: Email inválido
- **Dado** un email sin formato válido (ej: "usuario@.com")
- **Cuando** se envía el formulario
- **Entonces** el sistema retorna 400 Bad Request con detalle del error

---

## 🔐 RF-02: Validación Biométrica

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-02.1 | Tasa de acierto (huella) | FAR (False Accept Rate) | ≤0.01% |
| RF-02.2 | Tasa de rechazo (huella) | FRR (False Reject Rate) | ≤5% |
| RF-02.3 | Tasa de acierto (rostro) | Precisión | ≥90% en condiciones controladas |
| RF-02.4 | Tasa de acierto (voz) | Precisión | ≥85% en ambiente con ruido moderado |
| RF-02.5 | Latencia de verificación | Tiempo de respuesta | <2 segundos (p95) |
| RF-02.6 | Captura de plantilla | Tiempo máximo | ≤5 segundos por modalidad |
| RF-02.7 | Almacenamiento seguro | Cifrado | 100% plantillas cifradas con AES-256 |
| RF-02.8 | Verificación 1:1 (identificación) | Latencia | <1 segundo |
| RF-02.9 | Verificación 1:N (búsqueda) | Latencia | <3 segundos para 10,000 usuarios |

### Casos de Prueba

**Caso 1**: Enrolamiento de huella exitoso
- **Dado** un usuario sin plantilla biométrica
- **Cuando** se captura la huella en el dispositivo
- **Entonces** el sistema almacena la plantilla cifrada y retorna 201

**Caso 2**: Verificación exitosa de rostro
- **Dado** un usuario con plantilla facial enrolada
- **Cuando** se captura el rostro y se verifica
- **Entonces** el sistema retorna match=true en <2s

**Caso 3**: Rechazo por no coincidencia
- **Dado** una plantilla que no coincide con ninguna del sistema
- **Cuando** se intenta verificar
- **Entonces** el sistema retorna match=false sin error

---

## 🎫 RF-03: Control de Derecho Adquirido

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-03.1 | Validación en tiempo real | Latencia consulta | <500ms (p95) |
| RF-03.2 | Precisión de vigencias | Exactitud temporal | 100% derechos con vigencias correctas |
| RF-03.3 | Consumo de paquetes | Decremento correcto | 100% decrementos registrados atómicamente |
| RF-03.4 | Detección de expiración | Validación diaria | 100% derechos expirados detectados al día siguiente |
| RF-03.5 | Concurrencia | Transacciones simultáneas | Soporte para ≥50 validaciones/segundo sin pérdida de datos |

### Casos de Prueba

**Caso 1**: Acceso con derecho vigente DIARIO
- **Dado** un usuario con pago DIARIO válido para hoy
- **Cuando** se verifica el acceso a las 12:00
- **Entonces** el sistema retorna permitido=true

**Caso 2**: Acceso con derecho expirado
- **Dado** un usuario con derecho vencido ayer
- **Cuando** se verifica el acceso
- **Entonces** el sistema retorna permitido=false, motivo="SIN_DERECHO"

**Caso 3**: Consumo de paquete
- **Dado** un usuario con paquete de 5 días, 3 consumidos
- **Cuando** se aprueba el acceso
- **Entonces** el sistema decrementa a 2 días restantes

---

## 🏦 RF-04: Orientación a Caja

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-04.1 | Visualización de mensaje | Tiempo de respuesta UI | <200ms |
| RF-04.2 | Generación de código QR | Tiempo de generación | <500ms |
| RF-04.3 | Legibilidad de instrucciones | Tamaño de fuente | ≥16pt, contraste AAA (WCAG 2.1) |
| RF-04.4 | Referencia única | Unicidad | 100% referencias únicas (UUID o alfanumérico 6 dígitos) |

### Casos de Prueba

**Caso 1**: Usuario denegado sin derecho
- **Dado** un usuario sin derecho vigente
- **Cuando** se intenta el acceso
- **Entonces** se muestra pantalla de orientación con QR, referencia, e instrucciones claras

---

## 💰 RF-05: Registro de Pagos

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-05.1 | Validación de montos | Monto mínimo | >0, máximo 1,000,000 COP |
| RF-05.2 | Generación automática de derechos | Tiempo de procesamiento | <1 segundo tras aprobar pago |
| RF-05.3 | Integridad transaccional | Atomicidad | 100% pagos aprobados generan derecho o rollback |
| RF-05.4 | Registro de método de pago | Opciones | EFECTIVO, TARJETA, TRANSFERENCIA, OTRO |
| RF-05.5 | Persistencia de recibo | Formato | PDF con datos completos, almacenado en carpeta recibos/ |

### Casos de Prueba

**Caso 1**: Pago DIARIO
- **Dado** un monto de 8,000 COP tipo DIARIO
- **Cuando** se aprueba el pago
- **Entonces** se crea DerechoUso vigente para hoy

**Caso 2**: Pago MENSUAL
- **Dado** un monto de 120,000 COP tipo MENSUAL
- **Cuando** se aprueba el pago
- **Entonces** se crea DerechoUso vigente desde hoy hasta fin de mes

**Caso 3**: Pago PAQUETE
- **Dado** un monto de 35,000 COP, 5 días
- **Cuando** se aprueba el pago
- **Entonces** se crea PaquetePago con dias_restantes=5

---

## 📈 RF-06: Reporte de Asistencia

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-06.1 | Generación de reporte | Latencia | <3 segundos para 1,000 registros |
| RF-06.2 | Filtros funcionales | Tipos de filtro | Fecha, rango, tipo de derecho, usuario |
| RF-06.3 | Exportación CSV | Formato válido | 100% archivos con encoding UTF-8, delimitador coma |
| RF-06.4 | Exportación Excel | Formato válido | 100% archivos .xlsx con estilos (header bold) |
| RF-06.5 | Exportación PDF | Diseño | Logo, tabla con datos, pie de página con fecha |
| RF-06.6 | Paginación | Rendimiento | Soporte para ≥10,000 registros sin degradación |

### Casos de Prueba

**Caso 1**: Reporte de asistencias del día
- **Dado** filtro fecha=hoy
- **Cuando** se genera el reporte
- **Entonces** se muestran todas las asistencias del día en <3s

**Caso 2**: Exportación CSV
- **Dado** un reporte generado con 500 registros
- **Cuando** se exporta a CSV
- **Entonces** se descarga archivo válido con todas las filas

---

## 👥 RF-07: Gestión de Usuarios

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-07.1 | CRUD completo | Endpoints | 100% operaciones (Create, Read, Update, Delete) funcionales |
| RF-07.2 | Soft delete | Auditoría | 0 eliminaciones físicas, 100% usuarios marcados como inactivos |
| RF-07.3 | Búsqueda por criterios | Latencia | <500ms para 10,000 usuarios |
| RF-07.4 | Paginación | Tamaño de página | Configurable (10, 25, 50, 100) |
| RF-07.5 | Validación de duplicados | Documento único | 0 duplicados en UPDATE |

### Casos de Prueba

**Caso 1**: Listar usuarios con paginación
- **Dado** 500 usuarios en BD
- **Cuando** se solicita página 2, tamaño 25
- **Entonces** se retornan usuarios 26-50

**Caso 2**: Desactivar usuario
- **Dado** un usuario activo
- **Cuando** se ejecuta DELETE /api/users/{id}
- **Entonces** el usuario queda con activo=false, no se elimina físicamente

---

## 🔗 RF-08: Integración con Caja

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-08.1 | Webhook funcional | Disponibilidad | 99.9% uptime |
| RF-08.2 | Matching automático | Precisión | ≥95% transacciones conciliadas automáticamente |
| RF-08.3 | Reintentos con backoff | Estrategia | 1s, 2s, 4s, 8s, fallo final |
| RF-08.4 | Firma de webhook | Validación | 100% requests con firma HMAC válida |
| RF-08.5 | Conciliación manual | UI | Pantalla para vincular transacciones no match |

### Casos de Prueba

**Caso 1**: Webhook con referencia válida
- **Dado** una notificación de caja con referencia_externa que coincide
- **Cuando** se recibe el webhook
- **Entonces** se crea TransaccionCaja, se vincula Pago, conciliado=true

**Caso 2**: Webhook sin match
- **Dado** una notificación sin referencia coincidente
- **Cuando** se recibe el webhook
- **Entonces** se crea TransaccionCaja, conciliado=false, disponible para revisión manual

---

## 📜 RF-09: Historial de Accesos

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-09.1 | Registro completo | Campos | 100% accesos con fecha, hora, usuario, estado, motivo, modalidad |
| RF-09.2 | Consulta con filtros | Latencia | <1 segundo para 50,000 registros con índices |
| RF-09.3 | Paginación | Rendimiento | Sin degradación hasta 100,000 registros |
| RF-09.4 | Exportación | Formatos | CSV, Excel, PDF |

### Casos de Prueba

**Caso 1**: Consultar historial de usuario
- **Dado** un usuarioId con 200 accesos históricos
- **Cuando** se consulta GET /api/access/history?userId={id}
- **Entonces** se retornan todos los accesos paginados

---

## 📊 RF-10: Reportes Administrativos

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-10.1 | Reporte de ingresos | Latencia | <5 segundos usando MV mv_ingresos_diarios |
| RF-10.2 | Reporte de asistencias | Precisión | 100% datos coinciden con tabla accesos |
| RF-10.3 | Reporte de inasistencias | Lógica | Usa calendario_servicio, detecta ausencias |
| RF-10.4 | Exportación múltiple | Formatos | CSV, Excel, PDF |
| RF-10.5 | Dashboard en tiempo real (opcional) | Actualización | Cada 30 segundos |

### Casos de Prueba

**Caso 1**: Reporte de ingresos mensual
- **Dado** filtro mes=octubre, tipo=MENSUAL
- **Cuando** se genera el reporte
- **Entonces** se muestran todos los pagos MENSUAL de octubre con totales

---

## 🔍 RF-11: Auditoría de Operaciones

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-11.1 | Captura automática | Cobertura | 100% operaciones CRUD auditadas |
| RF-11.2 | Detalle de cambios | Formato | JSON con valores anteriores y nuevos |
| RF-11.3 | Identificación de actor | Precisión | 100% registros con actor desde SecurityContext |
| RF-11.4 | Consulta de auditoría | Latencia | <2 segundos para 10,000 registros |
| RF-11.5 | Exportación | Formatos | CSV, Excel |

### Casos de Prueba

**Caso 1**: Auditoría de creación
- **Dado** un usuario nuevo creado
- **Cuando** se consulta la auditoría
- **Entonces** existe registro con acción=CREATE, entidad=Usuario, actor identificado

**Caso 2**: Auditoría de actualización
- **Dado** un usuario actualizado (cambio de email)
- **Cuando** se consulta la auditoría
- **Entonces** existe registro con acción=UPDATE, valores_anteriores y valores_nuevos en JSON

---

## 💳 RF-12: Registro y Venta de Mensualidades

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-12.1 | Cálculo automático de vigencia | Precisión | 100% mensualidades con vigencia desde 1° hasta último día del mes |
| RF-12.2 | Validación de mensualidad duplicada | Detección | 0 mensualidades duplicadas para mismo mes |
| RF-12.3 | Generación de recibo | Formato | PDF con detalles completos |
| RF-12.4 | Notificación de vencimiento | Anticipación | 3 días antes de vencer |

### Casos de Prueba

**Caso 1**: Venta de mensualidad octubre
- **Dado** fecha actual 10 de octubre
- **Cuando** se vende mensualidad
- **Entonces** vigencia es 01/10/2025 - 31/10/2025

---

## 📉 RF-13: Reporte de Inasistencias

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RF-13.1 | Detección de inasistencias | Precisión | 100% usuarios con derecho vigente pero sin acceso registrado |
| RF-13.2 | Uso de calendario | Lógica | Solo cuenta días hábiles según calendario_servicio |
| RF-13.3 | Generación de reporte | Latencia | <5 segundos para 1,000 usuarios |
| RF-13.4 | Exportación | Formatos | CSV, Excel, PDF |

### Casos de Prueba

**Caso 1**: Reporte de inasistencias semanal
- **Dado** una semana con 5 días hábiles
- **Cuando** se genera el reporte
- **Entonces** se listan todos los usuarios con derecho vigente sin acceso en esos días

---

## 🔒 RNF-01: Seguridad

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RNF-01.1 | Autenticación JWT | Estándar | 100% endpoints protegidos con JWT válido |
| RNF-01.2 | Cifrado de plantillas | Algoritmo | AES-256-GCM con clave rotable |
| RNF-01.3 | Contraseñas | Hash | BCrypt con cost factor ≥10 |
| RNF-01.4 | Auditoría de accesos | Cobertura | 100% operaciones sensibles auditadas |
| RNF-01.5 | Rotación de claves | Frecuencia | Cada 90 días |
| RNF-01.6 | HTTPS obligatorio | Producción | 100% tráfico sobre TLS 1.3 |
| RNF-01.7 | Control de acceso por roles | Granularidad | 5 roles: ADMIN, OPERADOR_CAJA, OPERADOR_ACCESO, AUDITOR, SUPERVISOR |
| RNF-01.8 | Sesiones | Timeout | JWT expira en 24h, refresh token en 7 días |

### Pruebas de Seguridad

**Prueba 1**: Acceso sin token
- **Dado** un request a endpoint protegido sin header Authorization
- **Cuando** se envía el request
- **Entonces** se retorna 401 Unauthorized

**Prueba 2**: Token expirado
- **Dado** un JWT expirado
- **Cuando** se envía request con ese token
- **Entonces** se retorna 401 con mensaje "Token expired"

**Prueba 3**: Plantilla cifrada
- **Dado** una plantilla biométrica almacenada
- **Cuando** se lee directamente de BD
- **Entonces** el campo template_data está cifrado (no legible)

---

## 🖥️ RNF-02: Compatibilidad Hardware

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RNF-02.1 | Dispositivos huella | Compatibilidad | DigitalPersona U.are.U, ZKTeco, Suprema |
| RNF-02.2 | Cámaras | Compatibilidad | USB 2.0+, resolución ≥720p |
| RNF-02.3 | Micrófonos | Compatibilidad | Frecuencia ≥16kHz |
| RNF-02.4 | Fallback a mock | Comportamiento | Si hardware no disponible, usar MockBiometricProvider sin error |
| RNF-02.5 | Documentación | Completitud | Lista de dispositivos probados, drivers requeridos |

### Pruebas de Hardware

**Prueba 1**: Captura con dispositivo real
- **Dado** un lector de huella DigitalPersona conectado
- **Cuando** se solicita captura
- **Entonces** se obtiene template válido en <5s

**Prueba 2**: Fallback a mock
- **Dado** ningún dispositivo hardware conectado
- **Cuando** se solicita captura
- **Entonces** se usa MockBiometricProvider y retorna template simulado

---

## 📈 RNF-03: Performance

### Criterios de Aceptación

| ID | Criterio | Métrica | Objetivo |
|----|----------|---------|----------|
| RNF-03.1 | Latencia /api/access/check | p95 | <2 segundos |
| RNF-03.2 | Latencia /api/payments | p95 | <1 segundo |
| RNF-03.3 | Latencia /api/reports/* | p95 | <3 segundos para 1,000 registros |
| RNF-03.4 | Throughput | Requests/segundo | ≥100 rps sin degradación |
| RNF-03.5 | Usuarios concurrentes | Capacidad | ≥500 usuarios activos simultáneos |
| RNF-03.6 | Uso de memoria JVM | Heap | ≤2GB en operación normal |
| RNF-03.7 | Queries DB | Tiempo máximo | <200ms (p95) con índices optimizados |

### Pruebas de Carga

**Prueba 1**: 500 usuarios concurrentes
- **Dado** 500 hilos simulando verificación de acceso
- **Cuando** se ejecutan durante 5 minutos
- **Entonces** latencia p95 <2s, 0 errores 5xx

---

## 🧪 Plan de Pruebas

### Tests Unitarios
- **Cobertura**: ≥80% en capa de servicio
- **Framework**: JUnit 5 + Mockito
- **Ejecución**: `mvn test`

### Tests de Integración
- **Framework**: Spring Boot Test + Testcontainers
- **Alcance**: Flujos end-to-end críticos
- **Ejecución**: `mvn verify`

### Tests de Carga
- **Herramienta**: JMeter o Gatling
- **Escenarios**: 500 usuarios concurrentes, 1000 rps
- **Duración**: 10 minutos por escenario

### Tests de Aceptación de Usuario (UAT)
- **Participantes**: Operadores de caja, acceso, administradores, auditores
- **Duración**: 2 semanas
- **Criterio de éxito**: ≥90% casos de prueba exitosos, feedback positivo

---

## ✅ Resumen de Métricas Clave

| Requisito | Métrica Crítica | Valor Objetivo |
|-----------|----------------|----------------|
| RF-02 | Tasa de acierto biométrica (huella) | ≥95%, FAR ≤0.01% |
| RF-02 | Latencia verificación biométrica | <2s (p95) |
| RF-03 | Latencia validación de derecho | <500ms (p95) |
| RF-05 | Generación automática de derechos | <1s tras aprobar pago |
| RF-06 | Generación reporte asistencia | <3s para 1,000 registros |
| RF-08 | Precisión conciliación automática | ≥95% |
| RNF-01 | Cobertura auditoría | 100% operaciones CRUD |
| RNF-03 | Usuarios concurrentes | ≥500 sin degradación |
| RNF-03 | Throughput | ≥100 rps |

---

## 📝 Notas Finales

- Este documento debe revisarse y actualizarse tras la reunión con el cliente
- Los umbrales de performance pueden ajustarse según resultados de pruebas de carga
- Los criterios de aceptación deben firmarse por cliente y equipo antes de iniciar Fase 2
- Cada sprint debe validar al menos 3 criterios de aceptación

---

**Aprobaciones:**

| Rol | Nombre | Firma | Fecha |
|-----|--------|-------|-------|
| Cliente | | | |
| Tech Lead | | | |
| QA Engineer | | | |

