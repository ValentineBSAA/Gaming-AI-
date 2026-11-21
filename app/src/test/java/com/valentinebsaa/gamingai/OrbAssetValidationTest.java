package com.valentinebsaa.gamingai;

import static org.junit.Assert.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.Test;

/** Build gate for the three grayscale, alpha-preserving reusable PNG layers. */
public final class OrbAssetValidationTest {
    private static final String[] LAYERS = {"orb_core", "orb_petals", "orb_ring"};

    @Test public void sourcePngLayersHaveRealTransparencyAndNeutralPixels() throws Exception {
        for (String layer : LAYERS) {
            File file = new File("src/main/res/drawable-nodpi/" + layer + ".png");
            assertTrue(layer + " is missing", file.isFile());
            BufferedImage image = ImageIO.read(file);
            assertNotNull(layer + " is not a valid PNG", image);
            assertTrue(layer + " resolution is insufficient", image.getWidth() >= 64 && image.getHeight() >= 64);
            assertTrue(layer + " must have alpha", image.getColorModel().hasAlpha());
            int visible = 0, clear = 0, shades = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int px = image.getRGB(x, y);
                    int alpha = (px >>> 24) & 255;
                    int red = (px >>> 16) & 255;
                    int green = (px >>> 8) & 255;
                    int blue = px & 255;
                    if (alpha == 0) { clear++; continue; }
                    visible++;
                    assertTrue(layer + " embeds chroma at " + x + "," + y,
                        Math.abs(red - green) <= 1 && Math.abs(green - blue) <= 1);
                    if (red > 15 && red < 240) shades++;
                }
            }
            assertTrue(layer + " appears blank", visible > 40);
            assertTrue(layer + " background is not transparent", clear > 40);
            assertTrue(layer + " has no useful grayscale shading", shades > 30);
        }
    }
}
