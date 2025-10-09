SET TIME ZONE 'America/Bogota';
SHOW TIME ZONE;
SELECT now();
REFRESH MATERIALIZED VIEW mv_ingresos_diarios;
SELECT dia,tipo_pago,metodo,n_pagos,total FROM mv_ingresos_diarios ORDER BY dia DESC LIMIT 5;
SELECT dia,tipo_derecho,asistencias FROM vw_asistencias_por_dia ORDER BY dia DESC LIMIT 5;
