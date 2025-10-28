# Refuerzo de POO en EduFeed

Fecha: 2025-10-28

Se añadieron elementos explícitos de Programación Orientada a Objetos sin romper compatibilidad:

- Constructores sobrecargados en entidades JPA:
  - `Usuario`: no-args (JPA), esenciales y completo.
  - `PlantillaBiometrica`: no-args, esenciales y completo.
- Herencia en proveedores biométricos:
  - Nueva clase base `AbstractBiometricProvider` con atributos comunes de versión.
  - Proveedores extienden la clase base: `HardwareFingerprintProvider`, `FaceRecognitionProvider`, `VoiceRecognitionProvider`, `CompositeBiometricProvider`, `MockBiometricProvider`.
- Polimorfismo conservado:
  - Todas las implementaciones mantienen el contrato `BiometricProvider`.
- Sobrecarga de métodos en servicios:
  - `BiometricService` ahora expone sobrecargas para `enrolar` y `verificar1a1/1aN` que aceptan `Usuario` o `String` para modalidad, delegando a los métodos existentes.

Esto refuerza los pilares de POO (atributos, constructores, herencia, polimorfismo y sobrecarga) sin modificar los endpoints ni el wiring existente.
