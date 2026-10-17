package services;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class ImageService {
    public static final int IMAGE_SIZE = 128;
    public static final int PADDING = 8; // borda preta de 8px em cima e à esquerda (igual ao dataset de treino)

    /**
     * Extrai a matriz 128x128 de intensidade de cinza a partir do código-fonte.
     */
    public static int[][] getPixelIntensities(String sourceCode) {
        int[][] pixels = new int[IMAGE_SIZE][IMAGE_SIZE];
        String normalized = sourceCode.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);

        for (int row = 0; row < IMAGE_SIZE; row++) {
            for (int col = 0; col < IMAGE_SIZE; col++) {
                if (row < PADDING || col < PADDING) {
                    pixels[row][col] = 0;
                    continue;
                }

                int sourceRow = row - PADDING;
                int sourceCol = col - PADDING;

                if (sourceRow < lines.length && sourceCol < lines[sourceRow].length()) {
                    char c = lines[sourceRow].charAt(sourceCol);
                    pixels[row][col] = convertChar(c);
                } else {
                    pixels[row][col] = 0;
                }
            }
        }
        return pixels;
    }

    /**
     * Cria um BufferedImage em escala de cinza a partir da matriz de intensidades.
     */
    public static BufferedImage createBufferedImage(int[][] pixels) {
        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        for (int r = 0; r < IMAGE_SIZE; r++) {
            for (int c = 0; c < IMAGE_SIZE; c++) {
                int gray = Math.max(0, Math.min(255, pixels[r][c]));
                int rgb = new Color(gray, gray, gray).getRGB();
                image.setRGB(c, r, rgb);
            }
        }
        return image;
    }

    /**
     * Gera a imagem em Base64 para o código-fonte (compatível com a versão anterior).
     */
    public static String createImage(String sourceCode) throws Exception {
        int[][] pixels = getPixelIntensities(sourceCode);
        BufferedImage image = createBufferedImage(pixels);
        return convertBase64(image);
    }

    /**
     * Constrói a imagem média pixel a pixel calculando a média aritmética.
     */
    public static BufferedImage createAverageImage(long[][] sumPixels, int count) {
        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        if (count <= 0) {
            return image;
        }

        for (int r = 0; r < IMAGE_SIZE; r++) {
            for (int c = 0; c < IMAGE_SIZE; c++) {
                int avgGray = Math.round((float) sumPixels[r][c] / count);
                avgGray = Math.max(0, Math.min(255, avgGray));
                int rgb = new Color(avgGray, avgGray, avgGray).getRGB();
                image.setRGB(c, r, rgb);
            }
        }
        return image;
    }

    public static int convertChar(char c) {
        int ascii = (int) c;
        if (ascii < 32) {
            return 0;            // controles (newline, tab, etc.) → preto
        } else if (ascii >= 48 && ascii <= 57) {
            return 53;           // dígitos → cinza 53
        } else if (ascii >= 65 && ascii <= 90) {
            return 77;           // maiúsculas → cinza 77
        } else if (ascii >= 97 && ascii <= 122) {
            return 109;          // minúsculas → cinza 109
        } else {
            return ascii;        // resto (espaço=32, pontuação, símbolos) → valor ASCII cru
        }
    }

    public static String convertBase64(BufferedImage image) throws IOException {
        String format = "png";
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }

    public static String createImageHash(String image) throws IOException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] hash = messageDigest.digest(image.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static String md5Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }
}
