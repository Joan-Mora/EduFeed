-- Reparación rápida: actualizar checksum de la migración V1 para coincidir con el checksum local
BEGIN;
UPDATE flyway_schema_history
SET checksum = -1290483415
WHERE version = '1';
COMMIT;