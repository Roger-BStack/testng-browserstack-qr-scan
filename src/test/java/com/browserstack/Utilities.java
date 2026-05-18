package com.browserstack;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.jcodec.api.awt.AWTSequenceEncoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility class providing QR code generation/scanning and image-to-video encoding.
 */
public class Utilities {

    /**
     * Generates a QR code image for the given URL and saves it to a file.
     *
     * @param url        The URL to encode in the QR code.
     * @param outputPath The file path where the QR code PNG will be saved.
     * @param width      Width of the QR code image in pixels.
     * @param height     Height of the QR code image in pixels.
     * @throws Exception if QR code generation or file writing fails.
     */
    public static void generateQRCode(String url, String outputPath, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height, hints);

        Path path = FileSystems.getDefault().getPath(outputPath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }

    public static void pasteImageCenter(BufferedImage background, BufferedImage foreground) {
        // 1. Get Graphics2D object
        Graphics2D g2d = background.createGraphics();

        // 2. Calculate coordinates
        int x = (background.getWidth() - foreground.getWidth()) / 2;
        int y = (background.getHeight() - foreground.getHeight()) / 2;

        // 3. Draw the image and dispose of graphics
        g2d.drawImage(foreground, x, y, null);
        g2d.dispose();
    }

    /**
     * Encodes a single image file into an MP4 video using JCodec and returns the output file path.
     *
     * @param imageFilePath Path to the source image file.
     * @return Path to the generated MP4 file.
     * @throws IOException if reading the image or writing the video fails.
     */
    public static String encodeImageToMp4(String imageFilePath) throws IOException {
        File imageFile = new File(imageFilePath);
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("Could not read image from: " + imageFilePath);
        }

        // Ensure dimensions are even (required by most video codecs)
        int width = image.getWidth() % 2 == 0 ? image.getWidth() : image.getWidth() - 1;;
        int height = image.getHeight() % 2 == 0 ? image.getHeight() : image.getHeight() - 1;

        // Size of border around QR Code
        int border = 100;

        // 1. Create the BufferedImage for the background
        // Use TYPE_INT_RGB for opaque images or TYPE_INT_ARGB for transparency
        BufferedImage backgroundImage = new BufferedImage(width + (border * 2),
                height + (border * 2), BufferedImage.TYPE_INT_RGB);

        // 2. Get the Graphics2D context
        Graphics2D g2d = backgroundImage.createGraphics();

        // 3. Set the desired color and fill the entire background image
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        // 4. Dispose of the graphics context to free resources
        g2d.dispose();

        pasteImageCenter(backgroundImage, image);

        String outputPath = imageFilePath.replaceAll("\\.[^.]+$", "") + "_output.mp4";
        File outputFile = new File(outputPath);

        AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(outputFile, 1);
        try {
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            frame.getGraphics().drawImage(backgroundImage, 0, 0, width, height, null);
            encoder.encodeImage(frame);
        } finally {
            encoder.finish();
        }

        return outputPath;
    }

    /**
     * Scans a BufferedImage for a QR code and returns any detected http/https URL.
     *
     * @param image The BufferedImage to scan.
     * @return The http/https URL found in the QR code, an empty string if QR data contains no http/https link,
     *         or null if no QR code was detected.
     */
    public static String scanQRCodeFromImage(BufferedImage image) {
        if (image == null) {
            return null;
        }
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            Result result = new MultiFormatReader().decode(bitmap, hints);
            String text = result.getText();
            if (text != null && (text.startsWith("http://") || text.startsWith("https://"))) {
                return text;
            }
            return "";
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Retrieves the media_name for a given media_id from the BrowserStack recent media files API.
     *
     * @param mediaId   The media_id to search for.
     * @param username  BrowserStack username.
     * @param accessKey BrowserStack access key.
     * @return The media_name if found, or null if not found.
     */
    public static String getBsMediaNameById(String mediaId, String username, String accessKey) {
        try {
            URL url = new URL("https://api-cloud.browserstack.com/automate/recent_media_files");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            String credentials = username + ":" + accessKey;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encoded);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            java.io.InputStream is = conn.getInputStream();
            String responseBody = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            is.close();

            JSONArray mediaFiles = new JSONArray(responseBody);
            for (int i = 0; i < mediaFiles.length(); i++) {
                JSONObject obj = mediaFiles.getJSONObject(i);
                if (obj.has("media_id") && mediaId.equals(obj.getString("media_id"))) {
                    return obj.optString("media_name", null);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String scanQRCodeFromBytes(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                return null;
            }
            return scanQRCodeFromImage(image);
        } catch (IOException e) {
            return null;
        }
    }
}