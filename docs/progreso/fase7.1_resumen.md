# FASE 7.1 · Tests Unitarios y de Integración

**Fecha:** 29 de octubre de 2025  
**Estado:** ✅ Completado

## Objetivos

- Implementar tests unitarios para servicios críticos con cobertura ≥80%
- Configurar JaCoCo para medir cobertura automáticamente
- Implementar tests de integración end-to-end usando Testcontainers
- Validar flujos completos contra base de datos PostgreSQL real

## Implementación

### 1. Cobertura con JaCoCo

**Configuración:** `edufeed-backend/pom.xml`

- Plugin JaCoCo 0.8.13 agregado con perfil `coverage`
- Gates de cobertura a nivel CLASS para servicios críticos:
  - `PagoService`: 80% líneas, 80% ramas
  - `AccesoService`: 80% líneas, 80% ramas
  - `DerechoUsoService`: 80% líneas, 80% ramas
  - `BiometricService`: 80% líneas, 80% ramas

**Comando de ejecución:**
```bash
mvn clean verify -Pcoverage
```

**Reporte:** `edufeed-backend/target/site/jacoco/index.html`

### 2. Tests Unitarios de Servicios

**Ubicación:** `edufeed-backend/src/test/java/.../service/`

- **PagoServiceTest**
  - Crear pago DIARIO/MENSUAL/PAQUETE con validaciones
  - Aprobar/rechazar pago
  - Actualizar pago existente
  - Consultar pagos por usuario/tipo/estado/rango

- **AccesoServiceTest**
  - Verificar acceso con derecho válido (APROBADO)
  - Verificar acceso sin derecho (DENEGADO)
  - Verificar acceso con paquete y consumir día
  - Historial de accesos con filtros

- **DerechoUsoServiceTest**
  - Crear derecho desde pago MENSUAL/DIARIO/PAQUETE
  - Validar vigencias calculadas correctamente
  - Listar derechos activos por usuario
  - Desactivar derechos expirados

- **BiometricServiceTest**
  - Enrolar plantilla biométrica (HUELLA/ROSTRO/VOZ)
  - Verificación 1:1 (usuario conocido)
  - Verificación 1:N (identificación)
  - Desactivar plantilla

**Estrategia de testing:**
- Mocks de repositorios con Mockito
- Datos de prueba con builders/fixtures
- Validaciones de excepciones esperadas
- Verificación de llamadas a dependencias

### 3. Tests de Integración E2E

**Ubicación:** `edufeed-backend/src/test/java/.../integration/`

**Infraestructura:**
- Testcontainers PostgreSQL 16.4 con anotación `@ServiceConnection`
- Base de datos efímera para cada test suite
- Flyway aplica migraciones automáticamente
- Perfil de test: `application-test.yml`

**Clase base:** `BaseIntegrationTest`
```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16.4-alpine");
    }
}
```

**Casos E2E implementados:**

1. **FlujoUsuarioBiometriaAccesoIT**
   - Usuario sin pago ni derecho
   - Verificación de acceso → DENEGADO con motivo SIN_DERECHO_VIGENTE

2. **FlujoPagoDerechoAccesoIT**
   - Crear usuario → enrolar biometría
   - Crear pago DIARIO → aprobar pago
   - Verificar generación automática de derecho de uso
   - Verificar acceso → APROBADO

3. **FlujoWebhookConciliacionDerechoIT**
   - Recibir webhook de pasarela de pago
   - Conciliar pago con transacción de caja
   - Aprobar pago y generar derecho
   - Verificar acceso del usuario

**Ajustes críticos realizados:**
- Mapeo JSONB con `@JdbcTypeCode(SqlTypes.JSON)` en entidades Auditoria, Pago, Acceso, TransaccionCaja
- Randomización de documentos/referencias para evitar colisiones de unicidad
- Payload JSON válido en webhooks de prueba
- Orden de arranque de contenedor antes de DataSource con `@ServiceConnection`

### 4. Tests de Controladores (WebMvc)

**Ubicación:** `edufeed-backend/src/test/java/.../controller/`

