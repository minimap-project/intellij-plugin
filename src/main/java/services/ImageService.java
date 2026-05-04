package services;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;

public class ImageService {
    public static String createImage(String img) throws Exception {
        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_BYTE_GRAY);

        int x = 0;
        int y = 0;

        for (char c : img.toCharArray()) {

            if (c == '\n') {
                y++;
                x = 0;

                if (y >= 128) {
                    break;
                }
                continue;
            }

            if (x >= 128) {
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
        if (ascii >= 0 && ascii <= 32) {
            return 0;
        } else if (ascii >= 48 && ascii <= 57) {
            return 53;
        } else if (ascii >= 65 && ascii <= 90) {
            return 77;
        } else if (ascii >= 97 && ascii <= 122) {
            return 109;
        } else if (ascii >= 122 && ascii <= 127) {
            return ascii;
        } else {
            return 127;
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

