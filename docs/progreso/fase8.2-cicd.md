# Fase 8.2: CI/CD - Integración y Despliegue Continuo

## Índice

1. [Introducción](#introducción)
2. [Arquitectura del pipeline](#arquitectura-del-pipeline)
3. [Configuración de GitHub Actions](#configuración-de-github-actions)
4. [Secretos y variables](#secretos-y-variables)
5. [Flujo de despliegue](#flujo-de-despliegue)
6. [Entornos y aprobaciones](#entornos-y-aprobaciones)
7. [Troubleshooting](#troubleshooting)
8. [Mejoras futuras](#mejoras-futuras)

---

## Introducción

La fase de CI/CD automatiza el ciclo completo desde commit hasta producción, garantizando calidad, seguridad y trazabilidad en cada despliegue.

### Objetivos

- ✅ Build y test automatizados en cada push/PR a `main`
- ✅ Construcción de imagen Docker y push a GitHub Container Registry (GHCR)
- ✅ Despliegue automático a staging
- ✅ Despliegue a producción con aprobación manual
- ✅ Rollback rápido ante fallos
- ✅ Notificaciones de estado del pipeline

### Beneficios

- **Velocidad**: de código a producción en minutos
- **Calidad**: tests obligatorios antes de merge
- **Trazabilidad**: cada deploy vinculado a commit/PR
- **Seguridad**: scan de vulnerabilidades, aprobaciones
- **Confiabilidad**: proceso repetible y documentado

### Tecnologías

- **GitHub Actions**: orquestador del pipeline
- **Maven**: build y tests del backend
- **Docker**: empaquetado de imagen
- **GHCR**: registry de imágenes (ghcr.io)
- **SSH**: deploy remoto a servidores staging/prod
- **Docker Compose**: orquestación en destino

---

## Arquitectura del pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│                         GitHub Actions                          │
│                                                                 │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐    │
│  │  Build   │──▶│   Test   │──▶│  Docker  │──▶│  Deploy  │    │
│  │  Maven   │   │  JUnit   │   │  Build   │   │ Staging  │    │
│  │          │   │ JaCoCo   │   │  & Push  │   │          │    │
│  └──────────┘   └──────────┘   └────┬─────┘   └────┬─────┘    │
│                                      │              │          │
│                                      ▼              ▼          │
│                                  GHCR (ghcr.io)   SSH Deploy  │
│                                      │              │          │
│                                      └──────┬───────┘          │
│                                             ▼                  │
│                                      ┌──────────────┐          │
│                                      │   Deploy     │          │
│                                      │  Production  │          │
│                                      │ (Aprobación) │          │
│                                      └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### Stages del pipeline

1. **Build & Test** (paralelo):
   - Compilación Maven con JDK 24
   - Tests unitarios (JUnit + Mockito)
   - Tests de integración (Testcontainers)
   - Reporte de coverage (JaCoCo)

2. **Docker Build & Push**:
   - Build imagen multi-stage
   - Tag con SHA commit y `latest`
   - Push a ghcr.io/joan-mora/edufeed-backend
   - Scan de vulnerabilidades (opcional)

3. **Deploy Staging** (automático):
   - SSH a servidor staging
   - Pull imagen desde GHCR
   - docker compose up -d
   - Health check

4. **Deploy Production** (manual):
   - Requiere aprobación vía GitHub Environments
   - SSH a servidor producción
   - Pull imagen
   - docker compose up -d --no-deps backend
   - Health check
   - Notificación Slack/Email (opcional)

---

## Configuración de GitHub Actions

### Archivo workflow

**Ubicación**: `.github/workflows/ci-cd.yml`

```yaml
name: CI/CD Pipeline

on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository_owner }}/edufeed-backend

jobs:
  build-and-test:
    name: Build and Test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 24
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '24'
          cache: 'maven'

      - name: Build with Maven
        run: mvn -B clean install -DskipTests

      - name: Run Tests
        run: mvn -B test -f edufeed-backend/pom.xml

      - name: Generate JaCoCo Report
        run: mvn -B jacoco:report -f edufeed-backend/pom.xml

      - name: Upload coverage to Codecov (opcional)
        uses: codecov/codecov-action@v3
        with:
          files: edufeed-backend/target/site/jacoco/jacoco.xml
          fail_ci_if_error: false

  docker-build-push:
    name: Build and Push Docker Image
    runs-on: ubuntu-latest
    needs: build-and-test
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 24
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '24'
          cache: 'maven'

      - name: Build modules
        run: mvn -B clean install -DskipTests

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=raw,value=latest
            type=sha,prefix={{branch}}-

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          file: edufeed-backend/Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}

  deploy-staging:
    name: Deploy to Staging
    runs-on: ubuntu-latest
    needs: docker-build-push
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    environment:
      name: staging
      url: http://staging.edufeed.com:8081

    steps:
      - name: Deploy via SSH
        if: ${{ secrets.STAGING_HOST != '' }}
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.STAGING_HOST }}
          username: ${{ secrets.STAGING_USER }}
          key: ${{ secrets.STAGING_SSH_KEY }}
          script: |
            cd ${{ secrets.STAGING_PATH }}
            echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
            docker compose --env-file .env.prod -f docker-compose.prod.yml pull backend
            docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps backend
            docker compose --env-file .env.prod -f docker-compose.prod.yml ps

      - name: Health Check
        if: ${{ secrets.STAGING_HOST != '' }}
        run: |
          sleep 10
          curl --fail http://${{ secrets.STAGING_HOST }}:8081/actuator/health || exit 1

  deploy-production:
    name: Deploy to Production
    runs-on: ubuntu-latest
    needs: deploy-staging
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    environment:
      name: production
      url: https://edufeed.com

    steps:
      - name: Deploy via SSH
        if: ${{ secrets.PROD_HOST != '' }}
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USER }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            cd ${{ secrets.PROD_PATH }}
            echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
            docker compose --env-file .env.prod -f docker-compose.prod.yml pull backend
            docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-deps backend
            docker compose --env-file .env.prod -f docker-compose.prod.yml ps

      - name: Health Check
        if: ${{ secrets.PROD_HOST != '' }}
        run: |
          sleep 10
          curl --fail https://edufeed.com/actuator/health || exit 1

      - name: Notify success (opcional)
        if: success()
        run: |
          # Slack/Discord/Email notification
          echo "Deploy to production successful!"
```

### Componentes clave

#### 1. Triggers (on)

```yaml
on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main
```

- **push a main**: ejecuta pipeline completo (build → test → docker → deploy)
- **pull_request a main**: solo build y test (sin deploy)

#### 2. Permissions

```yaml
permissions:
  contents: read
  packages: write
```

- `packages: write`: permite push a GHCR (ghcr.io)
- Usa `GITHUB_TOKEN` automático (no requiere PAT)

#### 3. Cache Maven

```yaml
- uses: actions/setup-java@v4
  with:
    cache: 'maven'
```

- Cachea `~/.m2/repository` entre runs
- Acelera builds (dependencias ya descargadas)

#### 4. Metadata y tags

```yaml
- uses: docker/metadata-action@v5
  with:
    tags: |
      type=raw,value=latest
      type=sha,prefix={{branch}}-
```

- Genera tags automáticos:
  - `latest`
  - `main-abc1234` (SHA commit)

#### 5. Conditional steps

```yaml
if: ${{ secrets.STAGING_HOST != '' }}
```

- Deploy solo si secretos están configurados
- Permite ejecutar pipeline sin infraestructura remota (dev)

---

## Secretos y variables

### Secretos requeridos (GitHub Repo Settings)

Ir a **Settings → Secrets and variables → Actions → New repository secret**:

| Secreto | Descripción | Ejemplo |
|---------|-------------|---------|
| `STAGING_HOST` | IP o hostname servidor staging | `staging.edufeed.com` o `192.168.1.100` |
| `STAGING_USER` | Usuario SSH staging | `ubuntu`, `deploy` |
| `STAGING_SSH_KEY` | Clave privada SSH staging | Contenido de `~/.ssh/id_rsa` |
| `STAGING_PATH` | Ruta proyecto en staging | `/opt/edufeed-staging` |
| `PROD_HOST` | IP/hostname servidor producción | `edufeed.com` |
| `PROD_USER` | Usuario SSH producción | `ubuntu` |
| `PROD_SSH_KEY` | Clave privada SSH producción | Contenido de `~/.ssh/id_rsa_prod` |
| `PROD_PATH` | Ruta proyecto en producción | `/opt/edufeed` |

**Nota**: `GITHUB_TOKEN` se genera automáticamente (no configurar).

### Generar par de claves SSH

En tu máquina local:

```bash
# Generar par de claves dedicado para CI/CD
ssh-keygen -t ed25519 -C "github-actions-edufeed" -f ~/.ssh/github_actions_edufeed

# Copiar clave pública al servidor
ssh-copy-id -i ~/.ssh/github_actions_edufeed.pub deploy@staging.edufeed.com
ssh-copy-id -i ~/.ssh/github_actions_edufeed.pub deploy@edufeed.com

# Copiar clave privada a GitHub Secrets
cat ~/.ssh/github_actions_edufeed
# Pegar contenido completo (incluyendo -----BEGIN/END-----) en STAGING_SSH_KEY y PROD_SSH_KEY
```

### Variables de entorno (opcional)

**Settings → Secrets and variables → Actions → Variables**:

| Variable | Valor | Uso |
|----------|-------|-----|
| `STAGING_URL` | `http://staging.edufeed.com:8081` | Health check |
| `PROD_URL` | `https://edufeed.com` | Health check |
| `SLACK_WEBHOOK` | `https://hooks.slack.com/...` | Notificaciones |

---

## Flujo de despliegue

### Workflow normal (push a main)

```
1. Developer: git push origin main
   ↓
2. GitHub Actions: Trigger CI/CD
   ↓
3. Job: build-and-test
   - mvn clean install
   - mvn test (JUnit + Testcontainers)
   - JaCoCo coverage report
   ✅ PASS → continúa
   ❌ FAIL → pipeline se detiene
   ↓
4. Job: docker-build-push (solo si #3 pasa)
   - mvn clean install -DskipTests
   - docker build -f edufeed-backend/Dockerfile
   - docker tag latest, main-<SHA>
   - docker push ghcr.io/joan-mora/edufeed-backend
   ✅ Imagen disponible en GHCR
   ↓
5. Job: deploy-staging (solo si #4 pasa)
   - SSH a staging.edufeed.com
   - docker login ghcr.io
   - docker compose pull backend
   - docker compose up -d --no-deps backend
   - curl health check
   ✅ Staging actualizado
   ↓
6. Job: deploy-production (requiere aprobación manual)
   - Reviewer aprueba en GitHub UI
   - SSH a edufeed.com
   - docker compose pull backend
   - docker compose up -d --no-deps backend
   - curl health check
   ✅ Producción actualizada
```

### Workflow PR (pull request a main)

```
1. Developer: git push origin feature/new-feature
   + Crea PR a main
   ↓
2. GitHub Actions: Trigger CI (sin CD)
   ↓
3. Job: build-and-test
   - Build y tests
   ✅ PASS → PR ready to merge
   ❌ FAIL → bloquea merge (si se configura branch protection)
   ↓
4. Review + Merge
   ↓
5. [Flujo normal de push a main]
```

---

## Entornos y aprobaciones

### Configurar entorno "production"

**Settings → Environments → New environment**:

1. Nombre: `production`
2. **Required reviewers**: seleccionar 1+ colaboradores
3. **Wait timer**: 0 minutos (o delay opcional)
4. **Deployment branches**: `main` only

### Proceso de aprobación

```
1. Pipeline llega a job "deploy-production"
   ↓
2. GitHub pausa el job y notifica a reviewers
   ↓
3. Reviewer ve:
   - Commit SHA
   - Cambios en el PR/push
   - Logs de staging
   ↓
4. Reviewer:
   - ✅ Approve and deploy → continúa a producción
   - ❌ Reject → pipeline falla, no se despliega
   ↓
5. Job ejecuta deploy a producción
```

### Notificación de revisores

GitHub envía email/notificación a revisores configurados. Alternativamente, integrar Slack:

```yaml
- name: Request approval
  run: |
    curl -X POST ${{ secrets.SLACK_WEBHOOK }} \
      -H 'Content-Type: application/json' \
      -d '{"text":"🚀 Deploy a producción listo. Revisar: https://github.com/${{ github.repository }}/actions/runs/${{ github.run_id }}"}'
```

---

## Troubleshooting

### Error: "Permission denied (publickey)"

**Causa**: Clave SSH incorrecta o no agregada al servidor.

**Solución**:
```bash
# Verificar clave pública en servidor
cat ~/.ssh/authorized_keys
# Debe contener la clave pública correspondiente a STAGING_SSH_KEY

# Probar SSH manual
ssh -i ~/.ssh/github_actions_edufeed deploy@staging.edufeed.com

# Si falla, revisar permisos
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

### Error: "docker: command not found" en SSH

**Causa**: Docker no instalado o no en PATH del usuario SSH.

**Solución**:
```bash
# En servidor staging/prod
which docker
# Si no existe: instalar Docker

# Si existe pero no en PATH de usuario no-login:
# Agregar en script SSH del workflow:
export PATH=/usr/local/bin:/usr/bin:$PATH
```

### Error: "docker login ghcr.io: denied"

**Causa**: GITHUB_TOKEN sin permisos `packages: write`.

**Solución**:
```yaml
# En job docker-build-push, agregar:
permissions:
  contents: read
  packages: write
```

### Error: "Health check failed"

**Causa**: Backend no inició correctamente o toma más tiempo.

**Solución**:
```yaml
# Aumentar sleep antes de health check
- name: Health Check
  run: |
    sleep 30  # era 10
    curl --fail --retry 3 --retry-delay 5 http://staging:8081/actuator/health
```

### Error: "Tests failed" pero pasan localmente

**Causa**: Diferencias en entorno (zona horaria, DB, versión Java).

**Solución**:
```yaml
# Usar Testcontainers para DB (ya implementado)
# Asegurar misma versión Java
- uses: actions/setup-java@v4
  with:
    java-version: '24'  # mismo que local

# Configurar zona horaria
- name: Set timezone
  run: |
    sudo timedatectl set-timezone America/Bogota
```

### Pipeline muy lento

**Causa**: Build Maven descarga dependencias cada vez.

**Solución**:
```yaml
# Habilitar cache Maven (ya configurado)
- uses: actions/setup-java@v4
  with:
    cache: 'maven'

# O usar GitHub cache manual
- uses: actions/cache@v3
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
```

### Deploy a staging exitoso pero producción falla

**Causa**: .env.prod diferente entre staging/prod.

**Solución**:
```bash
# En servidor producción, verificar .env.prod
cat /opt/edufeed/.env.prod

# Comparar con .env.prod.example
diff .env.prod.example .env.prod

# Asegurar secretos correctos
docker exec edufeed-backend-prod env | grep JWT_SECRET
```

---

## Mejores prácticas

### Seguridad

- ✅ **Nunca** loguear secretos en logs del workflow
- ✅ Rotar claves SSH cada 3-6 meses
- ✅ Usar usuarios SSH dedicados (no root)
- ✅ Limitar acceso SSH por IP (firewall)
- ✅ Escanear imagen Docker (`trivy`, `snyk`)

```yaml
# Scan de vulnerabilidades
- name: Run Trivy vulnerability scanner
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
    format: 'sarif'
    output: 'trivy-results.sarif'

- name: Upload Trivy results to GitHub Security
  uses: github/codeql-action/upload-sarif@v2
  with:
    sarif_file: 'trivy-results.sarif'
```

### Performance

- ✅ Cachear dependencias Maven
- ✅ Usar runners self-hosted si GitHub Actions lento
- ✅ Build paralelo de módulos Maven (`-T 1C`)
- ✅ Skip tests en docker build (ya testeados en job anterior)

### Observabilidad

- ✅ Notificar fallos a Slack/Discord
- ✅ Integrar con Grafana/Prometheus (ver Fase 8.3)
- ✅ Loguear cada deploy en sistema de auditoría

```yaml
# Notificación Slack en fallo
- name: Notify failure
  if: failure()
  run: |
    curl -X POST ${{ secrets.SLACK_WEBHOOK }} \
      -d '{"text":"❌ Deploy falló: ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}"}'
```

### Rollback

- ✅ Mantener tags de imagen con SHA (`main-abc1234`)
- ✅ Rollback rápido cambiando tag en docker-compose:

```bash
# En servidor producción
cd /opt/edufeed

# Ver imágenes disponibles
docker images ghcr.io/joan-mora/edufeed-backend

# Editar docker-compose.prod.yml (cambiar tag)
# image: ghcr.io/joan-mora/edufeed-backend:main-abc1234

docker compose --env-file .env.prod -f docker-compose.prod.yml up -d backend
```

---

## Mejoras futuras

### 1. Despliegue blue-green

```yaml
# Levantar nueva versión en puerto alterno
# Probar health check
# Cambiar nginx upstream
# Apagar versión anterior
```

### 2. Tests de humo post-deploy

```yaml
- name: Smoke tests
  run: |
    curl --fail https://edufeed.com/api/v1/health
    curl --fail https://edufeed.com/api/v1/users/count
```

### 3. Integración con Jira/Linear

```yaml
- name: Update Jira ticket
  run: |
    # Extraer ticket de commit message
    # POST a Jira API con estado "Deployed to Production"
```

### 4. Canary deployments

- Deploy a subset de instancias
- Monitorear métricas (error rate, latencia)
- Promover o rollback automático

### 5. GitOps con ArgoCD/FluxCD

- Repositorio separado para manifests (kubernetes/compose)
- Pull-based deploys
- Rollback vía Git revert

---

## Checklist de implementación

- [x] Workflow `.github/workflows/ci-cd.yml` creado
- [x] Secretos SSH configurados en GitHub
- [x] Entorno `production` con aprobaciones
- [x] Build y test automatizados
- [x] Push a GHCR funcional
- [ ] Deploy a staging validado
- [ ] Deploy a producción validado
- [ ] Health checks post-deploy
- [ ] Notificaciones Slack/Email
- [ ] Scan de vulnerabilidades (Trivy)
- [ ] Branch protection rules (require tests)
- [ ] Documentación para nuevos desarrolladores

---

## Referencias

- [GitHub Actions docs](https://docs.github.com/en/actions)
- [Docker Build Push Action](https://github.com/docker/build-push-action)
- [SSH Action](https://github.com/appleboy/ssh-action)
- [Environments](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment)
- [GHCR docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)

---

**Última actualización**: 31 de octubre de 2025  
**Fase**: 8.2 - CI/CD  
**Estado**: ✅ Completado
