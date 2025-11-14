package co.cellano.edufeed.desktop.admin;

import co.cellano.edufeed.desktop.service.UserApiClient;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Vista PREMIUM de administración de usuarios con diseño corporativo de alto
 * nivel.
 * Inspirada en Microsoft Fluent Design y Material Design 3.
 * Características: gradientes modernos, glassmorphism, microinteracciones y
 * jerarquía visual clara.
 */
public class UserManagementViewV2 extends BorderPane {
        // Filtros
        public final TextField filtroDocumento = new TextField();
        public final TextField filtroNombre = new TextField();
        public final ComboBox<String> filtroTipo = new ComboBox<>();
        public final CheckBox filtroActivos = new CheckBox("Solo activos");
        public final Button buscarBtn = new Button("🔍 Buscar");
        public final Button limpiarBtn = new Button("✖ Limpiar");

        // Tabla y paginación
        public final TableView<UserApiClient.UsuarioDto> table = new TableView<>();
        public final Pagination pagination = new Pagination(1, 0);

        // Acciones principales
        public final Button crearBtn = new Button("➕ Nuevo Usuario");
        public final Button verBtn = new Button("👁 Ver");
        public final Button editarBtn = new Button("✏ Editar");
        public final Button toggleActivoBtn = new Button("⚡ Activar/Desactivar");
        public final Button eliminarBtn = new Button("🗑 Eliminar");
        public final Button biometriaBtn = new Button("🔐 Gestión Biométrica");

        // Botón de registro biométrico
        public final Button registrarBioBtn = new Button("🔐 Registro Biométrico");

        // Estado y chips biométricos
        public final Label status = new Label("Listo");
        public final HBox bioChips = new HBox(6);
        public final Label statsLabel = new Label();
        public final Label statsCount = new Label("0");

        public UserManagementViewV2() {
                setPadding(new Insets(0));

                // Fondo con gradiente sutil
                setStyle(
                                "-fx-background-color: linear-gradient(to bottom right, " +
                                                "derive(-fx-background, -2%), " +
                                                "derive(-fx-background, 2%));");

                // === HEADER HERO CON GRADIENTE ===
                VBox header = createPremiumHeader();

                // Contenedor principal con espaciado fluido
                VBox content = new VBox(20);
                content.setPadding(new Insets(24, 32, 32, 32));

                // === CARD DE FILTROS GLASSMORPHIC ===
                VBox filtersCard = createPremiumFiltersCard();

                // === CARD DE TABLA ELEVADA ===
                VBox tableCard = createPremiumTableCard();

                // === BARRA DE ACCIONES FLOTANTE ===
                VBox actionsBar = createPremiumActionsBar();

                content.getChildren().addAll(filtersCard, tableCard, actionsBar);

                // Layout principal
                BorderPane mainContainer = new BorderPane();
                mainContainer.setTop(header);
                mainContainer.setCenter(content);
                setCenter(mainContainer);
        }

        /**
         * HEADER HERO con gradiente corporativo y efecto de profundidad
         */
        private VBox createPremiumHeader() {
                VBox header = new VBox(12);
                header.setPadding(new Insets(40, 32, 40, 32));

                // Gradiente corporativo dinámico (ajustable según tema)
                header.setStyle(
                                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #667eea 0%, #764ba2 100%);"
                                                +
                                                "-fx-background-radius: 0 0 16 16;");

                // Efecto de elevación con sombra difusa
                DropShadow headerShadow = new DropShadow();
                headerShadow.setRadius(24);
                headerShadow.setOffsetY(4);
                headerShadow.setColor(Color.rgb(0, 0, 0, 0.2));
                header.setEffect(headerShadow);

                // Título principal con contraste mejorado y sombra más visible
                Label title = new Label("Administración de Usuarios");
                title.setStyle(
                                "-fx-font-size: 30; " +
                                                "-fx-font-weight: bold; " +
                                                "-fx-text-fill: white; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 6, 0, 0, 3);");

                // Subtítulo descriptivo con mejor contraste
                Label subtitle = new Label("Gestiona usuarios, permisos y autenticación biométrica del sistema");
                subtitle.setStyle(
                                "-fx-font-size: 15; " +
                                                "-fx-text-fill: white; " +
                                                "-fx-font-weight: 500; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 3, 0, 0, 2);");

                // Stats card incrustado con glassmorphism mejorado (más opaco)
                HBox statsCard = new HBox(16);
                statsCard.setAlignment(Pos.CENTER_LEFT);
                statsCard.setPadding(new Insets(14, 24, 14, 24));
                statsCard.setStyle(
                                "-fx-background-color: rgba(255, 255, 255, 0.25); " +
                                                "-fx-background-radius: 12; " +
                                                "-fx-border-color: rgba(255, 255, 255, 0.5); " +
                                                "-fx-border-width: 1.5; " +
                                                "-fx-border-radius: 12; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");

                // Ícono decorativo
                Label icon = new Label("👥");
                icon.setStyle("-fx-font-size: 24;");

                // Contador de usuarios con estilo hero
                statsCount.setText("0");
                statsCount.setStyle(
                                "-fx-font-size: 32; " +
                                                "-fx-font-weight: bold; " +
                                                "-fx-text-fill: white;");

                Label statsLabelLocal = new Label("usuarios registrados");
                statsLabelLocal.setStyle(
                                "-fx-font-size: 14; " +
                                                "-fx-text-fill: white; " +
                                                "-fx-font-weight: 600;");

                statsCard.getChildren().addAll(icon, statsCount, statsLabelLocal);

                VBox textContainer = new VBox(4, title, subtitle);
                Region spacer = new Region();
                VBox.setVgrow(spacer, Priority.ALWAYS);

                header.getChildren().addAll(textContainer, spacer, statsCard);
                return header;
        }

