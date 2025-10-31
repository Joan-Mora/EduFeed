# Evaluación: Administradores y Auditores
## Sistema EduFeed v2.0

**Nombre del participante**: ___________________________________  
**Fecha**: _______________  
**Instructor**: ___________________

---

## Parte I: Quiz Teórico (45 puntos)

**Instrucciones**: Responde las siguientes preguntas. Tiempo: 15 minutos.

### A. Preguntas de opción múltiple (3 puntos c/u = 27 puntos)

**1. ¿Cuál es el puerto por defecto del backend de EduFeed?**
- [ ] a) 3000
- [ ] b) 5432
- [ ] c) 8080
- [ ] d) 9090

**2. ¿Qué algoritmo se usa para encriptar contraseñas?**
- [ ] a) MD5
- [ ] b) SHA-256
- [ ] c) BCrypt
- [ ] d) AES-256

**3. ¿Cuál es el umbral de coincidencia biométrica recomendado?**
- [ ] a) 0.70
- [ ] b) 0.85
- [ ] c) 0.95
- [ ] d) 1.00

**4. ¿Qué rol tiene permisos de solo lectura en el sistema?**
- [ ] a) OPERADOR_CAJA
- [ ] b) OPERADOR_ACCESO
- [ ] c) ADMIN
- [ ] d) AUDITOR

**5. ¿Cuántas muestras de huella se capturan durante el registro?**
- [ ] a) 1
- [ ] b) 2
- [ ] c) 3
- [ ] d) 5

---

**6. Si la base de datos está lenta, ¿qué comando SQL se recomienda para mantenimiento?**
- [ ] a) COMMIT
- [ ] b) ROLLBACK
- [ ] c) VACUUM ANALYZE
- [ ] d) DROP INDEX

**7. ¿Qué archivo se usa para importar usuarios masivamente?**
- [ ] a) JSON
- [ ] b) XML
- [ ] c) CSV
- [ ] d) SQL

**8. ¿Qué información NO se registra en el log de auditoría?**
- [ ] a) Quién realizó la acción
- [ ] b) Cuándo se realizó
- [ ] c) Contraseña del usuario
- [ ] d) IP de la computadora

**9. ¿Cuál es la duración por defecto de un token JWT?**
- [ ] a) 1 hora
- [ ] b) 8 horas
- [ ] c) 24 horas
- [ ] d) 7 días

---

### B. Verdadero o Falso (3 puntos c/u = 12 puntos)

**10. Un administrador puede cambiar el número de documento de un usuario después de crearlo.**
- [ ] Verdadero
- [ ] Falso

**11. Las plantillas biométricas se guardan encriptadas con AES-256.**
- [ ] Verdadero
- [ ] Falso

**12. Los cambios en la configuración de tarifas afectan retroactivamente a los pagos ya registrados.**
- [ ] Verdadero
- [ ] Falso

**13. Es posible eliminar completamente los datos biométricos de un usuario.**
- [ ] Verdadero
- [ ] Falso

---

### C. Pregunta de respuesta corta (6 puntos)

**14. Menciona 3 componentes principales de la arquitectura de EduFeed:**

1. _________________________________________________
2. _________________________________________________
3. _________________________________________________

**15. Si un usuario reporta que no es reconocido por el lector de huellas, menciona 3 pasos de troubleshooting:**

1. _________________________________________________
2. _________________________________________________
3. _________________________________________________

---

## Parte II: Ejercicio Práctico Integrador (55 puntos)

**Instrucciones**: Realiza las siguientes tareas en el sistema sandbox. Tiempo: 20 minutos.

### Ejercicio 1: Gestión de usuarios (15 puntos)

**Tareas**:
1. **Crear 2 usuarios manualmente** (8 pts):
   - Usuario 1: Estudiante (documento: 1234567890, nombre: Test Estudiante, grado: 11A)
   - Usuario 2: Docente (documento: 9876543210, nombre: Test Docente, área: Matemáticas)
   
