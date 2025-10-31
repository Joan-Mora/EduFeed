# Capacitación: Operadores de Acceso
## Sistema EduFeed v2.0

---

## Agenda (2 horas)

1. Introducción al control de acceso (10 min)
2. Hardware biométrico (20 min)
3. Flujo de verificación biométrica (20 min)
4. **DESCANSO** (10 min)
5. Manejo de accesos denegados (20 min)
6. Orientación y atención al usuario (15 min)
7. Troubleshooting básico (15 min)
8. Ejercicios prácticos / Role-playing (10 min)

---

## 1. Introducción al Control de Acceso

### ¿Qué es el Módulo de Acceso?

Sistema de **verificación biométrica** para controlar el ingreso al comedor.

**Propósito**:
- ✅ Validar que el usuario tenga pago vigente
- ✅ Verificar identidad mediante huella dactilar o rostro
- ✅ Registrar hora de acceso para auditoría
- ✅ Prevenir accesos no autorizados
- ✅ Generar reportes de asistencia

---

### Componentes del sistema

**Hardware**:
- 👆 Lector de huellas dactilares
- 📷 Cámara web (reconocimiento facial)
- 💻 Computadora con software EduFeed Desktop
- 🔊 Altavoz para mensajes de voz

**Software**:
- Módulo de Acceso en EduFeed Desktop
- Base de datos centralizada
- Algoritmos de biometría (1:N)

---

### Tu rol: Operador de Acceso

**Responsabilidades**:
1. Verificar identidad de usuarios mediante biometría
2. Autorizar o denegar acceso según estado del pago
3. Orientar a usuarios sobre el proceso
4. Resolver problemas técnicos básicos
5. Reportar incidencias
6. Mantener el orden en la fila

**NO eres responsable de**:
- Registrar pagos (solo Caja)
- Dar de alta/baja usuarios (solo Admin)
- Reparar hardware (Soporte Técnico)

---

## 2. Hardware Biométrico

### Lector de huellas dactilares

**Tipos comunes**:
1. **Óptico**: Toma imagen de la huella
2. **Capacitivo**: Mide electricidad de crestas
3. **Ultrasónico**: Usa ondas de sonido (más preciso)

**Cuidados**:
- Limpiar sensor con paño suave y seco
- NO usar líquidos directamente
- Evitar golpes o presión excesiva
- Verificar cable USB conectado

---

### Colocación correcta del dedo

**Posición óptima**:

```
        ┌─────────┐
        │ SENSOR  │ ← Centro del sensor
        │    ▓    │ ← Dedo centrado
        │    ▓    │
        │    ▓    │
        └─────────┘
```

**Instrucciones al usuario**:
- "Coloca tu dedo índice en el centro del sensor"
- "Presiona suavemente, sin hacer fuerza"
- "Mantén el dedo quieto hasta que veas el resultado"

---

### Problemas comunes con huellas

**Dedo húmedo/sudado**:
- ❌ Lectura borrosa
- ✅ Pedir que se seque el dedo con papel

**Dedo muy seco/agrietado**:
- ❌ Sensor no detecta crestas
- ✅ Usar reconocimiento facial alternativo

**Dedo sucio**:
- ❌ Patrón alterado
- ✅ Limpiar con toallita húmeda

**Dedo mal posicionado**:
- ❌ Lectura parcial
- ✅ Indicar posición correcta (centro, sin ángulo)

---

### Cámara web (reconocimiento facial)

**Requisitos**:
- Resolución: ≥720p
- Buena iluminación
- Usuario debe mirar de frente
- Distancia: 30-50 cm

**Posición del usuario**:
```
      📷 CÁMARA
        ↓
      30-50 cm
        ↓
      😊 USUARIO
   (mirar de frente)
```

---

### Troubleshooting de hardware

| Problema | Causa probable | Solución |
|----------|----------------|----------|
| Lector no enciende | Cable desconectado | Verificar USB |
| Lector no responde | Driver no instalado | Llamar a Soporte |
| Cámara borrosa | Lente sucia | Limpiar con paño |
| Cámara no detecta cara | Iluminación baja | Encender luces |
| Altavoz sin sonido | Volumen bajo | Subir volumen en Windows |

---

## 3. Flujo de Verificación Biométrica

### Proceso completo

```
1. Usuario se acerca
   ↓
2. Operador solicita identificación
   ↓
3. Usuario coloca huella (o mira cámara)
   ↓
4. Sistema busca coincidencia (1-3 seg)
   ↓
5a. MATCH ✓         5b. NO MATCH ✗
    ↓                   ↓
6a. Verificar vigencia   6b. Intentar otro dedo
    ↓                   ↓
7a. VIGENTE ✓       7b. VENCIDO ✗
    ↓                   ↓
8a. ACCESO PERMITIDO    8b. ACCESO DENEGADO
    🚪✅                 🚫
```

