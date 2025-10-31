# Fase 8.3: Monitoreo, Logs y Alertas

## Índice

1. [Introducción](#introducción)
2. [Arquitectura de observabilidad](#arquitectura-de-observabilidad)
3. [Stack tecnológico](#stack-tecnológico)
4. [Configuración de Prometheus](#configuración-de-prometheus)
5. [Dashboards de Grafana](#dashboards-de-grafana)
6. [Alertas (Alertmanager)](#alertas-alertmanager)
7. [Logging con ELK Stack](#logging-con-elk-stack)
8. [Métricas de aplicación](#métricas-de-aplicación)
9. [Validación con k6](#validación-con-k6)
10. [Troubleshooting](#troubleshooting)
11. [Mejores prácticas](#mejores-prácticas)

---

## Introducción

La observabilidad permite comprender el estado interno del sistema a partir de sus salidas (métricas, logs, traces). Esta fase implementa monitoreo completo de infraestructura, aplicación y negocio.

### Objetivos

- ✅ Recolectar métricas HTTP (latencia, throughput, errores) con Prometheus
- ✅ Visualizar KPIs en dashboards Grafana
- ✅ Alertar sobre SLA (p95 < 500ms, error rate < 1%)
- ✅ Centralizar logs de aplicación y contenedores con ELK
- ✅ Validar alertas con tests de carga (k6)

### Beneficios

- **Proactividad**: detectar problemas antes que usuarios
- **Debugging**: reducir MTTR con logs centralizados
- **Capacidad**: planificar escalamiento basado en métricas reales
- **SLA**: garantizar cumplimiento de acuerdos de servicio
- **Auditoría**: trazabilidad de errores y eventos

### SLOs (Service Level Objectives)

| Métrica | Objetivo | Alerta |
|---------|----------|--------|
| Latencia p95 | < 500ms | > 500ms por 5 min |
| Error rate (5xx) | < 1% | > 1% por 2 min |
| Uptime | > 99.9% | Down por 1 min |
| Throughput | > 100 req/s | - |

---

## Arquitectura de observabilidad

```
┌──────────────────────────────────────────────────────────────┐
│                        Docker Host                           │
│                                                              │
│  ┌─────────────┐      ┌──────────────┐    ┌──────────────┐  │
│  │   Backend   │─────▶│  Prometheus  │───▶│   Grafana    │  │
│  │  :8080      │      │    :9090     │    │    :3000     │  │
│  │/actuator/   │      │              │    │              │  │
│  │prometheus   │      │  Scrape 15s  │    │  Dashboards  │  │
│  └─────────────┘      └──────┬───────┘    │  Alerting    │  │
│         │                    │            └──────────────┘  │
│         │ logs               │                    ▲          │
│         ▼                    │ rules              │          │
│  ┌─────────────┐             ▼                    │          │
│  │  Filebeat   │      ┌──────────────┐            │          │
│  │             │─────▶│ Alertmanager │────────────┘          │
│  └─────┬───────┘      │    :9093     │                       │
│        │              │              │                       │
│        │              │ Slack/Email  │                       │
│        ▼              └──────────────┘                       │
│  ┌─────────────┐                                             │
│  │  Logstash   │                                             │
│  │   :5044     │                                             │
│  └─────┬───────┘                                             │
│        │                                                     │
│        ▼                                                     │
│  ┌─────────────┐      ┌──────────────┐                      │
│  │Elasticsearch│─────▶│    Kibana    │                      │
│  │   :9200     │      │    :5601     │                      │
│  └─────────────┘      └──────────────┘                      │
└──────────────────────────────────────────────────────────────┘
```

### Flujo de datos

**Métricas**:
1. Backend expone `/actuator/prometheus` (formato Prometheus)
2. Prometheus scrapea cada 15s
3. Evalúa reglas de alertas (rules.yml)
4. Grafana consulta Prometheus y renderiza dashboards
5. Alertmanager notifica vía Slack/Email si se dispara alerta

**Logs**:
1. Backend escribe logs a stdout/stderr (Spring Boot Logback)
2. Filebeat recolecta logs de contenedores Docker
3. Logstash parsea y enriquece logs
4. Elasticsearch indexa logs
5. Kibana visualiza y permite búsquedas

---

## Stack tecnológico

| Componente | Versión | Puerto | Propósito |
|------------|---------|--------|-----------|
| **Prometheus** | latest | 9090 | Time-series DB, scraping |
| **Grafana** | latest | 3000 | Visualización, dashboards |
| **Alertmanager** | latest | 9093 | Routing de alertas |
| **Elasticsearch** | 8.11.0 | 9200 | Indexación de logs |
| **Logstash** | 8.11.0 | 5044 | Procesamiento de logs |
| **Kibana** | 8.11.0 | 5601 | UI de Elasticsearch |
| **Filebeat** | 8.11.0 | - | Recolector de logs |
| **k6** | latest | - | Load testing |

---

## Configuración de Prometheus

### 1. Prometheus config

**Ubicación**: `observability/prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'edufeed-prod'
    environment: 'production'

# Alertmanager
alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

# Reglas de alertas
rule_files:
  - /etc/prometheus/rules.yml

# Scrape targets
scrape_configs:
  - job_name: 'edufeed-backend'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - backend:8080
    scrape_interval: 15s
    scrape_timeout: 10s
```

**Características**:
- **scrape_interval**: cada 15s (balance entre granularidad y carga)
- **external_labels**: identifica origen de métricas (multi-cluster)
- **metrics_path**: endpoint Spring Boot Actuator
- **targets**: nombre del servicio en Docker Compose (`backend`)

### 2. Reglas de alertas

**Ubicación**: `observability/prometheus/rules.yml`

```yaml
groups:
  - name: edufeed_http
    interval: 30s
    rules:
      # Latencia p95 > 500ms
      - alert: HighLatencyP95
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket{job="edufeed-backend"}[5m])) by (le, uri)
          ) > 0.5
        for: 5m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "Alta latencia p95 en {{ $labels.uri }}"
          description: "p95 es {{ $value | humanizeDuration }} (umbral: 500ms)"

      # Error rate 5xx > 1%
      - alert: HighErrorRate5xx
        expr: |
          (
            sum(rate(http_server_requests_seconds_count{job="edufeed-backend",status=~"5.."}[2m]))
            /
            sum(rate(http_server_requests_seconds_count{job="edufeed-backend"}[2m]))
          ) > 0.01
        for: 2m
        labels:
          severity: critical
          team: backend
        annotations:
          summary: "Error rate 5xx > 1%"
          description: "{{ $value | humanizePercentage }} de requests con errores 5xx"

      # Backend down
      - alert: BackendDown
        expr: up{job="edufeed-backend"} == 0
        for: 1m
        labels:
          severity: critical
          team: infrastructure
        annotations:
          summary: "Backend no responde"
          description: "El backend no responde al scraping por más de 1 minuto"
```

**Parámetros clave**:
- **expr**: consulta PromQL que dispara la alerta
- **for**: duración antes de disparar (evita falsos positivos)
- **severity**: warning (aviso) o critical (urgente)
- **annotations**: mensaje legible para humanos

### 3. Backend: habilitar métricas detalladas

**Ubicación**: `edufeed-backend/src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
      slo:
        http.server.requests: 50ms,100ms,200ms,500ms,1s,2s
    tags:
      application: ${spring.application.name}
      environment: ${SPRING_PROFILES_ACTIVE:dev}
```

**Características**:
- **percentiles-histogram**: exporta histogramas para calcular p95, p99
- **slo**: buckets personalizados para latencia (50ms, 100ms, 500ms...)
- **tags**: añade labels `application` y `environment` a métricas

---

## Dashboards de Grafana

### 1. Provisioning de datasource

**Ubicación**: `observability/grafana/provisioning/datasources/datasource.yml`

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    uid: PROM_DS
    isDefault: true
    editable: false

  - name: Elasticsearch
    type: elasticsearch
    access: proxy
    url: http://elasticsearch:9200
    database: "logstash-*"
    jsonData:
      timeField: "@timestamp"
      esVersion: "8.11.0"
      logMessageField: message
      logLevelField: level
```

**Características**:
- **uid: PROM_DS**: identificador fijo para referenciar en dashboards
- **isDefault**: Prometheus como datasource por defecto
- **editable: false**: evita cambios accidentales en UI

### 2. Provisioning de dashboards

**Ubicación**: `observability/grafana/provisioning/dashboards/dashboard.yml`

```yaml
apiVersion: 1

providers:
  - name: 'EduFeed Dashboards'
    orgId: 1
    folder: 'EduFeed'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
      foldersFromFilesStructure: false
```

### 3. Dashboard HTTP (JSON)

**Ubicación**: `observability/grafana/dashboards/edufeed-http.json`

**Paneles incluidos**:

1. **Request Rate** (queries/s):
   ```promql
   sum(rate(http_server_requests_seconds_count{job="edufeed-backend"}[1m]))
   ```

2. **Latencia p50, p95, p99**:
   ```promql
   histogram_quantile(0.95,
     sum(rate(http_server_requests_seconds_bucket{job="edufeed-backend"}[5m])) by (le, uri)
   )
   ```

3. **Error rate por código HTTP**:
   ```promql
   sum(rate(http_server_requests_seconds_count{job="edufeed-backend"}[1m])) by (status)
   ```

4. **Top 5 endpoints más lentos**:
   ```promql
   topk(5,
     histogram_quantile(0.95,
       sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
     )
   )
   ```

5. **JVM Memory Usage**:
   ```promql
   jvm_memory_used_bytes{job="edufeed-backend",area="heap"}
   ```

6. **Conexiones DB (HikariCP)**:
   ```promql
   hikaricp_connections_active{job="edufeed-backend"}
   ```

**Visualización**:
- Time series para latencia/throughput
- Gauge para error rate
- Table para top endpoints
- Bar chart para HTTP status distribution

---

## Alertas (Alertmanager)

### 1. Configuración Alertmanager

**Ubicación**: `observability/alertmanager/alertmanager.yml`

```yaml
global:
  resolve_timeout: 5m
  slack_api_url: '${SLACK_WEBHOOK_URL}'

route:
  receiver: 'default'
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 5m
  repeat_interval: 12h

  routes:
    - match:
        severity: critical
      receiver: 'critical-alerts'
      repeat_interval: 1h

    - match:
        severity: warning
      receiver: 'warning-alerts'
      repeat_interval: 6h

receivers:
  - name: 'default'
    slack_configs:
      - channel: '#edufeed-alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

  - name: 'critical-alerts'
    slack_configs:
      - channel: '#edufeed-critical'
        title: '🚨 CRITICAL: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}\n{{ .Annotations.description }}{{ end }}'
        send_resolved: true

  - name: 'warning-alerts'
    slack_configs:
      - channel: '#edufeed-alerts'
        title: '⚠️ WARNING: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname']
```

**Características**:
- **group_by**: agrupa alertas similares (evita spam)
- **repeat_interval**: frecuencia de reenvío si no se resuelve
- **inhibit_rules**: critical silencia warning del mismo alert

### 2. Provisioning de alertas en Grafana

**Ubicación**: `observability/grafana/provisioning/alerting/contactpoints.yaml`

```yaml
apiVersion: 1

contactPoints:
  - orgId: 1
    name: slack-edufeed
    receivers:
      - uid: slack-edufeed-uid
        type: slack
        settings:
          url: ${SLACK_WEBHOOK_URL}
          text: |
            {{ range .Alerts }}
              *{{ .Labels.alertname }}*
              {{ .Annotations.summary }}
              {{ .Annotations.description }}
            {{ end }}
        disableResolveMessage: false
```

**Políticas de notificación** (`policies.yaml`):

```yaml
apiVersion: 1

policies:
  - orgId: 1
    receiver: slack-edufeed
    group_by: ['alertname', 'grafana_folder']
    group_wait: 10s
    group_interval: 5m
    repeat_interval: 12h
```

**Reglas de alerta** (ejemplo: `rules/http-latency.yaml`):

```yaml
apiVersion: 1

groups:
  - orgId: 1
    name: HTTP Performance
    folder: EduFeed Alerts
    interval: 1m
    rules:
      - uid: http-latency-p95
        title: High Latency (p95 > 500ms)
        condition: A
        data:
          - refId: A
            queryType: ''
            relativeTimeRange:
              from: 600
              to: 0
            datasourceUid: PROM_DS
            model:
              expr: |
                histogram_quantile(0.95,
                  sum(rate(http_server_requests_seconds_bucket{job="edufeed-backend"}[5m])) by (le)
                ) > 0.5
              instant: false
              intervalMs: 15000
              maxDataPoints: 43200
        noDataState: NoData
        execErrState: Alerting
        for: 5m
        annotations:
          summary: Latencia p95 por encima del SLO
          description: "p95: {{ $values.A.Value }}s (umbral: 500ms)"
        labels:
          severity: warning
```

### 3. Configurar Slack Webhook

1. Ir a [Slack API](https://api.slack.com/apps)
2. Crear app → Incoming Webhooks → Add New Webhook
3. Seleccionar canal (#edufeed-alerts)
4. Copiar Webhook URL

**En docker-compose.observability.yml**:

```yaml
services:
  grafana:
    environment:
      GF_ALERTING_SLACK_WEBHOOK_URL: ${SLACK_WEBHOOK_URL}
```

**En .env.prod**:

```bash
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX
```

---

## Logging con ELK Stack

### 1. Logstash pipeline

**Ubicación**: `observability/logstash/pipeline/logstash.conf`

```ruby
input {
  beats {
    port => 5044
  }
}

filter {
  # Parse JSON logs de Spring Boot
  if [container][name] =~ /backend/ {
    json {
      source => "message"
      target => "log"
    }

    # Extraer campos comunes
    mutate {
      add_field => {
        "level" => "%{[log][level]}"
        "logger" => "%{[log][logger_name]}"
        "thread" => "%{[log][thread_name]}"
        "app" => "edufeed-backend"
      }
    }

    # Parse stack traces
    if [log][stack_trace] {
      mutate {
        add_field => { "has_exception" => "true" }
      }
    }

    # Grok para logs no-JSON (fallback)
    if "_jsonparsefailure" in [tags] {
      grok {
        match => {
          "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{DATA:logger} - %{GREEDYDATA:message}"
        }
      }
    }
  }

  # Enriquecer con metadata
  mutate {
    add_field => {
      "[@metadata][index]" => "logstash-%{+YYYY.MM.dd}"
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "%{[@metadata][index]}"
  }

  # Debug (opcional, comentar en prod)
  # stdout { codec => rubydebug }
}
```

**Características**:
- **beats input**: recibe logs desde Filebeat
- **json filter**: parsea JSON de Spring Boot (Logback JSON encoder)
- **grok fallback**: para logs plain text
- **enriquecimiento**: añade campos para búsqueda/filtrado

### 2. Filebeat config

**Ubicación**: `observability/filebeat/filebeat.yml`

```yaml
filebeat.inputs:
  - type: container
    paths:
      - '/var/lib/docker/containers/*/*.log'
    processors:
      - add_docker_metadata:
          host: "unix:///var/run/docker.sock"
      - decode_json_fields:
          fields: ["message"]
          target: ""
          overwrite_keys: true

output.logstash:
  hosts: ["logstash:5044"]

logging.level: info
```

**Características**:
- **type: container**: autodescubre logs de contenedores
- **add_docker_metadata**: añade labels Docker (container.name, image)
- **decode_json_fields**: parsea JSON en campo `message`

### 3. Elasticsearch index templates

Elasticsearch crea índices automáticamente con patrón `logstash-YYYY.MM.dd`. Para optimizar:

```bash
# Aplicar template (opcional)
curl -X PUT "http://localhost:9200/_index_template/logstash" -H 'Content-Type: application/json' -d'
{
  "index_patterns": ["logstash-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    },
    "mappings": {
      "properties": {
        "@timestamp": { "type": "date" },
        "level": { "type": "keyword" },
        "logger": { "type": "keyword" },
        "message": { "type": "text" },
        "has_exception": { "type": "boolean" }
      }
    }
  }
}
'
```

### 4. Kibana dashboards

**Acceso**: http://localhost:5601

**Setup inicial**:
1. Crear index pattern: `logstash-*` con time field `@timestamp`
2. Discover → filtrar por `container.name: edufeed-backend-prod`
3. Crear visualizaciones:
   - Bar chart: logs por nivel (INFO, WARN, ERROR)
   - Line chart: logs/minuto
   - Table: últimos errores con stack trace

**Búsquedas útiles**:

```
# Errores últimas 24h
level: ERROR AND @timestamp >= now-24h

# Excepciones con stack trace
has_exception: true

# Logs de endpoint específico
message: "/api/v1/access/check"

# Logs de un usuario
log.user_id: 12345
```

---

## Métricas de aplicación

### Métricas custom (Spring Boot)

**Ubicación**: `edufeed-backend/.../config/MetricsConfig.java` (crear)

```java
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags(
                "application", "edufeed-backend",
                "environment", System.getenv("SPRING_PROFILES_ACTIVE")
            );
    }

    @Bean
    public Counter accessGrantedCounter(MeterRegistry registry) {
        return Counter.builder("edufeed.access.granted")
            .description("Accesos concedidos")
            .tag("type", "biometric")
            .register(registry);
    }

    @Bean
    public Counter accessDeniedCounter(MeterRegistry registry) {
        return Counter.builder("edufeed.access.denied")
            .description("Accesos denegados")
            .tag("reason", "no_payment")
            .register(registry);
    }
}
```

**Uso en controllers**:

```java
@RestController
@RequestMapping("/api/v1/access")
public class AccessController {

    private final Counter accessGrantedCounter;
    private final Counter accessDeniedCounter;

    @PostMapping("/check")
    public ResponseEntity<?> checkAccess(@RequestBody AccessRequest req) {
        if (hasValidPayment(req.getUserId())) {
            accessGrantedCounter.increment();
            return ok("Access granted");
        } else {
            accessDeniedCounter.increment();
            return status(FORBIDDEN).body("No payment");
        }
    }
}
```

**Consultas Prometheus**:

```promql
# Tasa de accesos concedidos/s
rate(edufeed_access_granted_total[5m])

# Ratio denegados/total
rate(edufeed_access_denied_total[5m]) / rate(edufeed_access_granted_total[5m])
```

---

## Validación con k6

### Script de carga (smoke test)

**Ubicación**: `observability/k6/p95-smoke.js`

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // ramp-up a 50 usuarios
    { duration: '2m', target: 50 },    // mantener 50 usuarios
    { duration: '30s', target: 100 },  // ramp-up a 100
    { duration: '2m', target: 100 },   // mantener 100
    { duration: '30s', target: 0 },    // ramp-down
  ],
  thresholds: {
    'http_req_duration{expected_response:true}': ['p(95)<500'], // p95 < 500ms
    'http_req_failed': ['rate<0.01'],  // error rate < 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // Health check
  let res = http.get(`${BASE_URL}/actuator/health`);
  check(res, {
    'health is 200': (r) => r.status === 200,
  });

  sleep(1);

  // Endpoint de negocio (ej. listar usuarios)
  res = http.get(`${BASE_URL}/api/v1/users`, {
    headers: { 'Authorization': 'Bearer YOUR_TEST_JWT' },
  });
  check(res, {
    'users list is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(2);
}
```

**Ejecutar**:

```powershell
# Instalar k6 (Windows con Chocolatey)
choco install k6

# Ejecutar smoke test
k6 run observability/k6/p95-smoke.js

# Con variables de entorno
k6 run --env BASE_URL=http://staging.edufeed.com:8081 observability/k6/p95-smoke.js
```

**Salida esperada**:

```
     ✓ health is 200
     ✓ users list is 200
     ✓ response time < 500ms

     checks.........................: 100.00% ✓ 1500      ✗ 0
     http_req_duration..............: avg=120ms  min=50ms med=110ms max=450ms p(95)=380ms
     http_req_failed................: 0.00%   ✓ 0        ✗ 1500
```

### Validar alertas

1. Ejecutar k6 con carga alta para disparar alerta:

```powershell
k6 run --vus 500 --duration 10m observability/k6/p95-smoke.js
```

2. Verificar en Prometheus (http://localhost:9090):
   - Alerts → ver `HighLatencyP95` FIRING

3. Verificar en Grafana (http://localhost:3000):
   - Alerting → Alert rules → ver estado ALERTING

4. Verificar notificación Slack:
   - Canal #edufeed-alerts debe recibir mensaje

---

## Troubleshooting

### Prometheus no scrapea backend

**Síntoma**: Target "edufeed-backend" DOWN en http://localhost:9090/targets

**Diagnóstico**:
```powershell
# Verificar backend expone /actuator/prometheus
curl http://localhost:8080/actuator/prometheus

# Verificar red Docker
docker network inspect edufeed_default
# backend y prometheus deben estar en misma red

# Verificar logs Prometheus
docker compose -f docker-compose.observability.yml logs prometheus | Select-String "backend"
```

**Solución**:
- Asegurar `management.endpoints.web.exposure.include` tiene `prometheus`
- Verificar firewall/security group no bloquea puerto 8080
- Usar `backend:8080` (nombre servicio Docker) no `localhost`

### Grafana no muestra datos

**Síntoma**: Dashboard vacío o "No data"

**Diagnóstico**:
```powershell
# Verificar datasource en Grafana
# UI: Connections → Data sources → Prometheus → Save & test

# Verificar query manual en Explore
# Paste query: up{job="edufeed-backend"}
```

**Solución**:
- Revisar UID de datasource (`PROM_DS`) coincida en dashboard JSON
- Verificar time range (últimas 24h, no "Last 5 minutes" si no hay tráfico)
- Confirmar métricas existen: http://localhost:9090/graph → ejecutar query

### Alertas no se disparan

**Síntoma**: Métrica supera umbral pero no hay alerta

**Diagnóstico**:
```promql
# En Prometheus, evaluar manualmente la expresión de la regla
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket{job="edufeed-backend"}[5m])) by (le)
)

# Verificar reglas cargadas
# http://localhost:9090/rules
```

**Solución**:
- Verificar `for: 5m` → regla debe cumplirse por 5 min antes de disparar
- Revisar sintaxis PromQL (usar Prometheus UI para validar)
- Confirmar Alertmanager está configurado en prometheus.yml

### Filebeat no envía logs

**Síntoma**: Kibana no muestra logs de backend

**Diagnóstico**:
```powershell
# Logs Filebeat
docker compose -f docker-compose.observability.yml logs filebeat | Select-String "error"

# Verificar Logstash recibe beats
docker compose logs logstash | Select-String "Starting Logstash"

# Test conexión
docker exec edufeed-filebeat filebeat test output
```

**Solución**:
- Verificar ruta logs Docker (`/var/lib/docker/containers`) montada
- En Windows, Filebeat debe correr en host Linux o WSL2 (Docker Desktop usa VM)
- Confirmar Logstash escucha en puerto 5044: `netstat -ano | findstr 5044`

### Elasticsearch OOM (Out of Memory)

**Síntoma**: Contenedor elasticsearch reinicia constantemente

**Solución**:
```yaml
# En docker-compose.observability.yml
services:
  elasticsearch:
    environment:
      - "ES_JAVA_OPTS=-Xms512m -Xmx1g"  # reducir heap
    deploy:
      resources:
        limits:
          memory: 2G
```

---

## Mejores prácticas

### Métricas

- ✅ Usar labels con cardinalidad baja (no incluir user_id, request_id)
- ✅ Prefijar métricas custom (`edufeed.*`)
- ✅ Documentar métricas en código (`.description()`)
- ✅ Evitar alta frecuencia de scrape (15s es razonable)

### Logs

- ✅ Usar structured logging (JSON)
- ✅ Incluir trace_id para correlación con métricas
- ✅ NO loguear datos sensibles (passwords, PII sin enmascarar)
- ✅ Rotar índices Elasticsearch (ILM policy: 7 días → delete)

```yaml
# Elasticsearch ILM policy (opcional)
PUT _ilm/policy/logstash-policy
{
  "policy": {
    "phases": {
      "hot": { "actions": {} },
      "delete": {
        "min_age": "7d",
        "actions": { "delete": {} }
      }
    }
  }
}
```

### Alertas

- ✅ Definir SLOs claros (p95 < 500ms, uptime > 99.9%)
- ✅ Usar `for` para evitar flapping (5m mínimo)
- ✅ Severity: `warning` (no urgente) vs `critical` (pager)
- ✅ Runbooks en annotations (links a troubleshooting)

```yaml
annotations:
  runbook_url: https://wiki.edufeed.com/runbooks/high-latency
```

### Dashboards

- ✅ Un dashboard por audiencia (Dev, Ops, Business)
- ✅ Incluir variables para filtrar (environment, endpoint)
- ✅ Usar colores semánticos (verde=OK, amarillo=warning, rojo=critical)
- ✅ Compartir enlaces con filtros pre-aplicados

---

## Checklist de implementación

- [x] Prometheus scrapeando backend
- [x] Grafana datasources provisionados
- [x] Dashboard HTTP con p95, error rate, throughput
- [x] Reglas de alerta (latency, 5xx, down)
- [x] Alertmanager configurado
- [ ] Slack webhook funcionando
- [x] ELK stack levantado
- [x] Filebeat recolectando logs
- [x] Kibana index pattern creado
- [x] k6 smoke test creado
- [ ] Alertas validadas con k6
- [ ] ILM policy en Elasticsearch (retención)
- [ ] Dashboards de negocio (accesos, pagos)

---

## Referencias

- [Prometheus docs](https://prometheus.io/docs/)
- [Grafana provisioning](https://grafana.com/docs/grafana/latest/administration/provisioning/)
- [Alertmanager config](https://prometheus.io/docs/alerting/latest/configuration/)
- [ELK Stack guide](https://www.elastic.co/guide/index.html)
- [k6 docs](https://k6.io/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer](https://micrometer.io/docs)

---

**Última actualización**: 31 de octubre de 2025  
**Fase**: 8.3 - Monitoreo y Logs  
**Estado**: ✅ Completado (validación pendiente)
