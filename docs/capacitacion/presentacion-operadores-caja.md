# Capacitación: Operadores de Caja
## Sistema EduFeed v2.0

---

## Agenda (2 horas)

1. Introducción al sistema (10 min)
2. Login y navegación (15 min)
3. Registro de pagos (20 min)
4. Métodos de pago digitales (15 min)
5. **DESCANSO** (10 min)
6. Manejo de excepciones (20 min)
7. Reportes de ingresos (15 min)
8. Mejores prácticas (10 min)
9. Evaluación (5 min)

---

## 1. Introducción al Sistema

### ¿Qué es EduFeed?

Sistema integral de **control de acceso y cobro** para comedores educativos.

**Componentes principales**:
- 💰 Módulo de Caja (tu rol)
- 🚪 Módulo de Acceso
- 👥 Módulo de Administración
- 📊 Reportes

---

### ¿Por qué EduFeed?

**Antes** (manual):
- ❌ Registro en papel o Excel
- ❌ Errores de cálculo
- ❌ Pérdida de información
- ❌ Auditoría difícil

**Ahora** (EduFeed):
- ✅ Registro digital centralizado
- ✅ Cálculo automático de vigencias
- ✅ Respaldos automáticos
- ✅ Auditoría completa

---

### Tu rol: Operador de Caja

**Responsabilidades**:
1. Registrar pagos de usuarios
2. Validar métodos de pago
3. Emitir comprobantes
4. Generar reportes de ingresos diarios
5. Reportar anomalías

**NO eres responsable de**:
- Crear o editar usuarios (solo Admin)
- Configurar el sistema
- Soporte técnico avanzado

---

## 2. Login y Navegación

### Acceso al sistema

**Requisitos**:
- Computadora con sistema instalado
- Usuario y contraseña (proporcionados por Admin)
- Rol: `OPERADOR_CAJA`

**Proceso**:
1. Abrir aplicación EduFeed Desktop
2. Ingresar usuario
3. Ingresar contraseña
4. Click en "Iniciar sesión"

---

### Pantalla principal

```
┌─────────────────────────────────────────┐
│  EduFeed - Operador: Maria González    │
├─────────────────────────────────────────┤
│  🏠 Inicio                              │
│  💰 Caja ← TU MÓDULO                    │
│  📊 Reportes                            │
│  🔧 Configuración                       │
│  🚪 Cerrar sesión                       │
└─────────────────────────────────────────┘
```

**Navegación**:
- Click en "Caja" para acceder a tu área de trabajo

---

### Interfaz del módulo Caja

**Secciones**:
1. **Búsqueda de usuario**: Por documento, nombre
2. **Registro de pago**: Formulario principal
3. **Historial de pagos**: Últimos 10 pagos
4. **Acciones rápidas**: Botones de uso frecuente

**Atajos de teclado**:
- `Ctrl+B`: Buscar usuario
- `Ctrl+N`: Nuevo pago
- `F5`: Refrescar historial
- `Ctrl+R`: Generar reporte

---

## 3. Registro de Pagos

### Flujo básico

```
1. Buscar usuario
   ↓
2. Verificar datos
   ↓
3. Ingresar monto y tipo
   ↓
4. Seleccionar método de pago
   ↓
5. Confirmar
   ↓
6. Imprimir comprobante
```

---

### Paso 1: Buscar usuario

**Opciones**:

**Por documento**:
- Campo: "Número de documento"
- Ejemplo: `1234567890`
- Click en "Buscar"

**Por nombre**:
- Campo: "Nombre o apellido"
- Ejemplo: `Juan Pérez`
- Click en "Buscar"

**Resultado**:
```
Documento: 1234567890
Nombre: Juan Pérez
Tipo: ESTUDIANTE
Grado: 11A
Estado: Activo ✓
```

---

### Paso 2: Verificar datos

**Checklist**:
- ✅ Nombre correcto
- ✅ Documento coincide con identificación física
- ✅ Usuario está activo

**Si hay error**:
- Usuario no encontrado → Verificar documento
- Usuario inactivo → Contactar Admin
- Datos incorrectos → Contactar Admin

---

### Paso 3: Ingresar monto y tipo

**Tipos de pago**:

