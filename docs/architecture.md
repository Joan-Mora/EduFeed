# Arquitectura — EduFeed

## Resumen
Sistema modular:
- Backend Spring Boot (API REST, seguridad, pagos, reportes, integración caja).
- Desktop JavaFX (acceso/portería y caja) que consume la API.
- Módulo biométrico: interfaz común y proveedores enchufables (huella, rostro, voz). Incluye Mock para desarrollo.
- PostgreSQL + Flyway.

## Biometría
- Huella: proveedor enchufable (SDK del hardware). Durante desarrollo se usa `MockBiometricProvider`.
- Rostro: OpenCV para captura; para reconocimiento se podrá integrar librería de embeddings (p.ej. FaceNet onnx) más adelante.
- Voz: TarsosDSP para extracción de rasgos; reconocimiento a definir (motor local o servicio externo). Por ahora mock.

## Seguridad de datos (RNF-01)
- Datos biométricos NO se almacenan como imágenes crudas; se almacenan como templates/rasgos cifrados (AES-256) con claves en Vault/`Azure Key Vault`/archivo secreto local en dev.
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