---

### Paso 1: Solicitar identificación (opcional)

**Opción A: Reconocimiento directo**
- Usuario solo pone el dedo
- Sistema busca en toda la base de datos (1:N)
- Más rápido si la base es pequeña (<1000 usuarios)

**Opción B: Identificación previa**
- Preguntar: "¿Número de documento o nombre?"
- Ingresar en sistema
- Sistema busca solo en ese usuario (1:1)
- Más rápido en bases grandes (>1000 usuarios)

**Recomendación**: Usar opción A para agilizar

---

### Paso 2: Captura biométrica

**Para huella**:
1. Usuario coloca dedo en sensor
2. Sistema captura imagen
3. Extrae patrón (minucias)
4. Busca en base de datos

**Para rostro**:
1. Usuario mira cámara
2. Sistema detecta cara
3. Extrae características (distancias, proporciones)
4. Busca en base de datos

**Tiempo**: 1-3 segundos

---

### Paso 3: Resultado de coincidencia

**MATCH (✓)**:
```
┌────────────────────────────┐
│  ✅ IDENTIFICACIÓN EXITOSA │
├────────────────────────────┤
│  Nombre: Juan Pérez        │
│  Documento: 1234567890     │
│  Tipo: ESTUDIANTE          │
│  Grado: 11A                │
│                            │
│  Verificando vigencia...   │
└────────────────────────────┘
```

**NO MATCH (✗)**:
```
┌────────────────────────────┐
│  ❌ NO IDENTIFICADO        │
├────────────────────────────┤
│  La huella no coincide     │
│  con ningún usuario.       │
│                            │
│  Intenta con otro dedo o   │
│  contacta al operador.     │
└────────────────────────────┘
```

---

### Paso 4: Verificación de vigencia

**Si hubo MATCH**:

El sistema revisa automáticamente:
- ¿Tiene pago registrado?
- ¿La fecha actual está dentro de la vigencia?

**Ejemplo**:
- Hoy: 31/10/2025
- Vigencia: 01/10/2025 - 30/11/2025
- ✅ **Vigente** → permitir acceso

**Ejemplo 2**:
- Hoy: 31/10/2025
- Vigencia: 01/09/2025 - 30/09/2025
- ❌ **Vencido** → denegar acceso

---

### Paso 5: Mensaje final

**ACCESO PERMITIDO**:
```
┌─────────────────────────────┐
│  ✅ ACCESO AUTORIZADO       │
├─────────────────────────────┤
│  Bienvenido, Juan Pérez     │
│  Grado: 11A                 │
│  Vigencia: Hasta 30/11/2025 │
│                             │
│  🚪 Puedes pasar            │
└─────────────────────────────┘
```
- 🔊 Sonido: "Acceso autorizado"
- 🟢 Luz verde (si hay indicador LED)

---

**ACCESO DENEGADO**:
```
┌─────────────────────────────┐
│  ❌ ACCESO DENEGADO         │
├─────────────────────────────┤
│  Juan Pérez                 │
│  Motivo: Pago vencido       │
│  Última vigencia: 30/09/2025│
│                             │
│  Dirígete a Caja para       │
│  renovar tu pago.           │
└─────────────────────────────┘
```
- 🔊 Sonido: "Acceso denegado"
- 🔴 Luz roja (si hay indicador LED)

---

## 4. Manejo de Accesos Denegados

### Caso 1: Pago vencido

**Mensaje del sistema**: "Pago vencido"

**Tu acción**:
1. Informar amablemente: "Tu pago está vencido desde el [fecha]"
2. Indicar: "Dirígete a Caja para renovar"
3. Señalar la ubicación de la caja
4. NO dejar pasar (política institucional)

**Excepciones** (si están autorizadas por Admin):
- Primera vez (advertencia)
- Situación especial (verificar con supervisor)

---

### Caso 2: Usuario no registrado

**Mensaje del sistema**: "No identificado"

**Tu acción**:
1. Preguntar: "¿Ya estás registrado en el sistema?"
2. Si dice que SÍ:
   - Intentar con otro dedo (índice, pulgar)
   - Intentar reconocimiento facial
   - Si sigue sin funcionar → reportar a Admin (posible error de registro)
3. Si dice que NO:
   - Indicar: "Primero debes registrarte con Administración"
   - Señalar ubicación de oficina administrativa

---

### Caso 3: Huella no reconocida (pero está registrado)

**Causas probables**:
- Dedo sucio, húmedo o lastimado
- Huella mal registrada inicialmente
- Problema con el sensor

