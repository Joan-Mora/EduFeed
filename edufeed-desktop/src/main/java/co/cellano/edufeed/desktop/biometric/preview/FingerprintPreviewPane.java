package co.cellano.edufeed.desktop.biometric.preview;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

/** Vista previa de huella: puede recibir imagen por WebSocket desde un lector o móvil. */
public class FingerprintPreviewPane extends BorderPane implements PreviewControl {
    private final ImageView image = new ImageView();
    private final Label helper = new Label("Conecte un lector de huella o use el móvil como lector.\nLa vista previa aparecerá aquí.\nDESKTOP_FP_WS_URL=ws://host:puerto/ruta (opcional)");
    private WebSocket ws;
    private String wsUrl;

    public FingerprintPreviewPane() {
        setPadding(new Insets(8));
        image.setPreserveRatio(true);
        image.setFitWidth(220);
        image.setFitHeight(220);
        setTop(helper);
        setCenter(image);
        wsUrl = System.getenv().getOrDefault("DESKTOP_FP_WS_URL", "");
    }

    public void setFingerprintBytes(byte[] pngOrJpg) {
        if (pngOrJpg == null || pngOrJpg.length == 0) return;
        Platform.runLater(() -> image.setImage(new Image(new java.io.ByteArrayInputStream(pngOrJpg))));
    }

    @Override public void start() {
        if (ws != null) return;
        if (wsUrl == null || wsUrl.isBlank()) {
            return; // sin WS configurado, queda en placeholder
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            ws = client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new BinaryAccumListener(this::setFingerprintBytes, this::setStatus))
                    .join();
            setStatus("Conectado a " + wsUrl);
        } catch (Exception e) {
            setStatus("WS error: " + e.getMessage());
            ws = null;
        }
    }

    @Override public void stop() {
        if (ws != null) {
            try { ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye"); } catch (Exception ignore) {}
            ws = null;
        }
    }

    private void setStatus(String txt) { Platform.runLater(() -> helper.setText(txt)); }

    /** Listener que acumula binarios fragmentados y notifica una vez listo. */
    static class BinaryAccumListener implements Listener {
        private final java.util.function.Consumer<byte[]> onImage;
        private final java.util.function.Consumer<String> onStatus;
        private final List<ByteBuffer> parts = new ArrayList<>();

        BinaryAccumListener(java.util.function.Consumer<byte[]> onImage, java.util.function.Consumer<String> onStatus) {
            this.onImage = onImage; this.onStatus = onStatus;
        }

        @Override public void onOpen(WebSocket webSocket) {
            onStatus.accept("WS abierto");
            Listener.super.onOpen(webSocket);
        }

        @Override public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            parts.add(data);
            if (last) {
                int total = parts.stream().mapToInt(ByteBuffer::remaining).sum();
                byte[] all = new byte[total];
                int offset = 0;
                for (ByteBuffer b : parts) { int r = b.remaining(); b.get(all, offset, r); offset += r; }
                parts.clear();
                onImage.accept(all);
            }
            webSocket.request(1);
            return null;
        }

        @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (last) onStatus.accept(data.toString());
            webSocket.request(1);
            return null;
        }

        @Override public void onError(WebSocket webSocket, Throwable error) {
            onStatus.accept("WS error: " + error.getMessage());
            Listener.super.onError(webSocket, error);
        }
    }
}