| Tipo | Duración | Precio típico |
|------|----------|---------------|
| DIARIO | 1 día | $5,000 |
| SEMANAL | 7 días | $30,000 |
| QUINCENAL | 15 días | $50,000 |
| MENSUAL | 30 días | $80,000 |
| TRIMESTRAL | 90 días | $200,000 |
| SEMESTRAL | 180 días | $350,000 |
| ANUAL | 365 días | $600,000 |

**Nota**: Precios son ejemplos, verificar tarifario institucional.

---

### Cálculo de vigencia (automático)

El sistema **calcula automáticamente** las fechas:

**Ejemplo**:
- Hoy: 31 de octubre de 2025
- Tipo: MENSUAL (30 días)
- Vigencia inicio: 31/10/2025 00:00
- Vigencia fin: 30/11/2025 23:59

**No necesitas calcular manualmente** ✓

---

### Paso 4: Seleccionar método de pago

**Opciones disponibles**:

1. **EFECTIVO** 💵
   - Recibe el dinero
   - Verifica el monto
   - Entrega cambio si aplica

2. **TARJETA** 💳
   - Usa datáfono externo
   - Ingresa referencia de aprobación en sistema

---

3. **TRANSFERENCIA** 🏦
   - Verifica comprobante bancario
   - Ingresa número de transacción como referencia

4. **PSE** 🌐
   - Pago electrónico
   - Referencia generada por pasarela

5. **QR_BANCOLOMBIA** 📱
   - Usuario escanea código QR
   - Esperar confirmación automática

6. **NEQUI** 📲
   - Transferencia desde app Nequi
   - Ingresar referencia

---

### Paso 5: Confirmar pago

**Pantalla de confirmación**:

```
┌─────────────────────────────────────┐
│  CONFIRMAR PAGO                     │
├─────────────────────────────────────┤
│  Usuario: Juan Pérez                │
│  Documento: 1234567890              │
│                                     │
│  Monto: $80,000                     │
│  Tipo: MENSUAL (30 días)            │
│  Método: EFECTIVO                   │
│                                     │
│  Vigencia: 31/10/2025 - 30/11/2025  │
│                                     │
│  [Confirmar]  [Cancelar]            │
└─────────────────────────────────────┘
```

**Verificar TODOS los datos antes de confirmar**

---

### Paso 6: Imprimir comprobante

**Comprobante incluye**:
- Logotipo institución
- Número de recibo
- Fecha y hora
- Datos del usuario
- Monto pagado
- Tipo y vigencia
- Método de pago
- Cajero que registró
- Firma o sello

**Acciones**:
- Entregar original al usuario
- Conservar copia en caja (si aplica)

---

## 4. Métodos de Pago Digitales

### PSE (Pago Seguro en Línea)

**Flujo**:
1. Usuario dice que pagará por PSE
2. En sistema, seleccionar método: PSE
3. Usuario realiza pago desde su banco online
4. **Esperar confirmación** (puede tardar 5-10 min)
5. Sistema recibe webhook con resultado
6. Ingresar referencia PSE en campo "Referencia"
7. Confirmar pago

**Importante**: No confirmar hasta tener referencia PSE válida

---

### QR Bancolombia

**Flujo**:
1. Seleccionar método: QR_BANCOLOMBIA
2. Sistema genera código QR en pantalla
3. Usuario escanea con app Bancolombia
4. Usuario confirma pago en su app
5. **Esperar notificación automática** (instantánea)
6. Sistema marca pago como aprobado
7. Imprimir comprobante

**Ventaja**: No necesitas ingresar referencia manualmente

---

### Nequi

**Flujo**:
1. Usuario transfiere desde app Nequi a cuenta institucional
2. Usuario muestra comprobante en su celular
3. Verificar:
   - Monto correcto
   - Cuenta destino correcta
   - Fecha actual
4. En sistema, seleccionar método: NEQUI
5. Ingresar código de transacción (ej. `NEQ123456789`)
6. Confirmar pago

---

### Conciliación de pagos digitales

**Al final del día**:
1. Ir a "Reportes" → "Pagos por conciliar"
2. Comparar lista con extractos bancarios
3. Marcar como conciliados los que coinciden
4. Reportar discrepancias a Admin/Supervisor