**Tu acción**:
1. Pedir que limpie/seque el dedo
2. Intentar con otro dedo
3. Usar reconocimiento facial como alternativa
4. Si nada funciona:
   - Verificar identidad manualmente (documento)
   - Ingresar documento en sistema
   - Si el usuario aparece y tiene pago vigente → permitir acceso excepcionalmente
   - Reportar a Admin para re-registrar huella

---

### Caso 4: Intento de acceso sin pago

**Mensaje del sistema**: "Sin pago registrado"

**Tu acción**:
1. Informar: "No tienes ningún pago registrado en el sistema"
2. Indicar: "Debes pagar en Caja primero"
3. NO dejar pasar

**Si el usuario insiste** que ya pagó:
- Pedirle el comprobante de pago
- Verificar en sistema con ayuda de Caja/Admin
- Si efectivamente pagó pero no aparece → escalamiento a Admin

---

### Caso 5: Usuario conflictivo

**Situación**: Usuario se molesta por acceso denegado

**Protocolo**:
1. Mantener la calma
2. Explicar cortésmente la política
3. Ofrecer alternativas (ir a Caja, hablar con Admin)
4. NO ceder a presión o amenazas
5. Si persiste → llamar a seguridad o supervisor
6. Registrar el incidente en bitácora

---

## 5. Orientación y Atención al Usuario

### Comunicación efectiva

**Lenguaje positivo**:
- ✅ "Tu pago está vigente hasta el 30/11. ¡Bienvenido!"
- ❌ "Todavía tienes días"

**Lenguaje claro**:
- ✅ "Tu pago venció el 30/09. Por favor, dirígete a Caja."
- ❌ "Estás vencido"

**Tono amable**:
- Sonreír
- Usar "por favor" y "gracias"
- Ser paciente con usuarios nuevos

---

### Instrucciones al usuario

**Usuario nuevo**:
1. "Bienvenido. Este es el sistema de acceso biométrico."
2. "Por favor, coloca tu dedo índice en el sensor" (señalar)
3. "Presiona suavemente y mantén quieto el dedo"
4. Esperar resultado
5. Explicar el mensaje (permitido/denegado y por qué)

**Usuario recurrente**:
- Solo señalar el sensor (ya conoce el proceso)
- Decir "Adelante" al ver luz verde

---

### Gestión de filas

**Horarios pico** (descansos, almuerzo):
- Mantener orden en la fila
- Indicar: "Espera tu turno"
- Evitar aglomeraciones frente al sensor
- Solicitar que tengan listo el dedo (no manos en bolsillos)

**Optimización**:
- Verificar que el sistema esté listo antes de llamar al siguiente
- Evitar tiempos muertos
- Meta: ≤10 segundos por usuario

---

### Preguntas frecuentes (respuestas rápidas)

**"¿Por qué no me reconoce?"**
→ "Intentemos con otro dedo o con reconocimiento facial."

**"¿Hasta cuándo tengo pago?"**
→ [Leer fecha en pantalla] "Hasta el 30/11/2025."

**"Olvidé pagar, ¿puedo entrar solo hoy?"**
→ "Lo siento, necesitas tener pago vigente. Caja está abierta hasta las [hora]."

**"¿Puedo registrar mi huella ahora?"**
→ "Debes ir a Administración primero para que te den de alta."

---

## 6. Troubleshooting Básico

### Problema: Lector de huella no responde

**Síntomas**:
- No enciende luz del sensor
- Sistema dice "Dispositivo desconectado"

**Solución**:
1. Revisar cable USB (desconectar y reconectar)
2. Verificar en Windows: Dispositivos → Buscar "lector de huellas"
3. Si sigue sin funcionar:
   - Usar reconocimiento facial mientras tanto
   - Reportar a Soporte Técnico

---

### Problema: Cámara no detecta rostro

**Síntomas**:
- Pantalla muestra: "No se detecta rostro"
- Usuario está frente a cámara pero no funciona

**Solución**:
1. Verificar iluminación (encender luces)
2. Ajustar distancia del usuario (30-50 cm)
3. Pedir que mire directamente a cámara (sin lentes oscuros, gorra)
4. Reiniciar módulo de acceso (cerrar y abrir)
5. Si persiste → usar lector de huellas alternativo

---

### Problema: Sistema muy lento

**Síntomas**:
- Tarda >10 segundos en identificar
- Pantalla se congela

**Solución**:
1. Verificar conexión a internet (si es requerida)
2. Cerrar otras aplicaciones en la computadora
3. Reiniciar aplicación EduFeed Desktop
4. Si persiste → reportar a Admin/Soporte

**Temporal**: Registrar accesos manualmente en papel