        /**
         * CARD DE FILTROS con efecto glassmorphism y bordes sutiles
         */
        private VBox createPremiumFiltersCard() {
                VBox card = new VBox(16);
                card.setPadding(new Insets(24));
                card.getStyleClass().add("app-card");

                // Estilo con fondo más sólido y sombra elevada
                card.setStyle(
                                "-fx-background-color: -fx-surface; " +
                                                "-fx-background-radius: 16; " +
                                                "-fx-border-color: derive(-fx-surface, 15%); " +
                                                "-fx-border-width: 1.5; " +
                                                "-fx-border-radius: 16; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 24, 0, 0, 8);");

                // Título de sección con ícono
                Label filterTitle = new Label("🔍 Filtros de Búsqueda");
                filterTitle.setStyle(
                                "-fx-font-size: 16; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-text-fill: -fx-text-base;");

                // Grid de filtros con espaciado moderno
                GridPane grid = new GridPane();
                grid.setHgap(16);
                grid.setVgap(14);
                grid.setPadding(new Insets(8, 0, 0, 0));

                // Campos de filtro con estilo premium
                String inputStyle = "-fx-background-color: -fx-background; " +
                                "-fx-background-radius: 10; " +
                                "-fx-border-color: derive(-fx-background, 15%); " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 10; " +
                                "-fx-padding: 10 14; " +
                                "-fx-font-size: 13; " +
                                "-fx-prompt-text-fill: -fx-text-secondary;";

                filtroDocumento.setPromptText("Documento de identidad");
                filtroDocumento.setStyle(inputStyle);
                filtroDocumento.setPrefWidth(220);

                filtroNombre.setPromptText("Nombre completo");
                filtroNombre.setStyle(inputStyle);
                filtroNombre.setPrefWidth(220);

                filtroTipo.setPromptText("Tipo de usuario");
                filtroTipo.setStyle(inputStyle);
                filtroTipo.setPrefWidth(220);
                // Valores alineados con la BD: NINO, ESTUDIANTE, DOCENTE, PERSONAL
                filtroTipo.getItems().setAll("Todos", "NINO", "ESTUDIANTE", "DOCENTE", "PERSONAL");
                filtroTipo.getSelectionModel().select(0);

                filtroActivos.setSelected(true);
                filtroActivos.setStyle("-fx-font-size: 13; -fx-text-fill: -fx-text;");

                // Labels modernos
                String labelStyle = "-fx-font-size: 13; -fx-font-weight: 500; -fx-text-fill: -fx-text-secondary;";
                Label lblDoc = new Label("Documento");
                lblDoc.setStyle(labelStyle);
                Label lblNombre = new Label("Nombre");
                lblNombre.setStyle(labelStyle);
                Label lblTipo = new Label("Tipo");
                lblTipo.setStyle(labelStyle);

                grid.add(lblDoc, 0, 0);
                grid.add(filtroDocumento, 0, 1);
                grid.add(lblNombre, 1, 0);
                grid.add(filtroNombre, 1, 1);
                grid.add(lblTipo, 2, 0);
                grid.add(filtroTipo, 2, 1);
                grid.add(filtroActivos, 0, 2, 3, 1);

                // Botones de acción con colores corporativos
                HBox actions = new HBox(12);
                actions.setAlignment(Pos.CENTER_RIGHT);
                actions.setPadding(new Insets(8, 0, 0, 0));

                buscarBtn.getStyleClass().addAll("app-button", "app-button--primary");
                buscarBtn.setStyle(
                                "-fx-background-color: linear-gradient(to bottom, #667eea, #5568d3); " +
                                                "-fx-text-fill: white; " +
                                                "-fx-background-radius: 10; " +
                                                "-fx-padding: 10 24; " +
                                                "-fx-font-size: 13; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-cursor: hand; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 8, 0, 0, 2);");

                limpiarBtn.getStyleClass().addAll("app-button", "app-button--ghost");
                limpiarBtn.setStyle(
                                "-fx-background-color: transparent; " +
                                                "-fx-text-fill: -fx-text-secondary; " +
                                                "-fx-border-color: derive(-fx-background, 20%); " +
                                                "-fx-border-width: 1; " +
                                                "-fx-border-radius: 10; " +
                                                "-fx-background-radius: 10; " +
                                                "-fx-padding: 10 24; " +
                                                "-fx-font-size: 13; " +
                                                "-fx-cursor: hand;");

                actions.getChildren().addAll(limpiarBtn, buscarBtn);

                card.getChildren().addAll(filterTitle, grid, actions);
                return card;
        }

