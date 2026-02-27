package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ImageIOHandlerTest {

    @TempDir
    Path tempDir;

    @Test
    void readAndWritePng() throws IOException {
        File file = createTestImage(80, 60, "png");

        BufferedImage image = ImageIOHandler.read(file);
        assertEquals(80, image.getWidth());
        assertEquals(60, image.getHeight());

        File output = tempDir.resolve("out.png").toFile();
        ImageIOHandler.write(image, output);

        BufferedImage result = ImageIO.read(output);
        assertEquals(80, result.getWidth());
        assertEquals(60, result.getHeight());
    }

    @Test
    void readAndWriteJpg() throws IOException {
        File file = createTestImage(80, 60, "jpeg");
        File jpgFile = tempDir.resolve("input.jpg").toFile();
        file.renameTo(jpgFile);

        BufferedImage image = ImageIOHandler.read(jpgFile);
        assertEquals(80, image.getWidth());

        File output = tempDir.resolve("out.jpg").toFile();
        ImageIOHandler.write(image, output);

        BufferedImage result = ImageIO.read(output);
        assertEquals(80, result.getWidth());
    }

    @Test
    void readAndWriteWebp() throws IOException, InterruptedException {
        assumeTrue(isToolAvailable("cwebp"), "cwebp not installed, skipping");
        assumeTrue(isToolAvailable("dwebp"), "dwebp not installed, skipping");

        // Create a PNG, then convert to WebP via cwebp
        File png = createTestImage(64, 48, "png");
        File webp = tempDir.resolve("test.webp").toFile();
        new ProcessBuilder("cwebp", png.getAbsolutePath(), "-o", webp.getAbsolutePath())
                .redirectErrorStream(true).start().waitFor();

        BufferedImage image = ImageIOHandler.read(webp);
        assertEquals(64, image.getWidth());
        assertEquals(48, image.getHeight());

        File outputWebp = tempDir.resolve("out.webp").toFile();
        ImageIOHandler.write(image, outputWebp);
        assertTrue(outputWebp.exists());
        assertTrue(outputWebp.length() > 0);

        // Verify round-trip by reading back
        BufferedImage roundTrip = ImageIOHandler.read(outputWebp);
        assertEquals(64, roundTrip.getWidth());
        assertEquals(48, roundTrip.getHeight());
    }

    @Test
    void readAndWriteAvif() throws IOException, InterruptedException {
        assumeTrue(isToolAvailable("heif-enc"), "heif-enc not installed, skipping");
        assumeTrue(isToolAvailable("heif-dec"), "heif-dec not installed, skipping");

        // Create a PNG, then convert to AVIF via heif-enc
        File png = createTestImage(64, 48, "png");
        File avif = tempDir.resolve("test.avif").toFile();
        new ProcessBuilder("heif-enc", png.getAbsolutePath(), "-o", avif.getAbsolutePath())
                .redirectErrorStream(true).start().waitFor();

        BufferedImage image = ImageIOHandler.read(avif);
        assertEquals(64, image.getWidth());
        assertEquals(48, image.getHeight());

        File outputAvif = tempDir.resolve("out.avif").toFile();
        ImageIOHandler.write(image, outputAvif);
        assertTrue(outputAvif.exists());
        assertTrue(outputAvif.length() > 0);

        // Verify round-trip by reading back
        BufferedImage roundTrip = ImageIOHandler.read(outputAvif);
        assertEquals(64, roundTrip.getWidth());
        assertEquals(48, roundTrip.getHeight());
    }

    @Test
    void unsupportedFormatThrows() {
        File file = tempDir.resolve("image.xyz").toFile();
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.read(file));
    }

    @Test
    void noExtensionThrows() {
        File file = tempDir.resolve("noext").toFile();
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.read(file));
    }

    @Test
    void standardFormatsAlwaysSupported() {
        assertTrue(ImageIOHandler.isFormatSupported("png"));
        assertTrue(ImageIOHandler.isFormatSupported("jpg"));
        assertTrue(ImageIOHandler.isFormatSupported("jpeg"));
        assertTrue(ImageIOHandler.isFormatSupported("gif"));
        assertTrue(ImageIOHandler.isFormatSupported("bmp"));
        assertTrue(ImageIOHandler.isFormatSupported("PNG"));
    }

    @Test
    void webpSupportedWhenToolsInstalled() {
        assumeTrue(isToolAvailable("cwebp"), "cwebp not installed, skipping");
        assumeTrue(isToolAvailable("dwebp"), "dwebp not installed, skipping");
        assertTrue(ImageIOHandler.isFormatSupported("webp"));
    }

    @Test
    void avifSupportedWhenToolsInstalled() {
        assumeTrue(isToolAvailable("heif-enc"), "heif-enc not installed, skipping");
        assumeTrue(isToolAvailable("heif-dec"), "heif-dec not installed, skipping");
        assertTrue(ImageIOHandler.isFormatSupported("avif"));
    }

    @Test
    void unknownFormatNotSupported() {
        assertFalse(ImageIOHandler.isFormatSupported("xyz"));
    }

    private File createTestImage(int width, int height, String format) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        File file = tempDir.resolve("input." + format).toFile();
        ImageIO.write(img, format, file);
        return file;
    }

    private boolean isToolAvailable(String tool) {
        try {
            Process p = new ProcessBuilder("which", tool)
                    .redirectErrorStream(true).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
