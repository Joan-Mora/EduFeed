Place facenet.onnx here. This repository ships without the model due to licensing/size.
Suggested sources:
- FaceNet (ONNX) export compatible with 128D/512D embeddings.
- Ensure input preprocessing matches the model (resize, normalization).

Configure embedding dimension via `edufeed.biometric.face.dim` (default 128).