2. **Importar 5 usuarios desde CSV** (5 pts):
   - Archivo proporcionado: `test_usuarios.csv`
   - Verificar que todos fueron importados correctamente
   
3. **Editar un usuario** (2 pts):
   - Cambiar el grado del Usuario 1 de "11A" a "11B"

---

**Evaluador verifica**:
- [ ] Usuario 1 creado con datos correctos
- [ ] Usuario 2 creado con datos correctos
- [ ] 5 usuarios importados desde CSV
- [ ] Usuario 1 modificado correctamente (grado = 11B)

**Puntos obtenidos**: _____ / 15

---

### Ejercicio 2: Registro biométrico (10 puntos)

**Tareas**:
1. Registrar huella dactilar para Usuario 1 (5 pts)
2. Registrar rostro para Usuario 2 (5 pts)

**Nota**: En sandbox, usar huellas/fotos de prueba proporcionadas.

**Evaluador verifica**:
- [ ] Huella registrada para Usuario 1 con calidad ≥70%
- [ ] Rostro registrado para Usuario 2 (mínimo 3 capturas)
- [ ] Ambos usuarios aparecen en lista de "Usuarios con biometría"

**Puntos obtenidos**: _____ / 10

---

### Ejercicio 3: Configuración del sistema (10 puntos)

**Tareas**:
1. Cambiar tarifa de pago SEMANAL de $30,000 a $35,000 (5 pts)
2. Cambiar umbral de coincidencia biométrica de 0.85 a 0.90 (5 pts)

**Evaluador verifica**:
- [ ] Tarifa SEMANAL modificada a $35,000
- [ ] Umbral biométrico modificado a 0.90
- [ ] Mensaje de confirmación "Configuración actualizada"

**Puntos obtenidos**: _____ / 10

---

### Ejercicio 4: Generar reportes (10 puntos)

**Tareas**:
1. Generar reporte de ingresos del día actual (5 pts)
2. Exportar a PDF (3 pts)
3. Interpretar resultado: ¿Cuánto se recaudó hoy? (2 pts)

**Evaluador verifica**:
- [ ] Reporte generado con filtro de fecha correcto
- [ ] Archivo PDF descargado/guardado
- [ ] Respuesta correcta al total recaudado

**Total recaudado HOY (según reporte)**: $ ________________

**Puntos obtenidos**: _____ / 10

---

### Ejercicio 5: Auditoría y troubleshooting (10 puntos)

**Tareas**:
1. Consultar log de auditoría para tus acciones de HOY (5 pts)
2. Resolver caso: "Un usuario dice que registró un pago pero no aparece en el sistema. ¿Cómo investigas?" (5 pts)
   - Explicar paso a paso al evaluador

**Evaluador verifica**:
- [ ] Log de auditoría consultado correctamente (filtro por usuario + fecha)
- [ ] Log muestra todas las acciones realizadas en el ejercicio
- [ ] Explicación de troubleshooting incluye:
  - Buscar usuario en sistema
  - Revisar historial de pagos
  - Consultar log de auditoría
  - Pedir comprobante al usuario
  - Verificar con cajero o revisar logs del backend

**Puntos obtenidos**: _____ / 10

---

## Calificación

| Sección | Puntos obtenidos | Puntos máximos |
|---------|------------------|----------------|
| Quiz teórico | _____ | 45 |
| Ejercicio práctico | _____ | 55 |
| **TOTAL** | _____ | **100** |

---

## Resultado

**Calificación**: _____ / 100

**Escala**:
- 90-100: Excelente ⭐⭐⭐
- 80-89: Aprobado ✓
- 70-79: Necesita refuerzo
- <70: Requiere recapacitación

**Estado**: 
- [ ] **APROBADO** (≥80) → Certificación completa
- [ ] Requiere sesión de refuerzo (70-79)
- [ ] Requiere recapacitación (<70)

---

## Observaciones del instructor

### Fortalezas identificadas:
_________________________________________________________________
_________________________________________________________________

