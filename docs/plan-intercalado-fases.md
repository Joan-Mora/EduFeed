# Plan de trabajo intercalado — Fases 1, 3, 5, 7 y 9

Fecha: 2025-10-20
Responsable: Tú (fases 1, 3, 5, 7, 9)
Compañero: Responsable de fases 2, 4, 6, 8, 10

---

## Vista general
- Objetivo: completar el proyecto alternando fases con tu compañero, minimizando bloqueos.
- Estrategia: tú comienzas la base (F1), levantas contratos (F3) pronto para habilitar trabajo en paralelo, integras biometría real (F5) cuando existan servicios mínimos, consolidas calidad (F7) y cierras con docs/capacitación (F9).

---

## Huella por teléfono (decisión de diseño)

Debido a que no se dispone de huellero físico, la validación de huella se realizará en el teléfono del usuario/operador usando autenticación biométrica del dispositivo vía WebAuthn/FIDO2. No se almacenan imágenes/plantillas de huella en el servidor; se guarda `credentialId/publicKey` y se valida una firma de desafío en cada autenticación.

Arquitectura (resumen):
- Backend (Spring Boot): endpoints WebAuthn (register/authenticate begin/complete), sesiones de desafío, persistencia de credenciales y asociación a usuario/dispositivo.
- Desktop (JavaFX): muestra QR con `sessionId/challenge`, hace polling del estado y presenta el resultado de verificación.
- Teléfono (PWA web mínima): ejecuta `navigator.credentials.create/get` y envía attestation/assertion al backend.

Flujos:
1) Registro: Desktop inicia registro → QR → teléfono realiza `create()` → backend valida y persiste credencial.
2) Autenticación: Desktop inicia autenticación → QR → teléfono realiza `get()` → backend valida firma → registra acceso (APROBADO/DENEGADO según derecho vigente).

Modelo de datos adicional (F1):
- Tablas: `dispositivos` y `credenciales_webauthn` con relaciones a `usuarios`.

Endpoints (F3):
- `POST /webauthn/register/begin|complete`, `POST /webauthn/authenticate/begin|complete`, `GET /webauthn/qr/{sessionId}`, `GET /webauthn/sessions/{sessionId}`.

Asignación por fase:
- F1 (tú): entidades/repos/DTOs de `dispositivos` y `credenciales_webauthn`.
- F2 (compañero): servicios de sesión WebAuthn y validación.
- F3 (tú): controladores WebAuthn + PWA mínima servida por backend + contratos OpenAPI.
- F5 (tú): `RemotePhoneFingerprintProvider` que orquesta el flujo WebAuthn y expone la interfaz biométrica homogénea.
- F6 (compañero): UI Desktop (QR + polling + manejo de timeouts).
- F7 (tú): tests unitarios/mocks de attestation/assertion y pruebas de integración.
- F9 (tú): documentación y troubleshooting del flujo.

Riesgos/mitigaciones: Compatibilidad de navegadores móviles (usar PWA), conectividad (fallback QR+polling), cumplimiento RNF-01 (no plantillas crudas en servidor).

## Línea de tiempo (estimada)
- Semana 1-3: F1 (Dominio/persistencia) — tú
- Semana 2-4: F2 (Servicios negocio) — compañero (requiere F1)
- Semana 3-5: F3 (API/Contratos) — tú (parcial al inicio con stubs)
- Semana 5-8: F5 (Biometría real) — tú (depende de F2 básico)
- Semana 6-8: F6 (Desktop) — compañero (usa F3)
- Semana 8-10: F7 (Testing/QA) — tú (tests unit+integ+carga)
- Semana 9-10: F8 (DevOps/CI/CD) — compañero
- Semana 10-11: F9 (Docs/Capacitación) — tú
- Semana 11: F10 (Entrega/Cierre) — compañero

---

## Dependencias clave y acuerdos
- F1 → F2: entregar entidades, repos y mappers listos.
- F2 ↔ F3: alinear contratos REST y errores; usar stubs hasta que servicios estén listos.
- F3 → F6: API estable para que desktop avance.
- F5 depende de: proveedores/mock listo; acuerdos de hardware.
- F7 depende de: F1-6 en estado funcional.

---

## Detalle por fase (tu responsabilidad)

### Fase 1 — Dominio y persistencia (Semana 1-3)
Tareas:
- [ ] Modelos JPA: Usuario, PlantillaBiometrica, Pago, PaquetePago, DerechoUso, Acceso, UsoPaquete, Auditoria, Rol, UsuarioRol, TransaccionCaja, CalendarioServicio.
- [ ] Enums: TipoUsuario, Modalidad, TipoPago, EstadoPago, EstadoAcceso.
- [ ] Repositorios Spring Data para todas las entidades.
- [ ] DTOs base y mappers (al menos Usuario/Pago/Acceso/DerechoUso).
- [ ] Tests @DataJpaTest para 3 entidades críticas (Usuario, Pago, Acceso).
 - [ ] (Huella por teléfono) Entidades extra: `Dispositivo`, `CredencialWebAuthn`; repositorios y DTOs correspondientes.

Criterios de aceptación:
- CRUD repositorios funcionales.
- `ddl-auto: validate` sin errores.
- 5+ tests verdes cubriendo save/find y constraints.

Entregables:
- `backend/model/*`, `backend/repository/*`, `backend/dto/*`, `backend/dto/mapper/*`.