        /**
         * CARD DE TABLA con diseño elevado y columnas optimizadas
         */
        private VBox createPremiumTableCard() {
                VBox card = new VBox(0);
                card.setStyle(
                                "-fx-background-color: -fx-surface; " +
                                                "-fx-background-radius: 16; " +
                                                "-fx-border-color: derive(-fx-surface, 15%); " +
                                                "-fx-border-width: 1.5; " +
                                                "-fx-border-radius: 16; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 24, 0, 0, 8);");

                // Header de tabla con título
                HBox tableHeader = new HBox();
                tableHeader.setPadding(new Insets(20, 24, 16, 24));
                tableHeader.setAlignment(Pos.CENTER_LEFT);
                tableHeader.setStyle(
                                "-fx-background-color: derive(-fx-surface, -3%); " +
                                                "-fx-background-radius: 16 16 0 0;");

                Label tableTitle = new Label("Listado de Usuarios");
                tableTitle.setStyle(
                                "-fx-font-size: 17; " +
                                                "-fx-font-weight: bold; " +
                                                "-fx-text-fill: -fx-text-base;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                tableHeader.getChildren().addAll(tableTitle, spacer, statsLabel);

                // Configurar tabla con estilo premium
                table.getStyleClass().add("app-table");
                table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                table.setPlaceholder(new Label("🔍 No hay usuarios para mostrar"));
                table.setStyle(
                                "-fx-background-color: -fx-surface; " +
                                                "-fx-padding: 0 16 16 16;");
                // Asegurar que el área de datos tenga altura suficiente para ver 10-12 filas
                // cómodamente
                table.setMinHeight(500);
                table.setPrefHeight(Region.USE_COMPUTED_SIZE);
                VBox.setVgrow(table, Priority.ALWAYS);

                // Columnas con diseño moderno
                TableColumn<UserApiClient.UsuarioDto, String> colDoc = new TableColumn<>("📄 Documento");
                colDoc.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().documento));
                colDoc.setPrefWidth(140);
                colDoc.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: 500;");

                TableColumn<UserApiClient.UsuarioDto, String> colNom = new TableColumn<>("👤 Nombre Completo");
                colNom.setCellValueFactory(
                                c -> new javafx.beans.property.SimpleStringProperty(c.getValue().nombreCompleto));
                colNom.setPrefWidth(260);
                colNom.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: 500;");

                TableColumn<UserApiClient.UsuarioDto, String> colTipo = new TableColumn<>("🏷 Tipo");
                colTipo.setCellValueFactory(
                                c -> new javafx.beans.property.SimpleStringProperty(c.getValue().tipoUsuario));
                colTipo.setPrefWidth(120);
                colTipo.setStyle("-fx-alignment: CENTER;");