**Discrepancias comunes**:
- Pago registrado pero no aparece en banco → Revisar referencia
- Pago en banco pero no registrado → Registrar manualmente
- Montos diferentes → Verificar comisiones

---

## 5. Manejo de Excepciones

### Caso 1: Pago rechazado (tarjeta)

**Síntoma**: Datáfono rechaza la tarjeta

**Solución**:
1. NO registrar el pago en sistema
2. Informar al usuario del rechazo
3. Ofrecer alternativas:
   - Intentar con otra tarjeta
   - Pagar en efectivo
   - Transferencia bancaria
4. Si prueba con otro método, registrar el nuevo intento

---

### Caso 2: Pago duplicado

**Síntoma**: Usuario dice que ya pagó, pero no aparece en sistema

**Solución**:
1. Buscar usuario en historial de pagos
2. Verificar con filtros:
   - Últimos 7 días
   - Método de pago mencionado
3. Si encuentras pago:
   - Mostrar comprobante al usuario
   - Reimprimir si perdió el suyo
4. Si NO encuentras pago:
   - Pedir comprobante al usuario
   - Verificar con Admin o revisar auditoría
   - NO registrar nuevo pago hasta confirmar

---

### Caso 3: Usuario sin documento

**Síntoma**: Usuario no trae cédula/documento

**Solución**:
1. Preguntar número de documento de memoria
2. Buscar en sistema
3. Verificar con pregunta de seguridad:
   - "¿Cuál es tu grado?"
   - "¿Cuál es tu apellido?"
4. Si confirma identidad → proceder con pago
5. Si NO confirma → solicitar que regrese con documento

**Importante**: NUNCA registrar pago sin identificar al usuario

---

### Caso 4: Monto incorrecto recibido

**Síntoma**: Usuario paga $50,000 pero debe $80,000

**Solución**:

**Si paga menos**:
1. Informar el faltante
2. Opciones:
   - Usuario completa el pago → registrar monto total
   - Usuario pagará después → NO registrar (pago incompleto no válido)

**Si paga más** (ej. $100,000 por pago de $80,000):
1. Entregar cambio: $20,000
2. Registrar solo $80,000 en sistema

---

### Caso 5: Sistema lento o no responde

**Síntoma**: Al confirmar pago, sistema se queda "pensando"

**Solución**:
1. **NO** presionar múltiples veces el botón (puede duplicar)
2. Esperar 30 segundos
3. Si sigue sin responder:
   - Verificar conexión a internet
   - Verificar backend corriendo (Admin)
4. Si no funciona:
   - Registrar pago **manualmente en papel**
   - Informar a Admin inmediatamente
   - Cuando sistema vuelva, registrar los pagos manuales

---

## 6. Reportes de Ingresos

### Generar reporte diario

**Pasos**:
1. Ir a "Reportes" → "Ingresos"
2. Filtros:
   - Fecha inicio: HOY (ej. 31/10/2025 00:00)
   - Fecha fin: HOY (ej. 31/10/2025 23:59)
3. Click "Generar"
4. Revisar resultados en pantalla
5. Click "Exportar a PDF" o "Exportar a CSV"
6. Imprimir o enviar por email

---

### Contenido del reporte

**Información incluida**:
- Total de ingresos del día: **$450,000**
- Cantidad de pagos: **15**
- Desglose por método:
  - Efectivo: $200,000 (8 pagos)
  - Tarjeta: $150,000 (5 pagos)
  - Transferencia: $100,000 (2 pagos)
- Desglose por tipo:
  - DIARIO: $30,000 (6 pagos)
  - SEMANAL: $120,000 (4 pagos)
  - MENSUAL: $300,000 (5 pagos)
- Cajero: María González

---

### Reporte semanal/mensual

**Cambiar filtros**:
- Semanal: Últimos 7 días
- Mensual: Primer día del mes - Último día del mes

**Ejemplo mensual** (octubre 2025):
- Fecha inicio: 01/10/2025
- Fecha fin: 31/10/2025

**Usar para**:
- Cierre de caja mensual
- Conciliación contable
- Planificación

---

### Reporte por método de pago

**Filtro adicional**:
- Seleccionar método específico (ej. "EFECTIVO")
- Útil para:
  - Cuadre de caja física
  - Conciliación bancaria (transferencias)
  - Análisis de preferencias de pago

---

