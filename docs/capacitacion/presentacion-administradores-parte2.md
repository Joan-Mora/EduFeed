# Capacitación: Administradores y Auditores
## Sistema EduFeed v2.0 - PARTE 2/2

---

## Repaso rápido (Parte 1)

Hemos cubierto:
- ✅ Arquitectura del sistema
- ✅ Gestión de usuarios (crear, editar, importar)
- ✅ Registro biométrico (huellas y rostros)
- ✅ Gestión de pagos (ajustes excepcionales)

Ahora veremos:
- Reportes avanzados
- Auditoría y logs
- Configuración del sistema
- Troubleshooting
- Ejercicio integrador

---

## 6. Reportes Avanzados

### Tipos de reportes disponibles

**Operacionales**:
1. Ingresos diarios/mensuales (por cajero, método, tipo)
2. Accesos diarios (por horario, usuario, resultado)
3. Vigencias próximas a vencer (alertas)

**Analíticos**:
4. Tendencias de pago (mensual, anual)
5. Preferencias de método de pago
6. Patrones de acceso (horas pico)

**Auditoría**:
7. Log de cambios (quién, qué, cuándo)
8. Pagos rechazados/anulados
9. Intentos de acceso denegados

---

### Reporte 1: Ingresos consolidados

**Parámetros**:
- Rango de fechas
- Filtro por cajero (opcional)
- Filtro por método de pago (opcional)
- Agrupación: Diaria, Semanal, Mensual

**Contenido**:
- Total de ingresos: $X,XXX,XXX
- Cantidad de transacciones: XX
- Desglose por método:
  ```
  Efectivo:       $800,000 (40%)
  Tarjeta:        $600,000 (30%)
  Transferencia:  $400,000 (20%)
  PSE:            $200,000 (10%)
  ```
- Desglose por tipo de pago:
  ```
  MENSUAL:    $1,200,000 (60%)
  SEMANAL:      $600,000 (30%)
  DIARIO:       $200,000 (10%)
  ```

---

**Gráficos** (si se exporta a Excel/PDF):
- Gráfico de barras: Ingresos por día
- Gráfico de pastel: Métodos de pago
- Gráfico de líneas: Tendencia semanal

**Exportación**:
- CSV: Para análisis en Excel
- PDF: Para presentación/impresión
- JSON: Para integraciones

---

### Reporte 2: Vigencias próximas a vencer

**Parámetros**:
- Días de anticipación (ej. 7 días, 15 días)
- Filtro por tipo de usuario (ESTUDIANTE, DOCENTE)

**Contenido**:
```
┌────────────────────────────────────────────────┐
│  VIGENCIAS POR VENCER (próximos 7 días)        │
├────────────────────────────────────────────────┤
│  Usuario         │ Documento  │ Vence el       │
├──────────────────┼────────────┼────────────────┤
│  Juan Pérez      │ 1234567890 │ 05/11/2025     │
│  Ana López       │ 9876543210 │ 06/11/2025     │
│  Carlos Gómez    │ 5555555555 │ 07/11/2025     │
└────────────────────────────────────────────────┘
```

**Uso**:
- Enviar recordatorios por email/SMS
- Planificar capacidad de caja
- Evitar aglomeraciones de último minuto

---

### Reporte 3: Accesos diarios

**Parámetros**:
- Fecha específica o rango
- Filtro por resultado (PERMITIDO, DENEGADO)
- Filtro por motivo de denegación

**Contenido**:
```
Total de accesos: 450
  Permitidos: 420 (93%)
  Denegados:   30 (7%)

Motivos de denegación:
  Pago vencido:       20
  Sin pago:            7
  Usuario inactivo:    3
```

**Gráfico**:
- Histograma de accesos por hora (identificar horas pico)

---

### Reporte 4: Patrones de acceso

**Parámetros**:
- Rango de fechas (ej. último mes)
- Agrupación: Por hora del día

