# Arquitectura — EduFeed

## Resumen
Sistema modular:
- Backend Spring Boot (API REST, seguridad, pagos, reportes, integración caja).
- Desktop JavaFX (acceso/portería y caja) que consume la API.
- Módulo biométrico: interfaz común y proveedores enchufables (huella, rostro, voz). Incluye Mock para desarrollo.
- PostgreSQL + Flyway.

## Biometría [ACTUALIZADO V3 - Nov 2025]
- **Huella**: WebAuthn con passkeys (W3C standard). Public key en PEM format, signature verification con EC/RSA. `WebAuthnService` + `WebAuthnController` para registro/autenticación.
- **Rostro**: face-api.js (vladmandic) con modelos TensorFlow.js (ssdMobilenetv1, faceLandmark68Net, faceRecognitionNet). Extracción de descriptor 128D, matching con distancia euclidiana L2 < 0.6. Preview en vivo con detección.
- **Voz**: Meyda para extracción de MFCC (13 coeficientes). Matching con similaridad coseno > 0.85. Grabación explícita con MediaRecorder (mínimo 3s).
- **Servicios**: `BiometricAuthService` (L2/cosine matching), `BiometricRegistrationService` (gestión de sesiones individuales), `WebAuthnService` (CBOR parsing, signature verification).
- **Frontend**: Thymeleaf + face-api.js CDN + Meyda CDN. MediaDevices API para cámara/micrófono. WebAuthn navigator.credentials.
- **Desktop**: JavaFX con QR individual por modalidad, polling 2s, auto-cierre tras registro.

## Seguridad de datos (RNF-01) [ACTUALIZADO V3]
- Datos biométricos NO se almacenan como imágenes/audio crudos; se almacenan como vectores JSON:
  - Huella: Public key PEM + signCount (anti-replay) en `webauthn_credencial`
  - Rostro: 128D descriptor de face-api.js en `plantilla_biometrica`
  - Voz: 13D MFCC promedio en `plantilla_biometrica`
- Templates cifrados con AES-256 en `plantilla_biometrica.plantilla` (BLOB)
- WebAuthn challenges generados con `SecureRandom` (32 bytes)
- Sesiones UUID con expiración (2 min) en `ConcurrentHashMap` (memoria) y tabla `webauthn_sesion` (BD)
- Tráfico HTTPS (en prod). Bcrypt para contraseñas de usuarios administradores.
- Auditoría (RF-11) vía `@EntityListeners` y tabla `audit_log`.

## Esquema preliminar
- app_user(id, document, full_name, user_type, ...)
- payment(id, user_id, type, from_date, to_date, days_quota, remaining_days)
- access_log(id, user_id, at, status, reason)
- audit_log(id, actor, action, entity, entity_id, at, details)

## Integración con caja (RF-08)
- Webhook o cola (RabbitMQ opcional) para notificar pagos aplicados; API para registrar pago y actualizar derechos.

## Entregables
- Manual de usuario, de instalación, Documento de arquitectura (este), URLs de acceso, usuarios/roles.