                TableColumn<UserApiClient.UsuarioDto, String> colEmail = new TableColumn<>("📧 Email");
                colEmail.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                                c.getValue().email != null && !c.getValue().email.isEmpty() ? c.getValue().email
                                                : "—"));
                colEmail.setPrefWidth(220);
                colEmail.setStyle("-fx-alignment: CENTER-LEFT;");

                TableColumn<UserApiClient.UsuarioDto, String> colTel = new TableColumn<>("📱 Teléfono");
                colTel.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                                c.getValue().telefono != null && !c.getValue().telefono.isEmpty()
                                                ? c.getValue().telefono
                                                : "—"));
                colTel.setPrefWidth(130);
                colTel.setStyle("-fx-alignment: CENTER;");

                TableColumn<UserApiClient.UsuarioDto, String> colActivo = new TableColumn<>("✅ Estado");
                colActivo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                                Boolean.TRUE.equals(c.getValue().activo) ? "✅ Activo" : "⏸ Inactivo"));
                colActivo.setPrefWidth(110);
                colActivo.setStyle("-fx-alignment: CENTER;");

                table.getColumns().clear();
                table.getColumns().add(colDoc);
                table.getColumns().add(colNom);
                table.getColumns().add(colTipo);
                table.getColumns().add(colEmail);
                table.getColumns().add(colTel);
                table.getColumns().add(colActivo);

                // Paginación con estilo
                pagination.getStyleClass().add("pagination");
                pagination.setStyle(
                                "-fx-background-color: derive(-fx-surface, -2%); " +
                                                "-fx-background-radius: 0 0 16 16; " +
                                                "-fx-padding: 12;");

                card.getChildren().addAll(tableHeader, new Separator(), table, pagination);
                // Permitir que la tabla crezca dentro de la tarjeta para mostrar filas
                VBox.setVgrow(table, Priority.ALWAYS);
                return card;
        }

        /**
         * BARRA DE ACCIONES flotante con botones color-coded y microinteracciones
         */
        private VBox createPremiumActionsBar() {
                VBox container = new VBox(18);
                container.setPadding(new Insets(20, 24, 20, 24));
                container.setStyle(
                                "-fx-background-color: -fx-surface; " +
                                                "-fx-background-radius: 16; " +
                                                "-fx-border-color: derive(-fx-surface, 15%); " +
                                                "-fx-border-width: 1.5; " +
                                                "-fx-border-radius: 16; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 6);");

                Label actionsTitle = new Label("⚙ Panel de Acciones");
                actionsTitle.setStyle(
                                "-fx-font-size: 14; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-text-fill: -fx-text-base;");

                // === BOTONES PRINCIPALES con color-coding corporativo ===

                // Botón CREAR (Verde Success)
                crearBtn.setStyle(
                                "-fx-background-color: linear-gradient(to bottom, #10b981, #059669); " +
                                                "-fx-text-fill: white; " +
                                                "-fx-background-radius: 10; " +
                                                "-fx-padding: 12 20; " +
                                                "-fx-font-size: 13; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-cursor: hand; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.4), 8, 0, 0, 2);");

                // Botón VER (Gris neutro con fondo sólido)
                verBtn.setStyle(
                                "-fx-background-color: linear-gradient(to bottom, #6b7280, #4b5563); " +
                                                "-fx-text-fill: white; " +
                                                "-fx-border-radius: 10; " +
                                                "-fx-background-radius: 10; " +
                                                "-fx-padding: 12 20; " +
                                                "-fx-font-size: 13; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-cursor: hand; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(107,114,128,0.4), 8, 0, 0, 2);");

                // Botón EDITAR (Azul Primary)
                editarBtn.setStyle(
                                "-fx-background-color: linear-gradient(to bottom, #3b82f6, #2563eb); " +
                                                "-fx-text-fill: white; " +
                                                "-fx-background-radius: 10; " +
                                                "-fx-padding: 12 20; " +
                                                "-fx-font-size: 13; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-cursor: hand; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.4), 8, 0, 0, 2);");

                // Botón TOGGLE (Naranja Warning)
                toggleActivoBtn.setStyle(
                                "-fx-background-color: linear-gradient(to bottom, #f59e0b, #d97706); " +
                                                "-fx-text-fill: white; " +
                                                "-fx-background-radius: 10; " +
                                                "-fx-padding: 12 20; " +
                                                "-fx-font-size: 13; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-cursor: hand; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(245,158,11,0.4), 8, 0, 0, 2);");

                // Botón ELIMINAR (Rojo Danger)
                eliminarBtn.setStyle(
                                "-fx-background-color: linear-gradient(to bottom, #ef4444, #dc2626); " +
                                                "-fx-text-fill: white; " +
                                                "-fx-background-radius: 10; " +
                                                "-fx-padding: 12 20; " +
                                                "-fx-font-size: 13; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-cursor: hand; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(239,68,68,0.4), 8, 0, 0, 2);");

                // Botón BIOMETRÍA (Púrpura Accent)
                biometriaBtn.setStyle(
                                "-fx-background-color: linear-gradient(to bottom, #8b5cf6, #7c3aed); " +
                                                "-fx-text-fill: white; " +
                                                "-fx-background-radius: 10; " +
                                                "-fx-padding: 12 20; " +
                                                "-fx-font-size: 13; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-cursor: hand; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.4), 8, 0, 0, 2);");

                // Botón de registro biométrico con estilo premium
                registrarBioBtn.setStyle(
                                "-fx-background-color: linear-gradient(to bottom, #6366f1, #4f46e5); " +
                                                "-fx-text-fill: white; " +
                                                "-fx-background-radius: 10; " +
                                                "-fx-padding: 12 20; " +
                                                "-fx-font-size: 13; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-cursor: hand; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.4), 8, 0, 0, 2);");

                // Layout de acciones en fila única con separadores visuales
                HBox actions = new HBox(10);
                actions.setAlignment(Pos.CENTER_LEFT);
                actions.getChildren().addAll(
                                crearBtn,
                                createButtonSeparator(),
                                verBtn,
                                editarBtn,
                                toggleActivoBtn,
                                eliminarBtn);

                HBox bioActions = new HBox(10);
                bioActions.setAlignment(Pos.CENTER_LEFT);
                bioActions.getChildren().addAll(biometriaBtn, registrarBioBtn);

                // === STATUS BAR con chips biométricos ===
                HBox statusBar = new HBox(14);
                statusBar.setPadding(new Insets(12, 0, 0, 0));
                statusBar.setAlignment(Pos.CENTER_LEFT);
                statusBar.setStyle(
                                "-fx-background-color: derive(-fx-surface, -2%); " +
                                                "-fx-background-radius: 10; " +
                                                "-fx-padding: 12 16;");

                Label bioLabel = new Label("🔐 Estado Biométrico:");
                bioLabel.setStyle(
                                "-fx-font-size: 12; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-text-fill: -fx-text-secondary;");

                bioChips.setAlignment(Pos.CENTER_LEFT);
                bioChips.setSpacing(8);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                status.setStyle(
                                "-fx-font-size: 12; " +
                                                "-fx-font-weight: 500; " +
                                                "-fx-text-fill: -fx-text-secondary;");

                statusBar.getChildren().addAll(bioLabel, bioChips, spacer, status);

                container.getChildren().addAll(
                                actionsTitle,
                                new Separator(),
                                actions,
                                bioActions,
                                statusBar);
                return container;
        }

        /**
         * Separador visual minimalista para botones
         */
        private Region createButtonSeparator() {
                Region sep = new Region();
                sep.setPrefWidth(1);
                sep.setMaxHeight(28);
                sep.setStyle("-fx-background-color: derive(-fx-background, 15%);");
                return sep;
        }

        // === MÉTODOS PÚBLICOS ===

        public void setTableData(List<UserApiClient.UsuarioDto> usuarios) {
                table.getItems().setAll(usuarios);
        }

        public UserApiClient.UsuarioDto getSelected() {
                return table.getSelectionModel().getSelectedItem();
        }

        /**
         * Actualiza el contador de usuarios en el header y el label de stats
         */
        public void updateStats(long total) {
                statsCount.setText(String.valueOf(total));
                statsLabel.setText(String.format("📊 Total: %d usuario%s", total, total == 1 ? "" : "s"));
                statsLabel.setStyle(
                                "-fx-font-size: 13; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-text-fill: -fx-text-secondary;");
        }

        /**
         * Muestra un diálogo elegante para seleccionar el tipo de registro biométrico
         * 
         * @return El tipo seleccionado: "completo", "huella", "facial", "voz", o null
         *         si se cancela
         */
        public String showBiometricRegistrationDialog() {
                Dialog<String> dialog = new Dialog<>();
                dialog.setTitle("Registro Biométrico");
                dialog.setHeaderText("Seleccione el tipo de registro biométrico");

                // Configurar el contenido del diálogo
                VBox content = new VBox(16);
                content.setPadding(new Insets(20));
                content.setStyle(
                                "-fx-background-color: -fx-background; " +
                                                "-fx-background-radius: 12;");

                Label instruction = new Label("¿Cómo desea realizar el registro biométrico?");
                instruction.setStyle(
                                "-fx-font-size: 14; " +
                                                "-fx-font-weight: 500; " +
                                                "-fx-text-fill: -fx-text-secondary;");

                // Crear botones para cada opción con estilos premium
                Button completoBtn = createDialogOptionButton(
                                "✅ Registro Completo",
                                "Registra todas las opciones biométricas disponibles",
                                "#10b981", "#059669");

                Button huellaBtn = createDialogOptionButton(
                                "👆 Huella Dactilar",
                                "Registra únicamente la huella dactilar",
                                "#3b82f6", "#2563eb");

                Button facialBtn = createDialogOptionButton(
                                "😊 Reconocimiento Facial",
                                "Registra el reconocimiento facial (FaceID)",
                                "#8b5cf6", "#7c3aed");

                Button vozBtn = createDialogOptionButton(
                                "🎤 Reconocimiento de Voz",
                                "Registra la voz para autenticación",
                                "#f59e0b", "#d97706");

                // Configurar acciones de los botones
                completoBtn.setOnAction(e -> {
                        dialog.setResult("completo");
                        dialog.close();
                });

                huellaBtn.setOnAction(e -> {
                        dialog.setResult("huella");
                        dialog.close();
                });

                facialBtn.setOnAction(e -> {
                        dialog.setResult("facial");
                        dialog.close();
                });

                vozBtn.setOnAction(e -> {
                        dialog.setResult("voz");
                        dialog.close();
                });

                content.getChildren().addAll(
                                instruction,
                                completoBtn,
                                huellaBtn,
                                facialBtn,
                                vozBtn);

                dialog.getDialogPane().setContent(content);
                dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

                // Estilo del diálogo
                DialogPane dialogPane = dialog.getDialogPane();
                dialogPane.setStyle(
                                "-fx-background-color: -fx-background; " +
                                                "-fx-background-radius: 16; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 30, 0, 0, 8);");

                return dialog.showAndWait().orElse(null);
        }

        /**
         * Crea un botón de opción para el diálogo con estilo premium
         */
        private Button createDialogOptionButton(String title, String description, String color1, String color2) {
                VBox buttonContent = new VBox(6);
                buttonContent.setPadding(new Insets(16));
                buttonContent.setStyle(
                                "-fx-background-color: linear-gradient(to right, " + color1 + ", " + color2 + "); " +
                                                "-fx-background-radius: 10; " +
                                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);");

                Label titleLabel = new Label(title);
                titleLabel.setStyle(
                                "-fx-font-size: 15; " +
                                                "-fx-font-weight: 600; " +
                                                "-fx-text-fill: white;");

                Label descLabel = new Label(description);
                descLabel.setStyle(
                                "-fx-font-size: 12; " +
                                                "-fx-text-fill: rgba(255,255,255,0.9); " +
                                                "-fx-wrap-text: true;");
                descLabel.setMaxWidth(400);

                buttonContent.getChildren().addAll(titleLabel, descLabel);

                Button button = new Button();
                button.setGraphic(buttonContent);
                button.setMaxWidth(Double.MAX_VALUE);
                button.setStyle(
                                "-fx-background-color: transparent; " +
                                                "-fx-cursor: hand; " +
                                                "-fx-padding: 0;");

                // Efecto hover
                button.setOnMouseEntered(e -> {
                        buttonContent.setStyle(
                                        "-fx-background-color: linear-gradient(to right, " + color1 + ", " + color2
                                                        + "); " +
                                                        "-fx-background-radius: 10; " +
                                                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 4); "
                                                        +
                                                        "-fx-scale-x: 1.02; " +
                                                        "-fx-scale-y: 1.02;");
                });

                button.setOnMouseExited(e -> {
                        buttonContent.setStyle(
                                        "-fx-background-color: linear-gradient(to right, " + color1 + ", " + color2
                                                        + "); " +
                                                        "-fx-background-radius: 10; " +
                                                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);");
                });

                return button;
        }
}
