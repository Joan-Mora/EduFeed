package co.cellano.edufeed.desktop.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Generador de códigos QR para registro biométrico.
 * Utiliza la librería ZXing para generar códigos QR de alta calidad.
 */
public class QRCodeGenerator {

    private static final int DEFAULT_SIZE = 300;
    private static final Color QR_COLOR = Color.BLACK;
    private static final Color BACKGROUND_COLOR = Color.WHITE;

    /**
     * Genera un código QR para el registro biométrico.
     *
     * @param url URL completa que será codificada en el QR
     * @return BufferedImage con el código QR generado
     * @throws WriterException si hay error al generar el QR
     */
    public static BufferedImage generateQRCode(String url) throws WriterException {
        return generateQRCode(url, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    /**
     * Genera un código QR con dimensiones personalizadas.
     *
     * @param url    URL completa que será codificada en el QR
     * @param width  Ancho del código QR en píxeles
     * @param height Alto del código QR en píxeles
     * @return BufferedImage con el código QR generado
     * @throws WriterException si hay error al generar el QR
     */
    public static BufferedImage generateQRCode(String url, int width, int height) throws WriterException {
        // Configurar opciones del QR
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // Nivel alto de corrección
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1); // Margen mínimo

        // Generar matriz de bits
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height, hints);

        // Convertir a BufferedImage
        return toBufferedImage(bitMatrix);
    }

    /**
     * Genera una URL completa para el registro biométrico.
     *
     * @param baseUrl   URL base del backend (ej: http://localhost:8080)
     * @param userId    ID del usuario
     * @param token     Token de sesión
     * @param sessionId ID de la sesión de registro
     * @return URL completa para el QR
     */
    public static String buildRegistrationUrl(String baseUrl, String userId, String token, String sessionId) {
        return String.format("%s/api/biometric/register?userId=%s&token=%s&sessionId=%s",
                baseUrl, userId, token, sessionId);
    }

    /**
     * Genera una URL para el registro de una modalidad específica.
     *
     * @param baseUrl   URL base del backend
     * @param userId    ID del usuario
     * @param token     Token de sesión
     * @param sessionId ID de la sesión
     * @param modalidad Modalidad a registrar (fingerprint, face, voice)
     * @return URL completa para el QR
     */
    public static String buildModalityUrl(String baseUrl, String userId, String token,
            String sessionId, String modalidad) {
        return String.format("%s/api/biometric/register/%s?userId=%s&token=%s&sessionId=%s",
                baseUrl, modalidad, userId, token, sessionId);
    }

    /**
     * Convierte una BitMatrix a BufferedImage.
     *
     * @param matrix Matriz de bits del código QR
     * @return BufferedImage del código QR
     */
    private static BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        // Pintar fondo
        graphics.setColor(BACKGROUND_COLOR);
        graphics.fillRect(0, 0, width, height);

        // Pintar código QR
        graphics.setColor(QR_COLOR);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (matrix.get(x, y)) {
                    graphics.fillRect(x, y, 1, 1);
                }
            }
        }

        graphics.dispose();
        return image;
    }

    /**
     * Genera un código QR con logo en el centro (opcional).
     *
     * @param url       URL a codificar
     * @param logoImage Imagen del logo a colocar en el centro
     * @return BufferedImage con QR y logo
     * @throws WriterException si hay error al generar el QR
     */
    public static BufferedImage generateQRCodeWithLogo(String url, BufferedImage logoImage)
            throws WriterException {
        BufferedImage qrImage = generateQRCode(url, DEFAULT_SIZE, DEFAULT_SIZE);

        if (logoImage == null) {
            return qrImage;
        }

        // Calcular tamaño del logo (20% del tamaño del QR)
        int logoWidth = qrImage.getWidth() / 5;
        int logoHeight = qrImage.getHeight() / 5;

        // Redimensionar logo si es necesario
        BufferedImage scaledLogo = new BufferedImage(logoWidth, logoHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaledLogo.createGraphics();
        g.drawImage(logoImage, 0, 0, logoWidth, logoHeight, null);
        g.dispose();

        // Dibujar logo en el centro del QR
        Graphics2D qrGraphics = qrImage.createGraphics();
        int logoX = (qrImage.getWidth() - logoWidth) / 2;
        int logoY = (qrImage.getHeight() - logoHeight) / 2;

        // Fondo blanco para el logo
        qrGraphics.setColor(Color.WHITE);
        qrGraphics.fillRect(logoX - 5, logoY - 5, logoWidth + 10, logoHeight + 10);

        // Dibujar logo
        qrGraphics.drawImage(scaledLogo, logoX, logoY, null);
        qrGraphics.dispose();

        return qrImage;
    }
}
