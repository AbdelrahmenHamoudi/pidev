package org.example.Services.user.facelogin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class ImageBase64 {

    /**
     * Convertit une BufferedImage en JPEG Base64.
     * (Version simple: qualité JPEG gérée par l'encodeur par défaut ImageIO)
     */
    public static String toJpegBase64(BufferedImage img, float qualityIgnored) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
}