package services;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;

public class ImageService {
    private static final int IMAGE_SIZE = 128;
    private static final int PADDING = 8; // borda preta de 8px em cima e à esquerda (igual ao dataset de treino)

    public static String createImage(String img) throws Exception {
        BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_BYTE_GRAY);

        // O código começa no pixel (PADDING, PADDING), não no (0, 0).
        // A área de padding já fica preta porque TYPE_BYTE_GRAY inicializa em 0.
        int x = PADDING;
        int y = PADDING;

        for (char c : img.toCharArray()) {

            if (c == '\n') {
                y++;
                x = PADDING;

                if (y >= IMAGE_SIZE) {
                    break;
                }
                continue;
            }

            if (x >= IMAGE_SIZE) {
                continue;
            }

            int grayValue = convertChar(c);

            int rgb = new Color(grayValue, grayValue, grayValue).getRGB();
            image.setRGB(x, y, rgb);
            x++;
        }
        return convertBase64(image);
    }

    ;

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

    ;

    public static String convertBase64(BufferedImage image) throws IOException {
        String format = "png";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            ImageIO.write(image, format, outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } finally {
            outputStream.close();
        }
    }

    ;

    public static String createImageHash(String image) throws IOException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] hash = messageDigest.digest(image.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
};

