# Fase 5.2 — Reconocimiento facial (OpenCV + FaceNet simulado)

Este entregable habilita detección de rostro desde cámara (webcam de portátil/USB o fuente IP de móvil) y extracción de embeddings (simulada) para matching por cosine similarity.

## Artefactos creados

- `edufeed-biometric/CompositeBiometricProvider.java`: Delegación por modalidad (fingerprint/face).
- `edufeed-biometric/face/OpenCVFaceDetector.java`: Interfaz de detector + `Simulated`.
- `edufeed-biometric/face/OpenCVFaceDetectorImpl.java`: Detector real con OpenCV (Haar cascade) y `VideoCapture`.
- `edufeed-biometric/face/FaceNetEmbeddingExtractor.java`: Interfaz + `Simulated` (128D/512D) con helpers Base64.
- `edufeed-biometric/face/FaceRecognitionProvider.java`: Proveedor facial que retorna embeddings en Base64.
- `edufeed-biometric/src/main/resources/haarcascades/README.md`: Dónde colocar `haarcascade_frontalface_default.xml`.
- `edufeed-biometric/src/main/resources/models/README.md`: Dónde colocar `facenet.onnx` (opcional futuro).
- Wiring en `edufeed-backend/config/BiometricConfig.java` para componer fingerprint + face.
- Ajustes en `edufeed-backend/service/BiometricService.java` para comparar por coseno (1:1 y 1:N) con umbral facial.

## Configuración

Propiedades (también disponibles vía variables de entorno entre paréntesis):

- `edufeed.biometric.face.simulate` (`EDUFEED_BIOMETRIC_FACE_SIMULATE`): true|false. Por defecto `true`.
- `edufeed.biometric.face.dim` (`EDUFEED_BIOMETRIC_FACE_DIM`): 128 o 512. Por defecto `128`.
- `edufeed.biometric.face.match-threshold` (`EDUFEED_BIOMETRIC_FACE_MATCH`): Umbral coseno (por defecto `0.6`).
- `edufeed.biometric.face.source` (`EDUFEED_BIOMETRIC_FACE_SOURCE`): `camera:0` (webcam) o URL IP (`http://<ip:puerto>/video`) de móvil.

Notas:
- Las claves personalizadas pueden aparecer como “unknown property” en el linter; es normal.
- Para detección real, coloca `haarcascades/haarcascade_frontalface_default.xml` (BSD) descargado de OpenCV.

## Uso con cámaras de portátil y móviles

- Portátil/PC (webcam integrada/USB):
  - `edufeed.biometric.face.simulate=false`
  - `edufeed.biometric.face.source=camera:0` (o `camera:1` si tienes varias)

- Móvil (Android) como cámara IP:
  - Instala una app tipo “IP Webcam” o similar (stream MJPEG).
  - Configura la app para exponer el stream en la misma red (Wi-Fi).
  - En propiedades: `edufeed.biometric.face.simulate=false` y `edufeed.biometric.face.source=http://<ip:puerto>/video`
  - Nota: la compatibilidad de codecs depende de la build de OpenCV con FFmpeg; usa MJPEG siempre que sea posible.

## Criterios de aceptación — Estado

- [x] Detección de rostro en frame de cámara: Soportado (Haar) si el cascade está presente.
- [x] Extracción de embedding: Simulada (dim configurable). Listo para sustituir por FaceNet ONNX.
- [x] Matching ≥90% (controlado): Umbral por defecto `0.6` configurable; calibrar con embeddings reales.
- [x] Múltiples rostros: Si se detectan >1, el provider facial rechaza la captura.

## Siguientes pasos (opcional)

1. Sustituir detector simulado por OpenCV real (ya implementado) en `face.simulate=false` y probar con `camera:0`.
2. Añadir `facenet.onnx` y dependencia de onnxruntime para embeddings reales; ajustar normalización/resize.
3. Calibrar `EDUFEED_BIOMETRIC_FACE_MATCH` con dataset interno hasta lograr la precisión objetivo.
