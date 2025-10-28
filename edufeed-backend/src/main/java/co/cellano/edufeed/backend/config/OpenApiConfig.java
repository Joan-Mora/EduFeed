package co.cellano.edufeed.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger para la documentación de la API REST.
 * 
 * <p>
 * Accesible en: http://localhost:8080/swagger-ui/index.html
 * JSON OpenAPI: http://localhost:8080/v3/api-docs
 * </p>
 * 
 * @since FASE 3.4
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduFeed API")
                        .description("""
                                API REST para el sistema de control de acceso con biometría EduFeed.
                                
                                ## Características principales:
                                - **Gestión de usuarios** con datos biométricos (huella, rostro, voz)
                                - **Control de acceso** con verificación de derechos de uso
                                - **Pagos y paquetes** (DIARIO, MENSUAL, PAQUETE)
                                - **Reportes administrativos** (asistencias, rechazos, ingresos, derechos activos)
                                - **Webhooks** para integración con sistemas de caja
                                
                                ## Autenticación:
                                - Actualmente sin autenticación (desarrollo)
                                - Producción: OAuth2/JWT (próximamente)
                                
                                ## Códigos de error:
                                - **400**: Datos inválidos o regla de negocio violada
                                - **404**: Recurso no encontrado
                                - **409**: Conflicto (ej: documento duplicado)
                                - **403**: Sin derecho de acceso vigente
                                - **500**: Error interno del servidor
                                """)
                        .version("0.1.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("Equipo EduFeed")
                                .email("soporte@edufeed.co")
                                .url("https://github.com/Joan-Mora/EduFeed"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor de desarrollo local"),
                        new Server()
                                .url("https://api.edufeed.co")
                                .description("Servidor de producción (próximamente)")));
    }
}
