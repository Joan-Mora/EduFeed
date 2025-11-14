package co.cellano.edufeed.desktop.admin;

import co.cellano.edufeed.desktop.service.UserApiClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Vista simplificada y elegante para visualizar información básica de un
 * usuario.
 * Muestra avatar grande, nombre, documento y email en un diseño limpio.
 */
public class UserViewCard extends VBox {

    public UserViewCard(UserApiClient.UsuarioDto usuario) {
        super(24);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(40, 48, 40, 48));
        setStyle(
                "-fx-background-color: -fx-background; " +
                        "-fx-background-radius: 16;");

        // === AVATAR GRANDE ===
        VBox avatarContainer = new VBox(16);
        avatarContainer.setAlignment(Pos.CENTER);

        // Círculo con inicial
        Label avatarCircle = new Label(getInitial(usuario.nombreCompleto));
        avatarCircle.setMinSize(120, 120);
        avatarCircle.setMaxSize(120, 120);
        avatarCircle.setAlignment(Pos.CENTER);
        avatarCircle.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #667eea 0%, #764ba2 100%); " +
                        "-fx-background-radius: 60; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 48; " +
                        "-fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.4), 20, 0, 0, 8);");

        // Badge de estado
        Label estadoBadge = new Label(Boolean.TRUE.equals(usuario.activo) ? "✅ Activo" : "⏸ Inactivo");
        estadoBadge.setStyle(
                "-fx-background-color: " + (Boolean.TRUE.equals(usuario.activo)
                        ? "linear-gradient(to right, #10b981, #059669)"
                        : "linear-gradient(to right, #6b7280, #4b5563)") + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 16; " +
                        "-fx-padding: 6 16; " +
                        "-fx-font-size: 13; " +
                        "-fx-font-weight: 600; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);");

        avatarContainer.getChildren().addAll(avatarCircle, estadoBadge);

        // === INFORMACIÓN PRINCIPAL ===
        VBox infoContainer = new VBox(20);
        infoContainer.setAlignment(Pos.CENTER);
        infoContainer.setPadding(new Insets(16, 0, 0, 0));

        // Nombre
        Label nombreLabel = new Label(usuario.nombreCompleto);
        nombreLabel.setAlignment(Pos.CENTER); // Alineación del label
        nombreLabel.setStyle(
                "-fx-font-size: 28; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: -fx-text-base; " +
                        "-fx-text-alignment: center;");
        nombreLabel.setWrapText(true);
        nombreLabel.setMaxWidth(450);

        // Tipo de usuario
        Label tipoLabel = new Label(formatTipo(usuario.tipoUsuario));
        tipoLabel.setStyle(
                "-fx-font-size: 15; " +
                        "-fx-text-fill: -fx-text-secondary; " +
                        "-fx-font-weight: 500;");

        // Separador decorativo
        Region separator1 = new Region();
        separator1.setPrefHeight(1);
        separator1.setMaxWidth(300);
        separator1.setStyle("-fx-background-color: derive(-fx-background, 10%);");

        infoContainer.getChildren().addAll(nombreLabel, tipoLabel, separator1);

        // === DETALLES EN CARDS ===
        VBox detailsContainer = new VBox(14);
        detailsContainer.setAlignment(Pos.CENTER);
        detailsContainer.setMaxWidth(500);

        // Card de documento
        HBox docCard = createInfoCard("📄", "Documento", usuario.documento);

        // Card de email
        HBox emailCard = createInfoCard("📧", "Correo Electrónico",
                usuario.email != null && !usuario.email.isEmpty() ? usuario.email : "No registrado");

        // Card de teléfono (opcional)
        if (usuario.telefono != null && !usuario.telefono.isEmpty()) {
            HBox phoneCard = createInfoCard("📱", "Teléfono", usuario.telefono);
            detailsContainer.getChildren().add(phoneCard);
        }

        detailsContainer.getChildren().addAll(docCard, emailCard);

        // === FOOTER INFO ===
        Label footerNote = new Label("💡 Información básica del usuario");
        footerNote.setStyle(
                "-fx-font-size: 12; " +
                        "-fx-text-fill: -fx-text-secondary; " +
                        "-fx-font-style: italic;");

        getChildren().addAll(avatarContainer, infoContainer, detailsContainer, footerNote);
    }

    private HBox createInfoCard(String icon, String label, String value) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
                "-fx-background-color: -fx-surface; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: derive(-fx-surface, 10%); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

        // Icono
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
                "-fx-font-size: 24; " +
                        "-fx-min-width: 40; " +
                        "-fx-alignment: center;");

        // Textos
        VBox textBox = new VBox(4);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label labelText = new Label(label);
        labelText.setStyle(
                "-fx-font-size: 12; " +
                        "-fx-text-fill: -fx-text-secondary; " +
                        "-fx-font-weight: 600;");

        Label valueText = new Label(value);
        valueText.setStyle(
                "-fx-font-size: 15; " +
                        "-fx-text-fill: -fx-text-base; " +
                        "-fx-font-weight: 500;");
        valueText.setWrapText(true);

        textBox.getChildren().addAll(labelText, valueText);
        card.getChildren().addAll(iconLabel, textBox);

        return card;
    }

    private String getInitial(String nombre) {
        if (nombre == null || nombre.isEmpty())
            return "?";
        return nombre.substring(0, 1).toUpperCase();
    }

    private String formatTipo(String tipo) {
        if (tipo == null)
            return "Usuario";
        switch (tipo.toUpperCase()) {
            case "NINO":
                return "👶 Niño";
            case "ESTUDIANTE":
                return "🎓 Estudiante";
            case "DOCENTE":
                return "👨‍🏫 Docente";
            case "PERSONAL":
                return "👔 Personal";
            default:
                return tipo;
        }
    }
}