Hitos intermedios:
- H1.1: Entidades y enums (semana 1)
- H1.2: Repos y DTOs/mappers (semana 2)
- H1.3: Tests @DataJpaTest (semana 3)

---

### Fase 3 — API REST y contratos (Semana 3-5)
Tareas:
- [ ] Controladores: UsuarioController, PagoController, AccesoController, ReportController (sólo endpoints de lectura iniciales), WebhookController (stub).
- [ ] GlobalExceptionHandler con modelo de error estándar.
- [ ] OpenAPI con ejemplos de request/response.
- [ ] Seguridad básica por rol (métodos anotados, sin JWT aún).
- [ ] Endpoints mock/stub que devuelvan 200 con payload de ejemplo hasta conectar servicios.
 - [ ] (Huella por teléfono) Endpoints WebAuthn (begin/complete) + recurso QR + sesión de polling; PWA mínima servida desde backend.

Criterios de aceptación:
- Swagger documenta todos los endpoints.
- 10+ pruebas MVC con MockMvc.
- Manejo de errores centralizado.

Entregables:
- `backend/api/*`, `backend/api/GlobalExceptionHandler.java`, `backend/resources/openapi-examples/*` (opcional).

Hitos intermedios:
- H3.1: Esqueleto de controladores y rutas (semana 3)
- H3.2: Respuestas de ejemplo y validación (@Valid) (semana 4)
- H3.3: Pruebas MVC (semana 5)

---

### Fase 5 — Biometría real (Semana 5-8)
Tareas:
- [ ] Selección de hardware de huella y SDK.
- [ ] `HardwareFingerprintProvider` + wrapper del SDK.
- [ ] Pipeline facial: OpenCV + modelo embeddings (FaceNet ONNX u otro).
- [ ] Configuración de umbrales (FAR/FRR) y feature flags (mock vs real).
- [ ] Pruebas en campo y ajuste de threshold.
 - [ ] (Huella por teléfono) Implementar `RemotePhoneFingerprintProvider` que use WebAuthn para validar y notificar resultado al desktop.

Criterios de aceptación:
- Demostración con dispositivo real.
- Tasa de acierto (FAR/FRR) dentro de objetivo acordado con cliente.
- Fallback a mock sin recompilar.

Entregables:
- `edufeed-biometric/biometric/fingerprint/*`, `biometric/face/*`, `backend/config/BiometricConfig.java` actualizado.

Hitos intermedios:
- H5.1: Proveedor de huella (semana 6)
- H5.2: Pipeline facial (semana 7)
- H5.3: Pruebas en sitio (semana 8)

---

### Fase 7 — Testing y QA (Semana 8-10)
Tareas:
- [ ] Unit tests servicios/controladores (cobertura ≥80%).
- [ ] Integración con Testcontainers para PostgreSQL.
- [ ] Pruebas end-to-end de flujos críticos.
- [ ] Pruebas de carga (JMeter/Gatling) y reporte de performance.
- [ ] Semilla QA actualizada.
 - [ ] (Huella por teléfono) Mocks de WebAuthn (attestation/assertion), tests de polling/sesión y guía de pruebas manuales en iOS/Android.

Criterios de aceptación:
- `mvn test` verde, cobertura ≥80%.
- p95 de `/api/access/check` <2s, `/api/payments` <1s.
- Reporte de carga adjunto.

Entregables:
- `backend/src/test/*`, `scripts/tests/*`, `scripts/seed/*` actualizada.

Hitos intermedios:
- H7.1: Unit + integra (semana 9)
- H7.2: Carga + reporte (semana 10)

---

### Fase 9 — Documentación y capacitación (Semana 10-11)
Tareas:
- [ ] Actualizar `manual-usuario.md` y `manual-instalacion.md` con capturas y pasos finales.
- [ ] `docs/api-reference.md` con ejemplos curl.
- [ ] `docs/troubleshooting.md` con problemas frecuentes.
- [ ] Slides y guías de capacitación (operadores, admin, auditor).
 - [ ] (Huella por teléfono) Documentar registro/autenticación WebAuthn, requisitos de navegador y flujos de QR/push.

Criterios de aceptación:
- Revisión con stakeholders y aprobación.
- Checklists de entregables completados.

Entregables:
- `docs/manual-usuario.md`, `docs/manual-instalacion.md`, `docs/api-reference.md`, `docs/troubleshooting.md`, `docs/capacitacion/*`.

Hitos intermedios:
- H9.1: Docs API y troubleshooting (semana 10)
- H9.2: Manuales y slides (semana 11)

---

## Coordinación semanal (resumen)
- Lunes: sync 30 min (bloqueos, dependencias F2↔F3, F5↔F6)
- Miércoles: checkpoint técnico (PRs en revisión, cobertura, issues)
- Viernes: demo/avance e integración cruzada (desktop consume API, biometría real en punto de acceso)

## Riesgos y mitigación
- Hardware biométrico: iniciar compra/selección en semana 4 como tarde.
- Contratos API: fijarlos en semana 3 para habilitar F6.
- Performance: planear pruebas de carga en semana 9.

## Indicadores de avance (KPIs)
- % endpoints con pruebas MVC
- Cobertura de tests backend
- Tasa FAR/FRR en biometría
- p95 latencias de endpoints críticos
- % documentos actualizados y aprobados