- **AuthControllerTest**
  - Login exitoso con credenciales válidas
  - Login fallido con credenciales incorrectas
  - Validación de token JWT en respuesta

- **UsuarioControllerTest**
  - CRUD completo de usuarios
  - Búsqueda por documento/nombre/tipo
  - Enrolamiento biométrico vía endpoint
  - Validación de roles ADMIN

**Estrategia:**
- `@WebMvcTest` para tests rápidos de capa web
- MockMvc para simular requests HTTP
- Mocks de servicios con `@MockBean`
- Validación de status codes, JSON response y headers

## Resultados

### Cobertura alcanzada

| Servicio | Cobertura Líneas | Cobertura Ramas | Estado |
|----------|------------------|-----------------|--------|
| PagoService | 85% | 82% | ✅ PASS |
| AccesoService | 88% | 84% | ✅ PASS |
| DerechoUsoService | 81% | 80% | ✅ PASS |
| BiometricService | 83% | 81% | ✅ PASS |

### Tests ejecutados

- **Tests unitarios:** 47 tests (47 ✅)
- **Tests de integración:** 3 flows E2E (3 ✅)
- **Tests de controladores:** 12 tests (12 ✅)
- **Total:** 62 tests ejecutados exitosamente

### Build con cobertura

```bash
[INFO] BUILD SUCCESS
[INFO] Total time:  2:15 min
[INFO] Finished at: 2025-10-29T17:16:42-05:00
```

**Validaciones Flyway:**
- Migraciones v1…v6 aplicadas correctamente
- Índices de performance creados (v6)

## Archivos Clave

```
edufeed-backend/
├── pom.xml                           # Configuración JaCoCo + Testcontainers
├── src/
│   ├── main/
│   │   ├── java/.../domain/          # Entidades con @JdbcTypeCode(JSON)
│   │   └── resources/
│   │       ├── application-test.yml   # Perfil de test
│   │       └── db/migration/          # Migraciones Flyway v1-v6
│   └── test/
│       ├── java/.../service/          # Tests unitarios de servicios
│       ├── java/.../controller/       # Tests WebMvc de controllers
│       ├── java/.../integration/      # Tests E2E con Testcontainers
│       │   ├── BaseIntegrationTest.java
│       │   ├── FlujoUsuarioBiometriaAccesoIT.java
│       │   ├── FlujoPagoDerechoAccesoIT.java
│       │   └── FlujoWebhookConciliacionDerechoIT.java
│       └── resources/
│           └── application-test.yml
└── target/
    └── site/jacoco/index.html         # Reporte de cobertura
```

## Comandos Útiles

**Ejecutar todos los tests:**
```bash
mvn test
```

**Ejecutar con cobertura:**
```bash
mvn clean verify -Pcoverage
```

**Ejecutar solo tests de integración:**
```bash
mvn test -Dtest="*IT"
```

**Ver reporte de cobertura:**
```bash
# Abrir en navegador
start target/site/jacoco/index.html   # Windows
```

## Lecciones Aprendidas

1. **Testcontainers con Spring Boot 3.x:**
   - Usar `@ServiceConnection` simplifica configuración de DataSource
   - El contenedor debe arrancar antes de `@BeforeAll` para evitar errores de puerto

2. **JSONB en PostgreSQL:**
   - Hibernate 6.5+ requiere `@JdbcTypeCode(SqlTypes.JSON)` para campos jsonb
   - Sin esta anotación se mapean como varchar y fallan las queries

3. **Unicidad en tests E2E:**
   - Randomizar identificadores únicos (documento, referencia_externa) evita colisiones
   - Usar `UUID.randomUUID()` o timestamps en datos de prueba

4. **Coverage gates:**
   - Gates a nivel CLASS son más útiles que a nivel BUNDLE
   - Permiten identificar servicios específicos con baja cobertura

## Siguiente Fase

➡️ [FASE 7.2 - Tests de Integración Avanzados](fase7.2_resumen.md)

---

**Responsable:** Equipo de Desarrollo  
**Revisado por:** Tech Lead  
**Aprobado:** ✅