### Áreas de mejora:
_________________________________________________________________
_________________________________________________________________

### Recomendaciones:
_________________________________________________________________
_________________________________________________________________

---

## Certificación

**Si aprobó (≥80)**:

**Certificado otorgado**: 
- [ ] Sí  
- [ ] No

**Nivel de certificación**:
- [ ] Administrador Completo (≥90)
- [ ] Administrador Estándar (80-89)

**Validez**: 1 año (hasta ___/___/2026)

**Número de certificado**: EF-ADMIN-2025-_______

---

**Firma del participante**: ___________________  
**Firma del instructor**: ___________________  
**Fecha**: _______________

---

## Respuestas correctas (solo para instructor)

### Quiz teórico:
1. **c) 8080**
2. **c) BCrypt**
3. **b) 0.85**
4. **d) AUDITOR**
5. **c) 3**
6. **c) VACUUM ANALYZE**
7. **c) CSV**
8. **c) Contraseña del usuario** (NUNCA se guarda en logs)
9. **b) 8 horas**
10. **Falso** (el documento NO se puede cambiar, es clave primaria)
11. **Verdadero**
12. **Falso** (solo afectan a pagos futuros)
13. **Verdadero**
14. Ejemplo de respuestas correctas:
   - Frontend (EduFeed Desktop)
   - Backend (EduFeed API / Spring Boot)
   - Base de Datos (PostgreSQL)
   - Dispositivos biométricos
15. Ejemplo de respuestas correctas:
   - Intentar con otro dedo
   - Usar reconocimiento facial como alternativa
   - Limpiar/secar el dedo del usuario
   - Verificar que el lector de huellas esté funcionando
   - Re-registrar la huella si ninguna opción funciona
   - Verificar calidad de la huella registrada inicialmente

### Rúbrica ejercicio práctico:

**Ejercicio 1 (15 pts)**:
- Usuario 1 creado: 4 pts (todos los datos correctos)
- Usuario 2 creado: 4 pts (todos los datos correctos)
- Importación CSV: 5 pts (5 usuarios importados sin errores)
- Edición usuario: 2 pts (grado modificado correctamente)

**Ejercicio 2 (10 pts)**:
- Huella Usuario 1: 5 pts (captura exitosa, calidad ≥70%)
- Rostro Usuario 2: 5 pts (mínimo 3 capturas, guardado correcto)

**Ejercicio 3 (10 pts)**:
- Cambio tarifa: 5 pts (SEMANAL = $35,000)
- Cambio umbral: 5 pts (0.90)

**Ejercicio 4 (10 pts)**:
- Generar reporte: 5 pts (filtro correcto, ejecutado)
- Exportar PDF: 3 pts (archivo descargado)
- Interpretar: 2 pts (respuesta numérica correcta)

**Ejercicio 5 (10 pts)**:
- Consultar log: 5 pts (filtros correctos, resultados visibles)
- Troubleshooting: 5 pts (menciona al menos 4 de los pasos esperados)

---

## Notas adicionales para el instructor

### Materiales necesarios para la evaluación:
- Sistema sandbox configurado con datos de prueba
- Archivo CSV de prueba (`test_usuarios.csv`) con 5 usuarios
- Huellas/fotos de prueba para registro biométrico
- Acceso a módulo de Admin completo
- Cronómetro para controlar tiempos

### Criterios de evaluación práctica:
- **Precisión**: ¿Los datos ingresados son correctos?
- **Eficiencia**: ¿Completó la tarea en tiempo razonable?
- **Comprensión**: ¿Entiende QUÉ está haciendo o solo sigue pasos?
- **Troubleshooting**: ¿Sabe resolver errores si aparecen?

### Feedback post-evaluación:
- Revisar cada ejercicio con el participante
- Explicar errores cometidos
- Reforzar conceptos débiles
- Programar sesión de refuerzo si aplica (70-79)
- Entregar certificado si aprobó (≥80)
