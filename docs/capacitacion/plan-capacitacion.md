# Plan de Capacitación - Sistema EduFeed

**Versión**: 1.0  
**Fecha**: 31 de octubre de 2025  
**Responsable**: Equipo EduFeed

---

## Índice

1. [Objetivos](#objetivos)
2. [Público objetivo](#público-objetivo)
3. [Estructura de sesiones](#estructura-de-sesiones)
4. [Material de capacitación](#material-de-capacitación)
5. [Logística](#logística)
6. [Evaluación](#evaluación)
7. [Seguimiento post-capacitación](#seguimiento-post-capacitación)

---

## Objetivos

### Generales
- Capacitar a todos los usuarios del sistema EduFeed en sus roles específicos
- Garantizar que ≥80% de participantes aprueben la evaluación
- Crear material reutilizable para capacitaciones futuras
- Establecer base de conocimiento con videos y documentación

### Específicos por rol

**Operadores de caja**:
- Dominar el proceso completo de registro de pagos
- Conocer todos los métodos de pago disponibles
- Manejar excepciones y casos especiales
- Generar reportes básicos de ingresos

**Operadores de acceso**:
- Realizar verificación de accesos con hardware biométrico
- Interpretar correctamente estados de acceso (permitido/denegado)
- Orientar usuarios sin derecho hacia caja
- Manejar problemas técnicos básicos del hardware

**Administradores y auditores**:
- Gestionar usuarios del sistema (crear, editar, desactivar)
- Configurar parámetros del sistema
- Generar y analizar reportes avanzados
- Realizar auditoría de operaciones
- Resolver problemas técnicos comunes

---

## Público objetivo

### Perfil de participantes

| Rol | Cantidad estimada | Nivel técnico | Prerequisitos |
|-----|-------------------|---------------|---------------|
| **Operadores de caja** | 5-10 | Básico | Manejo básico de Windows |
| **Operadores de acceso** | 3-5 | Básico | Ninguno específico |
| **Administradores** | 2-3 | Intermedio | Conocimientos de sistemas |
| **Auditores** | 1-2 | Intermedio | Análisis de datos |

### Competencias previas requeridas

**Todos los roles**:
- Alfabetización digital básica
- Uso de mouse y teclado
- Navegación en aplicaciones de escritorio

**Administradores** (adicional):
- Comprensión de bases de datos (conceptual)
- Lectura de reportes y métricas
- Troubleshooting básico de sistemas

---

## Estructura de sesiones

### Sesión 1: Operadores de caja (2 horas)

**Horario recomendado**: 9:00 AM - 11:00 AM

**Agenda**:

| Tiempo | Tema | Metodología |
|--------|------|-------------|
| 0:00 - 0:10 | Bienvenida e introducción al sistema | Presentación |
| 0:10 - 0:25 | Login y navegación básica | Demo + Práctica guiada |
| 0:25 - 0:45 | Registro de pagos (efectivo, tarjeta) | Demo + Ejercicio práctico |
| 0:45 - 1:00 | Métodos de pago digitales (PSE, Nequi, QR) | Demo + Ejercicio práctico |
| 1:00 - 1:10 | **DESCANSO** | — |
| 1:10 - 1:30 | Manejo de excepciones (pagos rechazados, duplicados) | Casos de estudio |
| 1:30 - 1:45 | Generación de reportes de ingresos | Demo + Práctica |
| 1:45 - 1:55 | Mejores prácticas y tips | Discusión |
| 1:55 - 2:00 | Evaluación (quiz) | Individual |

**Material**:
- Presentación: `capacitacion-operadores-caja.pptx`
- Manual de referencia: `docs/manual-usuario.md` (sección Caja)
- Ejercicios prácticos: `ejercicios-caja.pdf`
- Datos de prueba: usuarios y pagos simulados

**Ejercicios prácticos**:
1. Registrar pago en efectivo de $50,000 por tipo MENSUAL
2. Registrar pago con tarjeta de $30,000 por tipo SEMANAL
3. Buscar usuario por documento y registrar pago PSE
4. Generar reporte de ingresos del mes actual
5. Manejar caso de pago duplicado

---

### Sesión 2: Operadores de acceso (2 horas)

**Horario recomendado**: 2:00 PM - 4:00 PM

**Agenda**:

| Tiempo | Tema | Metodología |
|--------|------|-------------|
| 0:00 - 0:10 | Introducción al control de acceso | Presentación |
| 0:10 - 0:25 | Login y navegación del módulo de acceso | Demo + Práctica guiada |
| 0:25 - 0:50 | Hardware biométrico (huella, rostro, voz) | Demo en vivo + Práctica |
| 0:50 - 1:05 | Verificación de accesos: casos exitosos | Ejercicio práctico |
| 1:05 - 1:15 | **DESCANSO** | — |
| 1:15 - 1:35 | Manejo de accesos denegados | Casos de estudio |
| 1:35 - 1:50 | Orientación a caja y resolución de conflictos | Role-playing |
| 1:50 - 1:55 | Troubleshooting de hardware biométrico | Tips |
| 1:55 - 2:00 | Evaluación (quiz + práctica) | Individual |

**Material**:
- Presentación: `capacitacion-operadores-acceso.pptx`
- Manual de referencia: `docs/manual-usuario.md` (sección Acceso)
- Ejercicios prácticos: `ejercicios-acceso.pdf`
- Equipos: lector de huella, cámara web, micrófono

**Ejercicios prácticos**:
1. Verificar acceso con huella dactilar (caso permitido)
2. Verificar acceso con reconocimiento facial (caso denegado - sin derecho)
3. Manejar caso de usuario sin biometría registrada
4. Orientar usuario hacia caja cuando no tiene derecho
5. Resolver problema de hardware (lector no detectado)

---

### Sesión 3: Administradores y auditores (3 horas)

**Horario recomendado**: 9:00 AM - 12:00 PM

**Agenda**:

| Tiempo | Tema | Metodología |
|--------|------|-------------|
| 0:00 - 0:15 | Visión general del sistema y arquitectura | Presentación |
| 0:15 - 0:30 | Login y permisos de administrador | Demo + Práctica |
| 0:30 - 1:00 | Gestión de usuarios (crear, editar, desactivar) | Demo + Ejercicio práctico |
| 1:00 - 1:30 | Registro de biometría (huella, rostro, voz) | Demo en vivo + Práctica |
| 1:30 - 1:45 | **DESCANSO** | — |
| 1:45 - 2:10 | Reportes avanzados (ingresos, asistencias, rechazos) | Demo + Análisis de casos |
| 2:10 - 2:35 | Auditoría de operaciones | Demo + Ejercicio |
| 2:35 - 2:50 | Troubleshooting y resolución de problemas | Casos prácticos |
| 2:50 - 2:57 | Mejores prácticas de administración | Discusión |
| 2:57 - 3:00 | Evaluación (quiz + ejercicio integrador) | Individual |

**Material**:
- Presentación: `capacitacion-administradores.pptx`
- Manual completo: `docs/manual-usuario.md`
- Guía de API: `docs/api-reference.md`
- Troubleshooting: `docs/troubleshooting.md`
- Ejercicios prácticos: `ejercicios-admin.pdf`

**Ejercicios prácticos**:
1. Crear 3 usuarios (estudiante, docente, administrativo)
2. Registrar huella dactilar para un usuario
3. Registrar reconocimiento facial para un usuario
4. Generar reporte de ingresos del último trimestre
5. Generar reporte de asistencias de un grado específico
6. Auditar operaciones de un cajero específico
7. Desactivar usuario y reactivarlo
8. Resolver caso de error de conexión a base de datos (simulado)

---

## Material de capacitación

### Presentaciones (PowerPoint/Google Slides)

1. **capacitacion-operadores-caja.pptx** (40 slides)
   - Introducción al sistema
   - Módulo de caja: flujos completos
   - Métodos de pago
   - Manejo de excepciones
   - Reportes básicos
   - Ejercicios y quiz

2. **capacitacion-operadores-acceso.pptx** (35 slides)
   - Control de acceso: introducción
   - Hardware biométrico
   - Flujos de verificación
   - Casos de acceso permitido/denegado
   - Orientación a caja
   - Troubleshooting básico
   - Ejercicios y quiz

3. **capacitacion-administradores.pptx** (60 slides)
   - Arquitectura del sistema
   - Gestión completa de usuarios
   - Biometría: registro y gestión
   - Reportes avanzados
   - Auditoría
   - Configuración del sistema
   - Troubleshooting avanzado
   - Ejercicios y quiz

### Videos tutoriales

**Duración total**: ~90 minutos

| Video | Duración | Contenido |
|-------|----------|-----------|
| 1. Introducción a EduFeed | 5 min | Overview del sistema, casos de uso |
| 2. Login y navegación | 3 min | Acceso al sistema, menú principal |
| 3. Módulo Caja: Registro de pagos | 10 min | Flujo completo con todos los métodos |
| 4. Módulo Caja: Reportes | 5 min | Generación de reportes de ingresos |
| 5. Módulo Acceso: Verificación biométrica | 15 min | Uso de hardware, casos permitido/denegado |
| 6. Módulo Admin: Gestión de usuarios | 12 min | Crear, editar, desactivar usuarios |
| 7. Módulo Admin: Registro de biometría | 15 min | Huella, rostro, voz |
| 8. Módulo Admin: Reportes avanzados | 10 min | Asistencias, rechazos, auditoría |
| 9. Troubleshooting común | 10 min | Problemas frecuentes y soluciones |
| 10. Mejores prácticas | 5 min | Tips para uso eficiente |

**Plataforma de hosting**: YouTube (playlist privada) o servidor interno

### Documentos de referencia

- **Manual de usuario completo**: `docs/manual-usuario.md`
- **Manual de instalación**: `docs/manual-instalacion.md` (solo admins)
- **Guía de troubleshooting**: `docs/troubleshooting.md`
- **Referencia de API**: `docs/api-reference.md` (solo admins avanzados)
- **Quick Reference Cards** (tarjetas de referencia rápida):
  - `quick-ref-caja.pdf` (1 página, laminada)
  - `quick-ref-acceso.pdf` (1 página, laminada)
  - `quick-ref-admin.pdf` (2 páginas, laminadas)

### Ejercicios prácticos

**Archivos**:
- `ejercicios-caja.pdf`: 5 ejercicios con datos de prueba
- `ejercicios-acceso.pdf`: 5 ejercicios + 3 casos de estudio
- `ejercicios-admin.pdf`: 8 ejercicios integradores

**Base de datos de prueba**:
- Script SQL con 50 usuarios simulados
- 100 pagos de ejemplo
- 200 accesos registrados
- Ambiente sandbox independiente de producción

---

## Logística

### Requisitos de espacio

**Para sesiones presenciales**:
- Sala con capacidad para 15 personas
- Proyector o pantalla grande (mínimo 55")
- Pizarra o rotafolio
- Conexión a internet estable (100 Mbps)
- Tomas eléctricas para todos los participantes

### Equipamiento tecnológico

**Por participante**:
- Computadora con sistema EduFeed instalado (ambiente sandbox)
- Mouse, teclado
- Credenciales de acceso según rol

**Hardware biométrico** (sesiones de acceso y admin):
- 2-3 lectores de huella dactilar USB
- 2-3 cámaras web (1080p)
- 2-3 micrófonos USB

**Para grabación**:
- Cámara de video profesional o smartphone de alta calidad
- Micrófono de solapa para instructor
- Trípode
- Software de edición: OBS Studio (gratuito)

### Materiales impresos

**Por participante**:
- Carpeta con:
  - Agenda de la sesión
  - Manual de usuario (impreso o USB)
  - Ejercicios prácticos
  - Hoja de evaluación
  - Quick Reference Card laminada
  - Encuesta de satisfacción
- Bolígrafo
- Bloc de notas

### Refrigerios

- Café, agua, jugos
- Snacks (galletas, frutas)
- Para sesión de 3 horas: considerar almuerzo ligero

---

## Evaluación

### Estructura de evaluaciones

#### Operadores de caja

**Quiz teórico** (10 preguntas, 50 puntos):
- 5 preguntas de opción múltiple (5 pts c/u)
- 3 preguntas de verdadero/falso (5 pts c/u)
- 2 preguntas de respuesta corta (10 pts c/u)

**Ejercicio práctico** (50 puntos):
- Registrar 2 pagos correctamente (20 pts)
- Generar reporte de ingresos (15 pts)
- Resolver caso de pago rechazado (15 pts)

**Tiempo**: 15 minutos  
**Aprobación**: ≥80 puntos (80%)

#### Operadores de acceso

**Quiz teórico** (10 preguntas, 40 puntos):
- 6 preguntas de opción múltiple (5 pts c/u)
- 4 preguntas de verdadero/falso (2.5 pts c/u)

**Ejercicio práctico** (60 puntos):
- Verificar 2 accesos con huella (20 pts)
- Verificar 1 acceso con rostro (15 pts)
- Manejar caso de acceso denegado correctamente (15 pts)
- Orientar usuario a caja (10 pts)

**Tiempo**: 20 minutos  
**Aprobación**: ≥80 puntos (80%)

#### Administradores y auditores

**Quiz teórico** (15 preguntas, 45 puntos):
- 10 preguntas de opción múltiple (3 pts c/u)
- 5 preguntas de respuesta corta (3 pts c/u)

**Ejercicio integrador** (55 puntos):
- Crear usuario y registrar biometría (15 pts)
- Generar 2 reportes diferentes (20 pts)
- Auditar operaciones (10 pts)
- Resolver problema de troubleshooting (10 pts)

**Tiempo**: 25 minutos  
**Aprobación**: ≥80 puntos (80%)

### Criterios de calificación

**Escala**:
- 90-100: Excelente
- 80-89: Aprobado
- 70-79: Necesita refuerzo
- <70: Requiere recapacitación

**Refuerzo**:
- Participantes con 70-79: sesión de refuerzo de 1 hora
- Participantes con <70: repetir capacitación completa

### Certificación

**Certificado de aprobación** incluye:
- Nombre del participante
- Rol capacitado
- Fecha de capacitación
- Calificación obtenida
- Firma del instructor
- Sello de la institución

**Validez**: 1 año (requiere re-certificación anual)

---

## Seguimiento post-capacitación

### Primera semana

- **Día 1**: Email de seguimiento con material de referencia
- **Día 3**: Encuesta de satisfacción (Google Forms)
- **Día 5**: Sesión de Q&A virtual (1 hora, opcional)

### Primer mes

- **Semana 2**: Monitoreo de uso del sistema (analytics)
- **Semana 4**: Entrevista con usuarios para identificar dificultades

### Soporte continuo

**Canales de soporte**:
- Email: soporte@edufeed.com
- Slack: #edufeed-soporte
- WhatsApp: Grupo de usuarios
- Base de conocimiento: Intranet con FAQs y videos

**Horario de soporte**:
- Lunes a viernes: 8:00 AM - 6:00 PM
- Sábados: 9:00 AM - 1:00 PM
- Emergencias: Línea directa 24/7

**Re-capacitaciones**:
- Trimestral: Sesión de actualización (1 hora)
- Anual: Re-certificación completa
- Ad-hoc: Para nuevas funcionalidades

---

## Cronograma de implementación

### Fase de preparación (2 semanas)

| Semana | Actividad | Responsable |
|--------|-----------|-------------|
| 1 | Crear presentaciones y material | Equipo técnico |
| 1 | Preparar ambiente sandbox | DevOps |
| 1 | Grabar videos tutoriales | Instructor + Video editor |
| 2 | Revisar y aprobar material | Project Manager |
| 2 | Imprimir material y preparar logística | Admin |
| 2 | Enviar invitaciones y confirmar asistencia | RRHH |

### Fase de ejecución (1 semana)

| Día | Sesión | Horario | Participantes |
|-----|--------|---------|---------------|
| Lunes | Operadores de caja | 9:00 - 11:00 AM | 5-10 personas |
| Martes | Operadores de acceso | 2:00 - 4:00 PM | 3-5 personas |
| Miércoles | Administradores | 9:00 - 12:00 PM | 2-3 personas |
| Jueves | Refuerzo (si aplica) | 2:00 - 3:00 PM | Según necesidad |
| Viernes | Evaluaciones pendientes | 9:00 - 11:00 AM | Según necesidad |

### Fase de seguimiento (4 semanas)

- Semana 1: Soporte intensivo
- Semana 2: Monitoreo de uso
- Semana 3: Ajustes al material
- Semana 4: Reporte final

---

## Métricas de éxito

### KPIs de capacitación

| Métrica | Objetivo | Medición |
|---------|----------|----------|
| Tasa de aprobación | ≥80% | Resultados de evaluaciones |
| Satisfacción | ≥4.0/5.0 | Encuesta post-capacitación |
| Asistencia | ≥90% | Lista de asistencia |
| Videos publicados | 100% | Checklist |
| Material entregado | 100% | Checklist |

### KPIs de adopción del sistema

| Métrica | Objetivo | Período de medición |
|---------|----------|---------------------|
| Usuarios activos diarios | ≥80% capacitados | Primer mes |
| Errores operativos | <5% transacciones | Primer mes |
| Tickets de soporte | <10/semana | Primer mes |
| Tiempo promedio de transacción | ≤2 minutos | Primer mes |

---

## Presupuesto estimado

| Concepto | Cantidad | Costo unitario | Total |
|----------|----------|----------------|-------|
| Instructor (40 horas prep + 7 horas sesión) | 47 h | $50,000/h | $2,350,000 |
| Grabación y edición de videos | 10 videos | $200,000 | $2,000,000 |
| Material impreso | 20 packs | $30,000 | $600,000 |
| Refrigerios | 3 sesiones | $150,000 | $450,000 |
| Hardware biométrico (alquiler) | 3 días | $100,000 | $300,000 |
| Sala de capacitación | 3 días | $200,000 | $600,000 |
| Certificados | 20 | $10,000 | $200,000 |
| Contingencia (10%) | — | — | $650,000 |
| **TOTAL** | — | — | **$7,150,000** |

---

## Checklist de capacitación

### Pre-sesión

- [ ] Sala reservada y confirmada
- [ ] Equipos tecnológicos probados (proyector, computadoras, biométricos)
- [ ] Ambiente sandbox funcionando
- [ ] Material impreso listo
- [ ] Refrigerios confirmados
- [ ] Invitaciones enviadas y confirmadas
- [ ] Lista de asistencia preparada
- [ ] Evaluaciones impresas
- [ ] Equipos de grabación configurados

### Durante sesión

- [ ] Registro de asistencia
- [ ] Entrega de material
- [ ] Grabación iniciada
- [ ] Ejercicios prácticos completados
- [ ] Evaluación aplicada
- [ ] Encuesta de satisfacción entregada

### Post-sesión

- [ ] Calificar evaluaciones
- [ ] Editar video de grabación
- [ ] Subir video a plataforma
- [ ] Enviar certificados a aprobados
- [ ] Programar sesiones de refuerzo (si aplica)
- [ ] Analizar encuestas de satisfacción
- [ ] Generar reporte de la sesión

---

**Última actualización**: 31 de octubre de 2025  
**Versión**: 1.0  
**Aprobado por**: Project Manager EduFeed

Para consultas: capacitacion@edufeed.com
