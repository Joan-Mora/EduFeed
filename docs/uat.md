# FASE 7.4 · Pruebas de Aceptación de Usuario (UAT)

Este documento guía la ejecución de UAT en un ambiente de staging local.

## 1) Preparar ambiente de staging

- Base de datos y pgAdmin (staging)
  - Ejecuta:
    - PowerShell:
      ```powershell
      ./scripts/staging-db-up.ps1
      ```
  - DB: edufeed_staging (puerto 5433)
  - pgAdmin: http://localhost:5051 (uat@local.test / admin123)
  - (Opcional) Sembrar datos de ejemplo realistas:
    ```powershell
    ./scripts/staging-db-seed.ps1
    ```

- Backend (perfil `staging`)
  - Ejecuta:
    ```powershell
    ./scripts/staging-backend-run.ps1
    ```
  - Puerto: 8081
  - Seeding de operadores (automático al arrancar):
    - admin / Admin123$ (roles: ADMIN, OPERADOR_CAJA, OPERADOR_ACCESO, AUDITOR)
    - caja1 / Caja123$ (OPERADOR_CAJA)
    - acceso1 / Acceso123$ (OPERADOR_ACCESO)
    - auditor1 / Auditor123$ (AUDITOR)

- Swagger UI
  - http://localhost:8081/swagger

- Atajos con REST Client (opcional)
  - Archivo con operaciones prearmadas: `docs/uat.http`
  - Permite: login por rol, crear usuario, enrolar biometría, crear/aprobar pago, verificar acceso y consultar reportes.

## 2) Acceso a stakeholders

- Compartir las credenciales por rol:
  - Operador de caja: `caja1 / Caja123$`
  - Operador de acceso: `acceso1 / Acceso123$`
  - Administrador: `admin / Admin123$`
  - Auditor: `auditor1 / Auditor123$`
- Cada rol puede autenticarse vía `POST /api/auth/login` (Swagger UI) y operar con Bearer Token.

## 3) Casos de prueba predefinidos

Checklist y evidencias (marcar como OK/FAIL y adjuntar capturas/logs).

- Operador de caja
  1. Crear 10 pagos variados (DIARIO/MENSUAL/PAQUETE; efectivo/transferencia; con y sin referencia)
  2. Aprobar pagos donde aplique (endpoint `PUT /api/pagos/{id}/aprobar`)
  3. Consultar `GET /api/pagos/estado/APROBADO`

- Operador de acceso
  1. Verificar 20 accesos (mezclar aprobados y denegados)
     - `POST /api/accesos/verificar` con modalidad HUELLA
     - Confirmar motivos de denegación cuando aplique

- Administrador
  1. Crear 5 usuarios (`POST /api/usuarios`)
  2. Enrolar biometría (`POST /api/usuarios/{id}/biometria/enrolar`)
  3. Validar que aparecen plantillas en `GET /api/usuarios/{id}/biometria`

- Auditor
  1. Consultar reportes de asistencias (`GET /api/reportes/asistencias`)
  2. Consultar reportes de ingresos (`GET /api/reportes/ingresos` y `/ingresos/resumen`)

Sugerencia: usar Swagger UI o un archivo `.http` (VS Code REST Client) con ejemplos.

Ejemplos listos en `docs/uat.http`.

## 4) Recopilar feedback y bugs

- Registro de UAT: `docs/uat_resultados.md`
  - Formato por caso: Caso, Rol, Paso, Resultado, Evidencia, Observaciones, Severidad, Estatus.
- Cualquier bug crítico debe registrarse con pasos para reproducir y trazas.

## Criterios de salida (Done de UAT)

- ≥ 90% de casos de prueba exitosos
- Feedback positivo de usuarios finales (sin bloqueadores)
- Bugs críticos resueltos antes de producción

## Troubleshooting

- 401/403: revisa rol del operador y token Bearer
- 5xx: revisar logs en `logs/edufeed-backend.log` y métricas `/actuator/metrics`
- DB: credenciales y puertos según `.env.staging`