**Contenido**:
```
Horas pico:
  11:00 - 12:00 → 120 accesos (descanso/almuerzo)
  13:00 - 14:00 → 150 accesos (almuerzo principal)
  17:00 - 18:00 →  80 accesos (salida)

Días con mayor afluencia:
  Lunes:    100 accesos
  Miércoles: 95 accesos
```

**Uso**:
- Planificar turnos de operadores
- Optimizar capacidad del comedor
- Identificar anomalías (ej. pico inesperado)

---

### Reporte 5: Log de auditoría

**Parámetros**:
- Rango de fechas
- Filtro por usuario/rol que realizó la acción
- Filtro por tipo de acción (CREATE, UPDATE, DELETE)
- Filtro por módulo (USUARIOS, PAGOS, ACCESOS)

**Contenido**:
```
┌────────────────────────────────────────────────────────────────┐
│  Timestamp          │ Usuario      │ Acción   │ Detalle        │
├─────────────────────┼──────────────┼──────────┼────────────────┤
│  2025-10-31 10:30   │ admin_maria  │ CREATE   │ Usuario 123    │
│  2025-10-31 11:00   │ cajero_juan  │ CREATE   │ Pago #456      │
│  2025-10-31 11:15   │ admin_maria  │ UPDATE   │ Usuario 123    │
│  2025-10-31 11:20   │ auditor_ana  │ READ     │ Reporte XYZ    │
└────────────────────────────────────────────────────────────────┘
```

**Uso**:
- Investigación de incidentes
- Cumplimiento normativo
- Trazabilidad completa

---

## 7. Auditoría y Logs

### ¿Qué se audita?

**Todas las operaciones críticas**:
- ✅ Creación/edición/eliminación de usuarios
- ✅ Registro de pagos
- ✅ Ajustes de vigencia
- ✅ Accesos (permitidos y denegados)
- ✅ Cambios de configuración
- ✅ Login/Logout de operadores

**Información registrada**:
- **Quién**: Usuario/operador que realizó la acción
- **Qué**: Tipo de acción y entidad afectada
- **Cuándo**: Timestamp exacto
- **Dónde**: IP de la computadora (si aplica)
- **Resultado**: Éxito o error

---

### Consultar logs de auditoría

**Interfaz web** (recomendado):
1. Ir a "Auditoría" → "Logs del Sistema"
2. Aplicar filtros según necesidad
3. Ordenar por timestamp (descendente)
4. Exportar a CSV/PDF

**Consola backend** (para debugging):
```bash
# Ver logs en tiempo real
tail -f /var/log/edufeed/backend.log

# Buscar errores
grep ERROR /var/log/edufeed/backend.log

# Filtrar por usuario
grep "usuario=admin_maria" /var/log/edufeed/audit.log
```

---

### Casos de uso de auditoría

**Caso 1: Investigar pago duplicado**
- Usuario dice que le cobraron dos veces
- Revisar log de pagos por documento del usuario
- Identificar si realmente hay 2 registros
- Ver quién los creó y cuándo
- Tomar acción (anular uno si es duplicado)

**Caso 2: Cambio no autorizado**
- Configuración del sistema cambió sin previo aviso
- Revisar log de cambios de configuración
- Identificar quién hizo el cambio
- Contactar al responsable
- Revertir si es necesario

---

**Caso 3: Acceso sospechoso**
- Múltiples intentos fallidos de acceso
- Revisar log de accesos denegados
- Verificar si es un usuario legítimo o intento de fraude
- Tomar medidas (bloquear usuario, investigar)

---

## 8. Configuración del Sistema

### Parámetros configurables

**General**:
- Nombre de la institución
- Logo (para comprobantes y reportes)
- Zona horaria
- Idioma

**Pagos**:
- Tarifas por tipo de pago (DIARIO, SEMANAL, etc.)
- Métodos de pago habilitados
- Requiere comprobante impreso (Sí/No)

