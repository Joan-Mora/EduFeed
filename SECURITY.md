# Seguridad (RNF-01)

- Cifrado de información sensible en reposo (AES-256) y en tránsito (HTTPS).
- No almacenar datos biométricos crudos; sólo templates/rasgos cifrados.
- Gestión de secretos mediante variables de entorno y, en producción, gestor de secretos.
- Auditoría de operaciones: creación, actualización y eliminación de usuarios, pagos y accesos.
- Mínimos privilegios y controles de acceso por rol.