## 7. Mejores Prácticas

### Seguridad

✅ **DO (Hacer)**:
- Cerrar sesión al terminar turno
- No compartir tu contraseña
- Verificar SIEMPRE identidad del usuario
- Guardar comprobantes físicos en orden
- Reportar errores inmediatamente

❌ **DON'T (No hacer)**:
- Dejar sesión abierta sin supervisión
- Registrar pagos sin confirmar identidad
- Modificar fechas de vigencia manualmente (Admin)
- Eliminar registros de pagos

---

### Eficiencia

**Tips para ser más rápido**:
1. Usar atajos de teclado (`Ctrl+B`, `Ctrl+N`)
2. Tener tarifario a mano (impreso o digital)
3. Preparar cambio en efectivo antes de iniciar
4. Mantener datáfono listo y con papel
5. Familiarizarse con usuarios frecuentes

**Meta**: ≤2 minutos por transacción

---

### Comunicación

**Con usuarios**:
- Ser amable y paciente
- Explicar claramente el proceso
- Confirmar datos en voz alta
- Entregar comprobante SIEMPRE

**Con equipo**:
- Reportar problemas técnicos a Admin
- Compartir casos inusuales en reuniones
- Documentar mejoras sugeridas

---

### Cuadre de caja

**Al final del turno**:
1. Generar reporte de ingresos del turno
2. Contar efectivo físico
3. Comparar con reporte (debe coincidir)
4. Si hay diferencia:
   - Verificar comprobantes
   - Revisar historial de pagos
   - Reportar a supervisor
5. Entregar efectivo y reporte a supervisor/admin
6. Cerrar sesión

---

## 8. Casos de Estudio

### Caso A: Pago mensual en efectivo

**Escenario**:
- Usuario: Ana Rodríguez (estudiante)
- Documento: 9876543210
- Pago: $80,000 en efectivo
- Tipo: MENSUAL

**Tu turno**: Simular el proceso completo

---

### Caso B: Pago semanal con tarjeta

**Escenario**:
- Usuario: Carlos Gómez (docente)
- Documento: 5555555555
- Pago: $30,000 con tarjeta
- Tipo: SEMANAL
- Referencia datáfono: APPR123456

**Tu turno**: Simular el proceso completo

---

### Caso C: Pago duplicado

**Escenario**:
- Usuario: Laura Martínez
- Documento: 1111111111
- Situación: Dice que ya pagó ayer $50,000 pero no tiene comprobante

**Tu turno**: ¿Qué haces?

---

## 9. Preguntas Frecuentes (FAQs)

### ¿Qué hago si olvido mi contraseña?

Contactar a Administrador para resetearla.

### ¿Puedo cambiar la fecha de vigencia de un pago?

NO. Las vigencias son calculadas automáticamente. Solo Admin puede hacer ajustes excepcionales.

### ¿Puedo eliminar un pago registrado por error?

NO. Contactar a Admin o Supervisor para marcarlo como rechazado/anulado.

---

### ¿Qué hago si el sistema dice "Usuario no encontrado"?

1. Verificar número de documento
2. Buscar por nombre
3. Si sigue sin aparecer, el usuario debe registrarse primero (Admin)

### ¿Cómo imprimo un comprobante nuevamente?

Ir a historial de pagos, buscar el pago, click derecho → "Reimprimir comprobante"

---

## Evaluación

### Parte teórica (5 min)

10 preguntas en papel

### Parte práctica (10 min)

Ejercicio en sistema sandbox:
1. Registrar 2 pagos
2. Generar reporte
3. Resolver caso de pago rechazado

**Aprobación**: ≥80%

---

## ¡Gracias!

### Recursos

- **Manual completo**: `/docs/manual-usuario.md`
- **Quick Reference**: Tarjeta laminada (entregada)
- **Videos**: Playlist YouTube (enlace por email)
- **Soporte**: soporte@edufeed.com

### Certificación

Si apruebas la evaluación, recibirás:
- Certificado digital
- Acceso al sistema productivo
- Usuario y contraseña definitivos

---

## Contacto

**Instructor**: [Nombre]  
**Email**: capacitacion@edufeed.com  
**Slack**: #edufeed-capacitacion  

**¡Éxito en tu nuevo rol como Operador de Caja!** 🎉
