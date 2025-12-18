package src;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PawnAsset {
    String path;
    BufferedImage image;
    Color themeColor;

    public PawnAsset(String path, Color themeColor) {
        this.path = path;
        this.themeColor = themeColor;
        try {
            this.image = ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("Gagal load gambar: " + path);
            // Fallback: Buat kotak warna jika gambar tidak ketemu
            this.image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = this.image.createGraphics();
            g.setColor(themeColor);
            g.fillRect(0, 0, 50, 50);
            g.dispose();
        }
    }
}