---

### Problema: Reconoce a la persona equivocada

**Síntomas**:
- Usuario A pone su huella
- Sistema dice "Bienvenido Usuario B"

**Solución**:
1. **NO permitir el acceso** (error de identificación)
2. Cancelar el acceso
3. Intentar nuevamente con reconocimiento facial
4. Verificar identidad con documento físico
5. Reportar inmediatamente a Admin (posible error en base de datos)

---

## 7. Ejercicios Prácticos

### Role-playing 1: Acceso normal

**Roles**:
- Operador (tú)
- Usuario con pago vigente

**Escenario**:
- Usuario se acerca por primera vez
- Coloca huella
- Sistema: "Acceso autorizado"

**Evaluar**:
- Saludo amable
- Instrucciones claras
- Confirmación verbal del acceso

---

### Role-playing 2: Pago vencido

**Roles**:
- Operador (tú)
- Usuario con pago vencido

**Escenario**:
- Usuario coloca huella
- Sistema: "Acceso denegado - Pago vencido"
- Usuario se molesta

**Evaluar**:
- Explicación clara y cortés
- Indicación de cómo solucionar
- Manejo de conflicto

---

### Role-playing 3: Huella no reconocida

**Roles**:
- Operador (tú)
- Usuario registrado pero huella no funciona

**Escenario**:
- Usuario coloca huella
- Sistema: "No identificado"
- Usuario dice que sí está registrado

**Evaluar**:
- Intentos con otros dedos
- Uso de reconocimiento facial
- Solución alternativa (verificación manual)

---

## 8. Mejores Prácticas

### Seguridad

✅ **DO (Hacer)**:
- Verificar SIEMPRE identidad en caso de duda
- Reportar comportamientos sospechosos
- NO permitir acceso si el sistema dice "denegado"
- Mantener área de acceso despejada
- Verificar que solo pase 1 persona por validación

❌ **DON'T (No hacer)**:
- Permitir acceso "por esta vez" sin autorización
- Compartir contraseñas o dejar sesión abierta
- Dejar hardware sin supervisión
- Permitir que usuarios toquen la computadora

---

### Higiene del hardware

**Diario**:
- Limpiar sensor de huellas con paño suave
- Limpiar cámara con paño de microfibra
- Verificar cables conectados

**Semanal**:
- Desinfectar sensor con alcohol isopropílico al 70%
- Limpiar teclado y mouse

**Nota**: Especialmente importante en épocas de gripe/COVID

---

### Registro de incidencias

**Al final del turno**, reportar:
- Usuarios que no pudieron acceder (motivo)
- Problemas técnicos
- Incidentes de seguridad
- Sugerencias de mejora

**Formato**: Bitácora en papel o sistema digital

---

## 9. Casos de Estudio

### Caso A: Usuario nuevo nervioso

**Situación**: Estudiante de primer día, no sabe cómo funciona el sistema

**Tu turno**: ¿Cómo lo orientas?

---

### Caso B: Sensor no responde

**Situación**: Lector de huellas se desconectó, hay fila de 20 personas

**Tu turno**: ¿Qué haces?

---

### Caso C: Usuario insiste que pagó

**Situación**: Sistema dice "sin pago", usuario muestra transferencia bancaria de hace 1 hora

**Tu turno**: ¿Cómo manejas la situación?

---

## 10. Recursos

### Manual completo
`/docs/manual-usuario.md` - Sección "Módulo Acceso"

### Quick Reference Card
Tarjeta laminada con pasos básicos (entregada)

### Videos tutoriales
- Configuración de hardware biométrico
- Troubleshooting común
- Mejores prácticas de atención

### Soporte
- **Email**: soporte@edufeed.com
- **Slack**: #edufeed-acceso
- **Teléfono**: [número]
- **Urgencias**: [número 24/7]

---

## Evaluación

### Parte teórica (5 min)
10 preguntas en papel

### Parte práctica (10 min)
- Verificar 2 accesos (1 permitido, 1 denegado)
- Role-playing: resolver caso de huella no reconocida
- Orientar a un "usuario nuevo"

**Aprobación**: ≥80%

---

## ¡Gracias!

### Certificación

Si apruebas, recibirás:
- Certificado digital
- Acceso al sistema productivo
- Usuario y contraseña definitivos
- Quick reference card

### Próximos pasos
- Sesión de práctica supervisada (2 días)
- Evaluación de desempeño (1 semana)
- Feedback y ajustes

---

## Contacto

**Instructor**: [Nombre]  
**Email**: capacitacion@edufeed.com  
**Slack**: #edufeed-capacitacion  

**¡Éxito en tu nuevo rol como Operador de Acceso!** 🎉
