# Manual de Instalación — Borrador

## Prerrequisitos
- Windows 10/11, Docker Desktop, JDK 21, Maven 3.9, PowerShell 7, VS Code.

## Pasos
1. Clonar el repo y abrir en VS Code.
2. Copiar `.env.example` a `.env`.
3. `./scripts/db-up.ps1` para levantar DB y pgAdmin.
4. Compilar: `mvn -T1C -DskipTests package`.
5. Ejecutar backend: `mvn -pl edufeed-backend -am spring-boot:run`.
6. Ejecutar desktop: `mvn -pl edufeed-desktop -am -DskipTests javafx:run`.

## Hardware biométrico
- Instalar drivers/SDK del proveedor. Configurar el `BiometricProvider` correspondiente.