---

**Acceso**:
- Umbral de coincidencia biométrica (0.80 - 0.95)
- Modo de búsqueda (1:1 o 1:N)
- Tiempo de espera entre intentos (segundos)
- Permitir acceso sin biometría (emergencia)

**Seguridad**:
- Duración de sesión (horas)
- Complejidad de contraseñas (mínimo caracteres, símbolos)
- Intentos de login permitidos antes de bloqueo
- Rotación de tokens JWT (días)

---

### Modificar configuración

**Pasos**:
1. Ir a "Configuración del Sistema"
2. Seleccionar categoría (General, Pagos, Acceso, Seguridad)
3. Modificar parámetro deseado
4. Click "Guardar cambios"
5. Sistema puede requerir reinicio (advertencia previa)

**Precaución**:
- ❗ Cambios en configuración afectan a TODOS los módulos
- ❗ Documentar el motivo del cambio
- ❗ Hacer respaldo antes de cambios críticos

---

### Configurar tarifas

**Interfaz**:
```
┌────────────────────────────────────────────┐
│  TARIFAS DE PAGO                           │
├────────────────────────────────────────────┤
│  Tipo         │ Días │ Precio (COP)        │
├───────────────┼──────┼─────────────────────┤
│  DIARIO       │   1  │ [ 5,000        ]    │
│  SEMANAL      │   7  │ [30,000        ]    │
│  QUINCENAL    │  15  │ [50,000        ]    │
│  MENSUAL      │  30  │ [80,000        ]    │
│  TRIMESTRAL   │  90  │ [200,000       ]    │
│  SEMESTRAL    │ 180  │ [350,000       ]    │
│  ANUAL        │ 365  │ [600,000       ]    │
├───────────────┴──────┴─────────────────────┤
│  [Guardar cambios]  [Restaurar defaults]  │
└────────────────────────────────────────────┘
```

**Nota**: Cambios solo afectan a pagos futuros, no modifican vigencias existentes.

---

### Configurar umbral biométrico

**¿Qué es?**
- Valor entre 0.0 y 1.0 que indica qué tan similar debe ser la huella capturada vs la almacenada para considerarla "match".

**Valores recomendados**:
- **0.80**: Menos estricto (más "falsos positivos")
- **0.85**: Balanceado (recomendado)
- **0.90**: Más estricto (más "falsos negativos")

**Trade-off**:
- Alto umbral (0.90+): Mayor seguridad, pero usuarios pueden no ser reconocidos fácilmente
- Bajo umbral (0.75-): Mayor comodidad, pero riesgo de confundir usuarios

---

## 9. Troubleshooting Avanzado

### Problema: Backend no responde

**Síntomas**:
- Frontend muestra "Error de conexión"
- Timeout en peticiones API

**Diagnóstico**:
1. Verificar que el servicio backend esté corriendo:
   ```bash
   # En Windows (PowerShell)
   Get-Process | Select-String "java"
   
   # En Linux
   ps aux | grep java
   ```
2. Verificar puerto 8080 abierto:
   ```bash
   netstat -an | findstr :8080
   ```
3. Revisar logs del backend:
   ```bash
   tail -f /var/log/edufeed/backend.log
   ```

---

**Soluciones**:

**Si no está corriendo**:
```bash
# Iniciar backend
java -jar edufeed-backend.jar

# O usar script de inicio
./scripts/start-backend.sh
```

**Si está corriendo pero no responde**:
- Verificar conexión a base de datos
- Revisar memoria disponible (puede estar agotada)
- Reiniciar servicio

---

### Problema: Base de datos lenta

**Síntomas**:
- Consultas tardan >5 segundos
- Reportes no cargan

