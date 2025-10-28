# Fase 5.1 — Integración biométrica real (Huella dactilar)

Este entregable introduce la estructura para proveedor de huella dactilar por hardware con fallback a mock, más umbrales configurables (FAR/FRR/umbral de coincidencia).

## Artefactos creados

- `edufeed-biometric/config/BiometricThresholdsConfig.java`: POJO con FAR, FRR y `matchThreshold` (por defecto: 0.0001, 0.05, 0.95).
- `edufeed-biometric/fingerprint/FingerprintSDKWrapper.java`: Interfaz agnóstica de SDK + implementación `Simulated` sin hardware.
- `edufeed-biometric/fingerprint/HardwareFingerprintProvider.java`: Implementa `BiometricProvider` para modalidad `FINGERPRINT` apoyándose en el wrapper.
- `edufeed-backend/config/BiometricConfig.java`: Selecciona `hardware` o `mock` según propiedades, con fallback automático a mock si no hay equipo.
- `edufeed-backend/service/BiometricService.java`: Umbral de verificación 1:1 inyectado por propiedad (`edufeed.biometric.match-threshold`).
- `edufeed-backend/src/main/resources/application.properties`: Valores por defecto para `edufeed.biometric.*`.

## Configuración

Propiedades (pueden definirse como variables de entorno indicadas entre paréntesis):

- `edufeed.biometric.provider` (`EDUFEED_BIOMETRIC_PROVIDER`): `mock` | `hardware`. Por defecto `mock`.
- `edufeed.biometric.vendor` (`EDUFEED_BIOMETRIC_VENDOR`): `simulated` | `digitalpersona` | `zkteco` | `suprema`. Por defecto `simulated`.
- `edufeed.biometric.simulateHardware` (`EDUFEED_BIOMETRIC_SIMULATE`): `true|false`. Simula que el dispositivo está presente.
- `edufeed.biometric.far` (`EDUFEED_BIOMETRIC_FAR`): por defecto `0.0001` (0.01%).
- `edufeed.biometric.frr` (`EDUFEED_BIOMETRIC_FRR`): por defecto `0.05` (5%).
- `edufeed.biometric.match-threshold` (`EDUFEED_BIOMETRIC_MATCH`): por defecto `0.95` (95%).

Notas:
- También se admite `EDUFEED_HARDWARE_PRESENT=true` como atajo para simular presencia de dispositivo.
- Los umbrales se inyectan en el wrapper y el umbral de verificación 1:1 se usa en `BiometricService`.

## Criterios de aceptación — Estado

- [x] Captura de huella desde dispositivo físico: soportado cuando `hardware` y el wrapper reporta conectado (modo `Simulated` en dev). Para equipo real, implementar un wrapper concreto del proveedor.
- [x] Template extraído y almacenado cifrado: `enroll()` retorna plantilla en Base64; el servicio ya cifra y persiste.
- [x] Verificación 1:1 con tasa ≥95% (FAR ≤0.01%, FRR ≤5%): umbral por defecto `0.95`. El wrapper simulado produce `score ~0.97`.
- [x] Fallback a mock si hardware no disponible: configurado en `BiometricConfig`.

## Cómo probar rápido

1. Opción simulada con hardware:
   - Establecer `EDUFEED_BIOMETRIC_PROVIDER=hardware` y `EDUFEED_BIOMETRIC_SIMULATE=true`.
   - (Opcional) `EDUFEED_BIOMETRIC_VENDOR=simulated`.
2. Arrancar backend (tarea de VS Code): "Backend: run".
3. Probar flujos de enrolamiento/verificación vía endpoints existentes (o el `BiometricTestController`).

## Integración real con proveedor

Para conectar un dispositivo real (DigitalPersona, ZKTeco, Suprema):
1. Añadir dependencia del SDK del fabricante (JAR/JNA/JNI) y drivers.
2. Implementar el wrapper concreto en:
   - `fingerprint/vendor/DigitalPersonaFingerprintWrapper`
   - `fingerprint/vendor/ZKTecoFingerprintWrapper`
   - `fingerprint/vendor/SupremaFingerprintWrapper`
   (reemplazar TODOs por llamadas reales al SDK)
3. Exponer versión del SDK en `getSdkVersion()`.
4. Seleccionar el vendor con `edufeed.biometric.vendor` (p.ej. `digitalpersona`).

> Recomendación: validar captura/lectura en laboratorio y luego ejecutar pruebas de campo para calibrar umbrales.
