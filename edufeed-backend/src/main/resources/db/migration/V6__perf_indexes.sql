-- Índices de rendimiento para endpoints críticos (FASE 7.3)
-- pagos: filtros por estado, usuario y rango de fechas
CREATE INDEX IF NOT EXISTS idx_pagos_estado_creado ON pagos (estado_pago, creado_en DESC);
CREATE INDEX IF NOT EXISTS idx_pagos_usuario_creado ON pagos (usuario_id, creado_en DESC);
CREATE INDEX IF NOT EXISTS idx_pagos_tipo_creado ON pagos (tipo_pago, creado_en DESC);
CREATE INDEX IF NOT EXISTS idx_pagos_referencia_externa ON pagos (referencia_externa);

-- accesos: historial por usuario, estado y fecha
CREATE INDEX IF NOT EXISTS idx_accesos_usuario_fecha ON accesos (usuario_id, fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_accesos_estado_fecha ON accesos (estado, fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_accesos_modalidad_fecha ON accesos (modalidad, fecha_hora DESC);

-- derechos_uso: listados de activos y vigencias
CREATE INDEX IF NOT EXISTS idx_derechos_activo_usuario ON derechos_uso (activo, usuario_id);
CREATE INDEX IF NOT EXISTS idx_derechos_vigencias ON derechos_uso (vigente_desde, vigente_hasta);