**Diagnóstico**:
```sql
-- Ver consultas activas
SELECT pid, query, state, query_start 
FROM pg_stat_activity 
WHERE state = 'active';

-- Ver tamaño de tablas
SELECT 
  schemaname, 
  tablename, 
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

---

**Soluciones**:

**Optimizar índices**:
```sql
-- Crear índice en columna frecuentemente buscada
CREATE INDEX idx_pagos_usuario_id ON pagos(usuario_id);
CREATE INDEX idx_accesos_timestamp ON accesos(timestamp);
```

**Limpiar tablas de logs antiguos**:
```sql
-- Eliminar logs de auditoría >6 meses
DELETE FROM auditoria WHERE timestamp < NOW() - INTERVAL '6 months';
```

**Vacuum (mantenimiento)**:
```sql
VACUUM ANALYZE pagos;
VACUUM ANALYZE accesos;
```

---

### Problema: Dispositivos biométricos no reconocidos

**Síntomas**:
- Lector de huellas no aparece en sistema
- Cámara no detectada

**Diagnóstico**:
1. Verificar conexión física (USB)
2. Verificar en Administrador de Dispositivos (Windows):
   - Dispositivos → Dispositivos de imagen
   - Dispositivos → Lectores de huellas
3. Revisar drivers instalados

**Soluciones**:
- Reinstalar drivers del fabricante
- Probar en otro puerto USB
- Verificar compatibilidad del SDK

---

### Problema: Usuarios no pueden hacer login

**Síntomas**:
- Error: "Usuario o contraseña incorrectos"
- Sesión expira inmediatamente

**Diagnóstico**:
1. Verificar credenciales correctas
2. Verificar que usuario esté ACTIVO
3. Verificar que rol sea correcto
4. Revisar logs de autenticación:
   ```bash
   grep "LOGIN_FAILED" /var/log/edufeed/backend.log
   ```

**Soluciones**:
- Resetear contraseña del usuario
- Verificar que token JWT no esté expirado
- Revisar configuración de duración de sesión

---

### Problema: Migración/Flyway fallida

**Síntomas**:
- Error al iniciar backend: "Flyway migration failed"

**Diagnóstico**:
```sql
-- Ver estado de migraciones
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
```

**Soluciones**:

**Opción 1: Reparar Flyway**:
```bash
mvn flyway:repair
```

**Opción 2: Baselining** (si es necesario):
```bash
mvn flyway:baseline
```

**Opción 3: Rollback manual** (último recurso):
```sql
-- Ver script de migración fallida
-- Revertir cambios manualmente
-- Eliminar registro de flyway_schema_history
DELETE FROM flyway_schema_history WHERE version = 'X.Y';
```

---

## 10. Ejercicio Integrador

### Escenario completo

Tu institución está iniciando el año escolar. Debes:
1. Importar 50 estudiantes nuevos (CSV)
2. Registrar biometría de 10 de ellos
3. Configurar tarifa mensual en $90,000 (subió de $80,000)
4. Registrar pago mensual para 5 estudiantes
5. Simular verificación de acceso
6. Generar reporte de ingresos del día
7. Revisar log de auditoría de tus acciones

**Tiempo**: 15 minutos  
**Evaluación**: Por completitud y precisión

---

### Paso 1: Importar estudiantes (CSV)

**Archivo**: `estudiantes_nuevos.csv` (proporcionado)

**Contenido**:
```csv
documento,nombre,tipo,grado,email
1111111111,María García,ESTUDIANTE,10A,maria@mail.com
2222222222,Pedro Ruiz,ESTUDIANTE,10B,pedro@mail.com
...
```

**Tarea**:
- Ir a "Gestión de Usuarios" → "Importar CSV"
- Seleccionar archivo
- Verificar preview
- Confirmar importación
- Verificar que 50 usuarios fueron creados

---

### Paso 2: Registrar biometría (10 usuarios)

**Tarea**:
- Ir a "Registro Biométrico" → "Nueva Huella"
- Para los primeros 10 usuarios del CSV:
  - Buscar por documento
  - Simular captura de huella (sandbox usa huellas ficticias)
  - Guardar
- Verificar que aparecen en lista de "Usuarios con biometría registrada"

---

### Paso 3: Configurar tarifa mensual

**Tarea**:
- Ir a "Configuración del Sistema" → "Tarifas"
- Modificar precio de MENSUAL: de $80,000 a $90,000
- Guardar cambios
- Verificar mensaje: "Configuración actualizada"

---

### Paso 4: Registrar pagos

**Tarea**:
- Ir a "Módulo Caja" (o Admin → Pagos)
- Para 5 de los usuarios con biometría:
  - Buscar por documento
  - Registrar pago MENSUAL ($90,000, efectivo)
  - Confirmar
  - Imprimir comprobante (o guardar PDF)
- Verificar que vigencia es 30 días desde hoy

---

### Paso 5: Simular verificación de acceso

**Tarea**:
- Ir a "Módulo Acceso"
- Para 3 de los usuarios que pagaron:
  - Simular colocación de huella
  - Verificar mensaje: "Acceso autorizado"
- Para 2 usuarios SIN pago:
  - Simular huella
  - Verificar mensaje: "Acceso denegado - Sin pago"

---

### Paso 6: Generar reporte de ingresos

**Tarea**:
- Ir a "Reportes" → "Ingresos"
- Filtro: Fecha = HOY
- Generar reporte
- Verificar total: $450,000 (5 pagos × $90,000)
- Exportar a PDF

---

### Paso 7: Revisar log de auditoría

**Tarea**:
- Ir a "Auditoría" → "Logs del Sistema"
- Filtro: Usuario = TU_USUARIO, Fecha = HOY
- Verificar que aparecen:
  - Importación de 50 usuarios
  - Registro de 10 biometrías
  - Cambio de configuración (tarifa)
  - 5 pagos creados
  - 5 accesos verificados
- Exportar a CSV

---

## Evaluación Final

### Parte teórica (45 puntos)
15 preguntas de opción múltiple, verdadero/falso y respuesta corta

### Parte práctica (55 puntos)
Ejercicio integrador completo (pasos 1-7)

**Aprobación**: ≥80%

---

## Certificación

Si apruebas, recibirás:
- **Certificado de Administrador EduFeed v2.0**
- Acceso completo al sistema productivo
- Credenciales de Admin definitivas
- Manual completo digital
- Acceso a videos de referencia

**Validez**: 1 año (re-certificación anual)

---

## Recursos

### Documentación
- **Manual de usuario**: `/docs/manual-usuario.md`
- **Manual de instalación**: `/docs/manual-instalacion.md`
- **API Reference**: `/docs/api-reference.md`
- **Troubleshooting**: `/docs/troubleshooting.md`

### Soporte
- **Email**: soporte@edufeed.com
- **Slack**: #edufeed-admin
- **Emergencias**: [Teléfono 24/7]
- **Knowledge Base**: [URL intranet]

---

## Siguientes pasos

**Semana 1**: Práctica supervisada
- Acompañamiento de instructor
- Resolución de casos reales
- Feedback continuo

**Mes 1**: Evaluación de desempeño
- Revisión de acciones realizadas
- Identificación de áreas de mejora
- Ajustes si es necesario

**Trimestral**: Reuniones de seguimiento
- Nuevas funcionalidades
- Mejores prácticas compartidas
- Actualizaciones del sistema

---

## ¡Felicitaciones!

Has completado la capacitación de **Administradores y Auditores**.

**Recuerda**:
- Documentar todas las acciones importantes
- Mantener copias de seguridad actualizadas
- Consultar manuales ante dudas
- Reportar bugs o mejoras al equipo de desarrollo

### ¡Gracias por tu atención!

**Instructor**: [Nombre]  
**Email**: capacitacion@edufeed.com  

🎓 **¡Éxito en tu rol como Administrador de EduFeed!** 🎉
