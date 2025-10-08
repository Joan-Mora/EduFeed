# 🍽️ Reto de Transformación Digital: Restaurante Escolar

## 1. Información General del Reto
**Título:** Restaurante Escolar  

---

## 2. Resumen Ejecutivo del Reto

Desarrollo de un **sistema integral para la gestión del restaurante escolar**, con aproximadamente **500 usuarios**, que maneje datos personales y validación biométrica (huella o reconocimiento facial).  

El sistema debe permitir:
- Control de acceso según derechos adquiridos por pago (**diario, mensual o paquetes**).  
- Generación de reportes de **asistencia, pagos e inasistencias**.  
- Integración con sistema de caja y registro de transacciones.  
- Gestión completa de usuarios, pagos, historial de accesos y auditoría.  
- Seguridad biométrica y trazabilidad administrativa.  

---

## 3. Requisitos Funcionales

| ID | Requerimiento | Descripción |
|----|----------------|-------------|
| **RF-01** | Registro de usuarios | Permitir registrar a los ~500 usuarios con datos personales, tipo de usuario (niño, estudiante, docente) y su huella o rostro para validación biométrica y voz. |
| **RF-02** | Validación biométrica | Validar la identidad del usuario mediante huella o reconocimiento facial y voz al ingresar al restaurante. |
| **RF-03** | Control de derecho adquirido | Verificar si el usuario tiene un pago válido (diario, mensual o paquete) antes de permitir el ingreso. |
| **RF-04** | Orientación a caja | En caso de no tener derecho adquirido, mostrar notificación para que el usuario sea orientado a la caja y adquiera el servicio. |
| **RF-05** | Registro de pagos | Permitir registrar pagos por tipo: **mensualidad**, **diario** o **paquete de días**. |
| **RF-06** | Reporte de asistencia | Generar reportes de: usuarios que pagaron mensualidad pero no asistieron, y usuarios que asistieron con pago diario o por días. |
| **RF-07** | Gestión de usuarios | Alta, baja y actualización de información personal y biométrica de los usuarios. |
| **RF-08** | Integración con caja | Integrarse con el sistema de caja para actualizar derechos adquiridos automáticamente tras el pago. |
| **RF-09** | Historial de accesos | Registrar fecha, hora y estado (aprobado/denegado) de cada intento de ingreso. |
| **RF-10** | Reportes administrativos | Generar reportes por tipo de pago, ingresos diarios/mensuales, asistencias y no asistencias. |
| **RF-11** | Auditoría de operaciones | Registrar en bitácora las modificaciones de usuarios, pagos y accesos. |
| **RF-12** | Registro y venta de mensualidades | Gestionar la venta de mensualidades de manera diferenciada, con control administrativo. |
| **RF-13** | Reporte de inasistencias | Generar reportes de personas con derecho mensual o días prepagados que no asistieron. |

---

## 4. Requisitos No Funcionales

| ID | Requerimiento | Descripción |
|----|----------------|-------------|
| **RNF-01** | Seguridad de datos | Cifrar y proteger la información biométrica y financiera conforme a normativa de protección de datos personales. |
| **RNF-02** | Compatibilidad hardware | Garantizar compatibilidad con lectores de huella y cámaras faciales estándar. |

---

## 5. Tecnologías Sugeridas y Otros

- Implementación de **reconocimiento facial y huella digital** mediante herramientas compatibles con hardware estándar.  

---

## 6. Incentivos

- 💰 **Valor total:** $1.500.000 COP  

---

## 7. Entregables Mínimos

1. Manual de Usuario (Digital)  
2. Manual de Instalación (Digital)  
3. URLs de acceso a la solución y/o aplicación  
4. Usuarios, roles y accesos configurados  
5. Documento de Arquitectura  
6. _(Espacio reservado para fecha y firma del responsable)_
