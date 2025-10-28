package co.cellano.edufeed.desktop.access;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CashierRedirectView extends BorderPane {
    private final Label msg = new Label();
    private final Label ubic = new Label();
    private final Label horario = new Label();
    private final Label ref = new Label();
    private final ImageView qr = new ImageView();

    public CashierRedirectView() {
        setPadding(new Insets(16));
        var title = new Label("Orientación a caja");
        title.setStyle("-fx-font-size:18; -fx-font-weight:bold;");
        setTop(title); BorderPane.setAlignment(title, Pos.CENTER);

        qr.setFitWidth(160); qr.setFitHeight(160); qr.setPreserveRatio(true);

        var info = new VBox(6,
                new Label("Mensaje:"), msg,
                new Label("Ubicación:"), ubic,
                new Label("Horario:"), horario,
                new Label("Referencia:"), ref
        );
        var center = new HBox(16, info, qr);
        center.setAlignment(Pos.CENTER_LEFT);
        center.setPadding(new Insets(8));
        setCenter(center);

        var printBtn = new Button("Imprimir referencia");
        printBtn.setOnAction(e -> getScene().getWindow().sizeToScene());
        setBottom(new HBox(printBtn));
    }

    public void setData(String mensaje, String ubicacion, String horarioTxt, String referencia, String codigoQR) {
        msg.setText(mensaje != null ? mensaje : "");
        ubic.setText(ubicacion != null ? ubicacion : "");
        horario.setText(horarioTxt != null ? horarioTxt : "");
        ref.setText(referencia != null ? referencia : "");
        if (codigoQR != null) {
            if (codigoQR.startsWith("data:image/")) {
                int comma = codigoQR.indexOf(',');
                if (comma > 0) {
                    String base64 = codigoQR.substring(comma + 1);
                    byte[] bytes = Base64.getDecoder().decode(base64);
                    qr.setImage(new Image(new ByteArrayInputStream(bytes)));
                }
            } else if (codigoQR.startsWith("http")) {
                qr.setImage(new Image(codigoQR, true));
            } else {
                // texto plano: sin imagen
                qr.setImage(null);
            }
        }
    }
}
