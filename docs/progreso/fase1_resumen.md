# FASE 1: Capa de Dominio y Persistencia

**Periodo:** Semanas 2-4  
**Estado:** ✅ COMPLETADO  
**Fecha de finalización:** 20 de octubre de 2025

---

## 📋 Índice

1. [Objetivos](#objetivos)
2. [Entregables](#entregables)
3. [Modelo de Dominio](#modelo-de-dominio)
4. [Repositorios Spring Data](#repositorios-spring-data)
5. [DTOs y Mappers](#dtos-y-mappers)
6. [Migraciones de Base de Datos](#migraciones-de-base-de-datos)
7. [Configuración JPA](#configuración-jpa)
8. [Tests de Repositorio](#tests-de-repositorio)
9. [API Base Preparada](#api-base-preparada)
10. [Siguiente Fase](#siguiente-fase)

---

## 🎯 Objetivos

### Completados:
- ✅ Crear entidades JPA para las 13 tablas del schema
- ✅ Mapear relaciones (@OneToMany, @ManyToOne, @ManyToMany)
- ✅ Configurar enums para tipos de dominio
- ✅ Implementar repositorios Spring Data JPA
- ✅ Crear DTOs con Bean Validation
- ✅ Implementar mappers (entity ↔ DTO)
- ✅ Escribir tests de repositorio con BD real
- ✅ Validar schema con Hibernate (ddl-auto: validate)
- ✅ Preparar controladores base para FASE 2

---

## 📦 Entregables

### **Parte 1: Modelo de Dominio**

#### Entidades JPA creadas:

```
edufeed-backend/src/main/java/co/cellano/edufeed/backend/model/
├── Usuario.java                        [NUEVO]
├── PlantillaBiometrica.java            [NUEVO]
├── Pago.java                           [NUEVO]
├── PaquetePago.java                    [NUEVO]
├── DerechoUso.java                     [NUEVO]
├── Acceso.java                         [NUEVO]
├── UsoPaquete.java                     [NUEVO]
├── Auditoria.java                      [NUEVO]
├── Rol.java                            [NUEVO]
├── UsuarioRol.java                     [NUEVO]
├── UsuarioRolId.java                   [NUEVO - @Embeddable]
├── TransaccionCaja.java                [NUEVO]
├── CalendarioServicio.java             [NUEVO]
└── enums/
    ├── TipoUsuario.java                [NUEVO]
    ├── Modalidad.java                  [NUEVO]
    ├── TipoPago.java                   [NUEVO]
    ├── EstadoPago.java                 [NUEVO]
    └── EstadoAcceso.java               [NUEVO]
```

---

### **Parte 2: Repositorios Spring Data**

#### Interfaces creadas:

```
edufeed-backend/src/main/java/co/cellano/edufeed/backend/repository/
├── UsuarioRepository.java              [NUEVO]
├── PlantillaBiometricaRepository.java  [NUEVO]
├── PagoRepository.java                 [NUEVO]
├── PaquetePagoRepository.java          [NUEVO]
├── DerechoUsoRepository.java           [NUEVO]
├── AccesoRepository.java               [NUEVO]
├── UsoPaqueteRepository.java           [NUEVO]
├── AuditoriaRepository.java            [NUEVO]
├── RolRepository.java                  [NUEVO]
├── UsuarioRolRepository.java           [NUEVO]
├── TransaccionCajaRepository.java      [NUEVO]
└── CalendarioServicioRepository.java   [NUEVO]
```

---

### **Parte 3: DTOs y Mappers**

#### DTOs creados:

```
edufeed-backend/src/main/java/co/cellano/edufeed/backend/dto/
├── UsuarioDto.java                     [NUEVO]
├── PagoDto.java                        [NUEVO]
├── DerechoUsoDto.java                  [NUEVO]
└── AccesoDto.java                      [NUEVO]
```

#### Mappers implementados:

```
edufeed-backend/src/main/java/co/cellano/edufeed/backend/mapper/
├── UsuarioMapper.java                  [NUEVO]
├── PagoMapper.java                     [NUEVO]
├── DerechoUsoMapper.java               [NUEVO]
└── AccesoMapper.java                   [NUEVO]
```

---

### **Parte 4: API Base (preparación para FASE 2)**

#### Controladores iniciales:

```
edufeed-backend/src/main/java/co/cellano/edufeed/backend/controller/
├── UsuarioController.java              [NUEVO]
└── PagoController.java                 [NUEVO]
```

#### Servicios iniciales:

```
edufeed-backend/src/main/java/co/cellano/edufeed/backend/service/
├── UsuarioService.java                 [NUEVO]
└── PagoService.java                    [NUEVO]
```

---

## 🏗️ Modelo de Dominio

### **1. Usuario**

**Entidad:** `co.cellano.edufeed.backend.model.Usuario`

**Tabla:** `usuarios`

**Atributos principales:**
```java
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 20)
    private String documento;
    
    @Column(nullable = false, length = 200)
    private String nombreCompleto;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoUsuario tipoUsuario;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 20)
    private String telefono;
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    @Column(nullable = false, updatable = false)
    private OffsetDateTime creadoEn;
    
    @Column
    private OffsetDateTime actualizadoEn;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlantillaBiometrica> plantillasBiometricas = new ArrayList<>();
    
    @OneToMany(mappedBy = "usuario")
    private List<Pago> pagos = new ArrayList<>();
}
```

**Validaciones:**
- Documento único (constraint en BD + validación en servicio)
- Email con formato válido (regex en servicio)
- Teléfono con formato colombiano (regex en servicio)
- TipoUsuario: NINO, ESTUDIANTE, DOCENTE, PERSONAL

---

### **2. PlantillaBiometrica**

**Entidad:** `co.cellano.edufeed.backend.model.PlantillaBiometrica`

**Tabla:** `plantillas_biometricas`

**Atributos principales:**
```java
@Entity
@Table(name = "plantillas_biometricas")
public class PlantillaBiometrica {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modalidad modalidad;
    
    @Lob
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] plantilla;
    
    @Column(length = 50)
    private String proveedor;
    
    @Column(nullable = false)
    private Boolean activa = true;
    
    @Column(nullable = false, updatable = false)
    private OffsetDateTime creadoEn;
}
```

**Características especiales:**
- Plantilla almacenada como `bytea` (PostgreSQL) para evitar OID
- Mapeo con `@JdbcTypeCode(SqlTypes.BINARY)` para compatibilidad
- Cifrado de plantillas en capa de servicio (no en entidad)
- Modalidad: HUELLA, ROSTRO, VOZ

---

### **3. Pago**

**Entidad:** `co.cellano.edufeed.backend.model.Pago`

**Tabla:** `pagos`

**Atributos principales:**
```java
@Entity
@Table(name = "pagos")
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPago tipoPago;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;
    
    @Column
    private OffsetDateTime vigenteDesde;
    
    @Column
    private OffsetDateTime vigenteHasta;
    
    @Column(length = 100)
    private String referenciaExterna;
    
    @Column(length = 50)
    private String metodoPago;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadatos = new HashMap<>();
    
    @Column(nullable = false, updatable = false)
    private OffsetDateTime creadoEn;
}
```

**Validaciones:**
- Monto > 0 (constraint en BD + validación en servicio)
- TipoPago: DIARIO, MENSUAL, PAQUETE
- EstadoPago: PENDIENTE, APROBADO, RECHAZADO
- vigente_hasta >= vigente_desde (validación en servicio)

---

### **4. DerechoUso**

**Entidad:** `co.cellano.edufeed.backend.model.DerechoUso`

**Tabla:** `derechos_uso`

**Atributos principales:**
```java
@Entity
@Table(name = "derechos_uso")
public class DerechoUso {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;
    
    @Column(nullable = false)
    private OffsetDateTime vigenteDesde;
    
    @Column(nullable = false)
    private OffsetDateTime vigenteHasta;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPago tipoDerecho;
    
    @Column(nullable = false, updatable = false)
    private OffsetDateTime creadoEn;
}
```

**Lógica de negocio:**
- Generado automáticamente al aprobar pago
- Vigencias calculadas según tipo de pago
- Usado en verificación de acceso (RF-03)

---

### **5. Acceso**

**Entidad:** `co.cellano.edufeed.backend.model.Acceso`

**Tabla:** `accesos`

**Atributos principales:**
```java
@Entity
@Table(name = "accesos")
public class Acceso {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "derecho_id")
    private DerechoUso derecho;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAcceso estado;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Modalidad modalidad;
    
    @Column(length = 100)
    private String motivo;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadatos = new HashMap<>();
    
    @Column(nullable = false, updatable = false)
    private OffsetDateTime creadoEn;
}
```

**Casos de uso:**
- Registro de cada intento de acceso
- EstadoAcceso: APROBADO, DENEGADO
- Motivo de denegación: SIN_DERECHO, PAQUETE_AGOTADO, etc.
- Metadatos: información de coincidencia biométrica (score, umbral)

---

### **6. Enums de Dominio**

#### **TipoUsuario**
```java
public enum TipoUsuario {
    NINO,        // Niños
    ESTUDIANTE,  // Estudiantes
    DOCENTE,     // Docentes
    PERSONAL     // Personal administrativo
}
```

#### **Modalidad**
```java
public enum Modalidad {
    HUELLA,      // Huella dactilar
    ROSTRO,      // Reconocimiento facial
    VOZ          // Reconocimiento de voz
}
```

#### **TipoPago**
```java
public enum TipoPago {
    DIARIO,      // Pago diario
    MENSUAL,     // Mensualidad
    PAQUETE      // Paquete de días prepagados
}
```

#### **EstadoPago**
```java
public enum EstadoPago {
    PENDIENTE,   // Pendiente de aprobación
    APROBADO,    // Pago aprobado
    RECHAZADO    // Pago rechazado
}
```

#### **EstadoAcceso**
```java
public enum EstadoAcceso {
    APROBADO,    // Acceso permitido
    DENEGADO     // Acceso denegado
}
```

---

## 🗄️ Repositorios Spring Data

### **Ejemplo: UsuarioRepository**

```java
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    
    Optional<Usuario> findByDocumento(String documento);
    
    List<Usuario> findByActivoTrue();
    
    List<Usuario> findByTipoUsuario(TipoUsuario tipoUsuario);
    
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.nombreCompleto) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Usuario> findByNombreContaining(@Param("nombre") String nombre);
    
    boolean existsByDocumento(String documento);
}
```

**Características:**
- Métodos de consulta derivados de nombres
- Consultas custom con `@Query`
- Proyecciones para optimizar rendimiento
- Soporte para paginación y ordenamiento (Pageable)

---

### **Ejemplo: PagoRepository**

```java
@Repository
public interface PagoRepository extends JpaRepository<Pago, UUID> {
    
    List<Pago> findByUsuarioId(UUID usuarioId);
    
    List<Pago> findByEstadoPago(EstadoPago estadoPago);
    
    List<Pago> findByTipoPago(TipoPago tipoPago);
    
    @Query("SELECT p FROM Pago p WHERE p.creadoEn BETWEEN :inicio AND :fin")
    List<Pago> findByRangoFechas(
        @Param("inicio") OffsetDateTime inicio,
        @Param("fin") OffsetDateTime fin
    );
    
    Optional<Pago> findByReferenciaExterna(String referenciaExterna);
}
```

---

### **Ejemplo: DerechoUsoRepository**

```java
@Repository
public interface DerechoUsoRepository extends JpaRepository<DerechoUso, UUID> {
    
    @Query("SELECT d FROM DerechoUso d WHERE d.usuario.id = :usuarioId " +
           "AND :ahora BETWEEN d.vigenteDesde AND d.vigenteHasta")
    Optional<DerechoUso> findDerechoVigente(
        @Param("usuarioId") UUID usuarioId,
        @Param("ahora") OffsetDateTime ahora
    );
    
    List<DerechoUso> findByUsuarioId(UUID usuarioId);
    
    List<DerechoUso> findByTipoDerecho(TipoPago tipoDerecho);
}
```

---

## 📋 DTOs y Mappers

### **Ejemplo: UsuarioDto**

```java
public class UsuarioDto {
    private UUID id;
    
    @NotBlank(message = "El documento es obligatorio")
    @Size(max = 20, message = "El documento no puede exceder 20 caracteres")
    private String documento;
    
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String nombreCompleto;
    
    @NotNull(message = "El tipo de usuario es obligatorio")
    private TipoUsuario tipoUsuario;
    
    @Email(message = "El formato del email es inválido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;
    
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;
    
    private Boolean activo;
    
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;
    
    // Getters, setters, constructors
}
```

**Validaciones con Bean Validation:**
- `@NotBlank` - No nulo, no vacío, no solo espacios
- `@NotNull` - No nulo
- `@Email` - Formato de email válido
- `@Size` - Longitud mínima/máxima

---

### **Ejemplo: UsuarioMapper**

```java
@Component
public class UsuarioMapper {
    
    public UsuarioDto toDto(Usuario entity) {
        if (entity == null) {
            return null;
        }
        
        UsuarioDto dto = new UsuarioDto();
        dto.setId(entity.getId());
        dto.setDocumento(entity.getDocumento());
        dto.setNombreCompleto(entity.getNombreCompleto());
        dto.setTipoUsuario(entity.getTipoUsuario());
        dto.setEmail(entity.getEmail());
        dto.setTelefono(entity.getTelefono());
        dto.setActivo(entity.getActivo());
        dto.setCreadoEn(entity.getCreadoEn());
        dto.setActualizadoEn(entity.getActualizadoEn());
        
        return dto;
    }
    
    public Usuario toEntity(UsuarioDto dto) {
        if (dto == null) {
            return null;
        }
        
        Usuario entity = new Usuario();
        entity.setId(dto.getId());
        entity.setDocumento(dto.getDocumento());
        entity.setNombreCompleto(dto.getNombreCompleto());
        entity.setTipoUsuario(dto.getTipoUsuario());
        entity.setEmail(dto.getEmail());
        entity.setTelefono(dto.getTelefono());
        entity.setActivo(dto.getActivo());
        
        return entity;
    }
    
    public List<UsuarioDto> toDtoList(List<Usuario> entities) {
        return entities.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
}
```

**Ventajas:**
- Desacoplamiento de capa web y dominio
- No exponer entidades JPA directamente
- Facilita cambios en DTOs sin afectar modelo de dominio
- Evita problemas de lazy loading en JSON

---

## 🛢️ Migraciones de Base de Datos

### **V1__init.sql**

**Contenido:**
- Creación de 13 tablas
- Claves primarias (UUID)
- Claves foráneas con ON DELETE apropiado
- Índices compuestos para performance
- Constraints de validación (checks, unicidad)
- Tipos enumerados con checks

**Ejemplo de tabla (usuarios):**
```sql
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento VARCHAR(20) NOT NULL UNIQUE,
    nombre_completo VARCHAR(200) NOT NULL,
    tipo_usuario VARCHAR(20) NOT NULL CHECK (tipo_usuario IN ('NINO', 'ESTUDIANTE', 'DOCENTE', 'PERSONAL')),
    email VARCHAR(100),
    telefono VARCHAR(20),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ
);

CREATE INDEX idx_usuarios_documento ON usuarios(documento);
CREATE INDEX idx_usuarios_tipo ON usuarios(tipo_usuario);
CREATE INDEX idx_usuarios_activo ON usuarios(activo);
```

---

### **V2__webauthn_tables.sql**

**Contenido:**
- Tabla `dispositivos` (para autenticación por teléfono)
- Tabla `credenciales_webauthn` (claves públicas WebAuthn)
- Índices para búsqueda por usuario y dispositivo

**Propósito:**
- Soporte futuro para autenticación biométrica vía teléfono
- Implementación de WebAuthn/FIDO2
- Alternativa a hardware biométrico dedicado

---

## ⚙️ Configuración JPA

### **application.yml**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/edufeed
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Validar schema sin modificar
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
        jdbc:
          time_zone: America/Bogota
  
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: "0"
    validate-on-migrate: true
```

**Configuraciones clave:**
- `ddl-auto: validate` - Hibernate valida que entidades coincidan con schema
- `time_zone: America/Bogota` - Zona horaria para timestamps
- Flyway habilitado para migraciones versionadas

---

### **application-test.yml**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/edufeed_test
    username: postgres
    password: postgres
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
  
  flyway:
    enabled: true
    clean-disabled: false  # Permite limpiar BD en tests
```

**Perfil de tests:**
- Base de datos separada (`edufeed_test`)
- Flyway habilitado para aplicar migraciones
- `show-sql: true` para debug
- Sin usar H2 (tests contra PostgreSQL real)

---

## 🧪 Tests de Repositorio

### **UsuarioRepositoryTest**

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UsuarioRepositoryTest {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Test
    void testCrearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setDocumento("1234567890");
        usuario.setNombreCompleto("Juan Pérez");
        usuario.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        usuario.setActivo(true);
        usuario.setCreadoEn(OffsetDateTime.now(ZoneId.of("America/Bogota")));
        
        Usuario guardado = usuarioRepository.save(usuario);
        
        assertNotNull(guardado.getId());
        assertEquals("1234567890", guardado.getDocumento());
    }
    
    @Test
    void testBuscarPorDocumento() {
        Usuario usuario = new Usuario();
        usuario.setDocumento("9876543210");
        usuario.setNombreCompleto("María García");
        usuario.setTipoUsuario(TipoUsuario.DOCENTE);
        usuario.setActivo(true);
        usuario.setCreadoEn(OffsetDateTime.now(ZoneId.of("America/Bogota")));
        
        usuarioRepository.save(usuario);
        
        Optional<Usuario> encontrado = usuarioRepository.findByDocumento("9876543210");
        
        assertTrue(encontrado.isPresent());
        assertEquals("María García", encontrado.get().getNombreCompleto());
    }
}
```

**Características de tests:**
- `@DataJpaTest` - Configuración mínima de Spring Data JPA
- `@AutoConfigureTestDatabase(replace = NONE)` - Usar PostgreSQL real (no H2)
- `@ActiveProfiles("test")` - Usar `application-test.yml`
- Tests transaccionales (rollback automático)

---

### **PagoRepositoryTest**

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PagoRepositoryTest {
    
    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Test
    void testCrearPago() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setDocumento("1111111111");
        usuario.setNombreCompleto("Pedro López");
        usuario.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        usuario.setActivo(true);
        usuario.setCreadoEn(OffsetDateTime.now(ZoneId.of("America/Bogota")));
        usuario = usuarioRepository.save(usuario);
        
        // Crear pago
        Pago pago = new Pago();
        pago.setUsuario(usuario);
        pago.setMonto(new BigDecimal("50000"));
        pago.setTipoPago(TipoPago.DIARIO);
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pago.setCreadoEn(OffsetDateTime.now(ZoneId.of("America/Bogota")));
        
        Pago guardado = pagoRepository.save(pago);
        
        assertNotNull(guardado.getId());
        assertEquals(new BigDecimal("50000"), guardado.getMonto());
        assertEquals(TipoPago.DIARIO, guardado.getTipoPago());
    }
}
```

---

### **AccesoRepositoryTest**

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AccesoRepositoryTest {
    
    @Autowired
    private AccesoRepository accesoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private DerechoUsoRepository derechoUsoRepository;
    
    @Test
    void testRegistrarAccesoAprobado() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setDocumento("2222222222");
        usuario.setNombreCompleto("Ana Torres");
        usuario.setTipoUsuario(TipoUsuario.ESTUDIANTE);
        usuario.setActivo(true);
        usuario.setCreadoEn(OffsetDateTime.now(ZoneId.of("America/Bogota")));
        usuario = usuarioRepository.save(usuario);
        
        // Crear pago
        Pago pago = new Pago();
        pago.setUsuario(usuario);
        pago.setMonto(new BigDecimal("50000"));
        pago.setTipoPago(TipoPago.DIARIO);
        pago.setEstadoPago(EstadoPago.APROBADO);
        pago.setCreadoEn(OffsetDateTime.now(ZoneId.of("America/Bogota")));
        pago = pagoRepository.save(pago);
        
        // Crear derecho de uso
        DerechoUso derecho = new DerechoUso();
        derecho.setUsuario(usuario);
        derecho.setPago(pago);
        derecho.setVigenteDesde(OffsetDateTime.now(ZoneId.of("America/Bogota")));
        derecho.setVigenteHasta(OffsetDateTime.now(ZoneId.of("America/Bogota")).plusDays(1));
        derecho.setTipoDerecho(TipoPago.DIARIO);
        derecho.setCreadoEn(OffsetDateTime.now(ZoneId.of("America/Bogota")));
        derecho = derechoUsoRepository.save(derecho);
        
        // Registrar acceso
        Acceso acceso = new Acceso();
        acceso.setUsuario(usuario);
        acceso.setDerecho(derecho);
        acceso.setEstado(EstadoAcceso.APROBADO);
        acceso.setModalidad(Modalidad.HUELLA);
        acceso.setCreadoEn(OffsetDateTime.now(ZoneId.of("America/Bogota")));
        
        Acceso guardado = accesoRepository.save(acceso);
        
        assertNotNull(guardado.getId());
        assertEquals(EstadoAcceso.APROBADO, guardado.getEstado());
        assertNotNull(guardado.getDerecho());
    }
}
```

---

## 🌐 API Base Preparada

### **UsuarioController**

**Endpoints implementados:**

| Método | Ruta | Descripción | Estado |
|--------|------|-------------|--------|
| POST | /api/usuarios | Crear usuario | ✅ Funcional |
| GET | /api/usuarios | Listar usuarios | ✅ Funcional |
| GET | /api/usuarios/{id} | Obtener usuario por ID | ✅ Funcional |

**Ejemplo de uso:**

```bash
# Crear usuario
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "documento": "1234567890",
    "nombreCompleto": "Juan Pérez",
    "tipoUsuario": "ESTUDIANTE",
    "email": "juan@colegio.edu.co",
    "telefono": "+573001234567"
  }'

# Listar usuarios
curl http://localhost:8080/api/usuarios

# Obtener usuario
curl http://localhost:8080/api/usuarios/{id}
```

---

### **PagoController**

**Endpoints implementados:**

| Método | Ruta | Descripción | Estado |
|--------|------|-------------|--------|
| POST | /api/pagos | Crear pago | ✅ Funcional |
| GET | /api/pagos | Listar pagos | ✅ Funcional |

**Ejemplo de uso:**

```bash
# Crear pago
curl -X POST http://localhost:8080/api/pagos \
  -H "Content-Type: application/json" \
  -d '{
    "usuarioId": "{uuid}",
    "monto": 50000,
    "tipoPago": "DIARIO"
  }'

# Listar pagos
curl http://localhost:8080/api/pagos
```

---

## 🔄 Siguiente Fase

### **FASE 2: Capa de Servicio y Lógica de Negocio**

**Subdivisión en subfases:**
- **FASE 2.1:** UsuarioService CRUD completo (✅ COMPLETADO)
- **FASE 2.2:** Gestión de pagos y derechos (✅ COMPLETADO)
- **FASE 2.3:** Servicio de control de acceso (🔄 EN PROGRESO)

**Objetivos de FASE 2.3:**
- Implementar AccesoService con verificación de derechos
- Registrar accesos (APROBADO/DENEGADO) con motivo
- Consumir días de paquete si aplica
- Generar respuesta con instrucciones para usuario denegado (RF-04)

**Archivos a crear en FASE 2.3:**
- `AccesoService.java`
- `NoDerechoVigenteException.java`
- `PaqueteAgotadoException.java`
- Tests de AccesoService

**Estimación:** 1 semana

---

## ✅ Criterios de Aceptación

### **Verificados:**
- ✅ Esquema base operativo en PostgreSQL con migraciones V1 y V2
- ✅ Entidades JPA mapeadas para las 13 tablas
- ✅ Validación Hibernate sin errores (ddl-auto: validate)
- ✅ Repositorios funcionales con métodos custom
- ✅ DTOs con Bean Validation
- ✅ Mappers implementados (entity ↔ DTO)
- ✅ Tests de repositorio ejecutándose contra BD real
- ✅ Primeros endpoints REST operativos

---

## 📊 Métricas de la Fase

| Métrica | Valor | Objetivo |
|---------|-------|----------|
| Tiempo de desarrollo | 3 semanas | ✅ 2-3 semanas |
| Entidades JPA creadas | 13 | ✅ 13 |
| Repositorios creados | 12 | ✅ 12 |
| DTOs creados | 4 | ✅ 4 |
| Mappers creados | 4 | ✅ 4 |
| Tests de repositorio | 3 archivos | ✅ ≥3 |
| Cobertura de tests | ~60% | ⚠️ Objetivo ≥80% (siguiente fase) |
| Endpoints REST | 5 | ✅ ≥5 |

---

## 🎓 Lecciones Aprendidas

1. **Mapeo de bytea en PostgreSQL:** Usar `@JdbcTypeCode(SqlTypes.BINARY)` + `columnDefinition = "bytea"` para evitar OID
2. **PK compuesta en JPA:** Crear clase `@Embeddable` para mapear claves compuestas (ej: UsuarioRolId)
3. **Tests con BD real:** Configurar `@AutoConfigureTestDatabase(replace = NONE)` para usar PostgreSQL en tests
4. **Zona horaria:** Configurar explícitamente `jdbc.time_zone` en Hibernate para timestamps consistentes
5. **Lazy loading en DTOs:** Usar mappers para evitar excepciones de lazy loading al serializar JSON

---

**Fecha de actualización:** 20 de octubre de 2025  
**Responsable:** Equipo EduFeed  
**Estado del proyecto:** ✅ FASE 1 COMPLETADA - Continuando FASE 2.3
