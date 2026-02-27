package com.imageeditor;

import com.imageeditor.io.ImageIOHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ImageEditorWebpAvifTest {

    @TempDir
    Path tempDir;

    private static boolean webpAvailable;
    private static boolean avifAvailable;

    @BeforeAll
    static void checkTools() {
        webpAvailable = isToolAvailable("cwebp") && isToolAvailable("dwebp");
        avifAvailable = isToolAvailable("heif-enc") && isToolAvailable("heif-dec");
    }

    @Test
    void resizeWebp() throws Exception {
        assumeTrue(webpAvailable, "WebP tools not installed, skipping");

        File webpInput = createWebpImage(200, 150);
        File webpOutput = tempDir.resolve("resized.webp").toFile();

        ImageEditor.builder()
                .resize(100, 75)
                .build()
                .process(webpInput, webpOutput);

        BufferedImage result = ImageIOHandler.read(webpOutput);
        assertEquals(100, result.getWidth());
        assertEquals(75, result.getHeight());
    }

    @Test
    void cropWebp() throws Exception {
        assumeTrue(webpAvailable, "WebP tools not installed, skipping");

        File webpInput = createWebpImage(200, 200);
        File webpOutput = tempDir.resolve("cropped.webp").toFile();

        ImageEditor.builder()
                .crop(10, 10, 100, 80)
                .build()
                .process(webpInput, webpOutput);

        BufferedImage result = ImageIOHandler.read(webpOutput);
        assertEquals(100, result.getWidth());
        assertEquals(80, result.getHeight());
    }

    @Test
    void coverWebp() throws Exception {
        assumeTrue(webpAvailable, "WebP tools not installed, skipping");

        File webpInput = createWebpImage(300, 200);
        File webpOutput = tempDir.resolve("covered.webp").toFile();

        ImageEditor.builder()
                .cover(100, 100)
                .build()
                .process(webpInput, webpOutput);

        BufferedImage result = ImageIOHandler.read(webpOutput);
        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void resizeAvif() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        File avifInput = createAvifImage(200, 150);
        File avifOutput = tempDir.resolve("resized.avif").toFile();

        ImageEditor.builder()
                .resize(100, 75)
                .build()
                .process(avifInput, avifOutput);

        BufferedImage result = ImageIOHandler.read(avifOutput);
        assertEquals(100, result.getWidth());
        assertEquals(75, result.getHeight());
    }

    @Test
    void cropAvif() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        File avifInput = createAvifImage(200, 200);
        File avifOutput = tempDir.resolve("cropped.avif").toFile();

        ImageEditor.builder()
                .crop(10, 10, 100, 80)
                .build()
                .process(avifInput, avifOutput);

        BufferedImage result = ImageIOHandler.read(avifOutput);
        assertEquals(100, result.getWidth());
        assertEquals(80, result.getHeight());
    }

    @Test
    void webpToPngConversion() throws Exception {
        assumeTrue(webpAvailable, "WebP tools not installed, skipping");

        File webpInput = createWebpImage(120, 90);
        File pngOutput = tempDir.resolve("output.png").toFile();

        ImageEditor.builder()
                .resize(60, 45)
                .build()
                .process(webpInput, pngOutput);

        BufferedImage result = ImageIO.read(pngOutput);
        assertEquals(60, result.getWidth());
        assertEquals(45, result.getHeight());
    }

    private File createWebpImage(int width, int height) throws IOException, InterruptedException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        File png = tempDir.resolve("src_" + width + "x" + height + ".png").toFile();
        ImageIO.write(img, "png", png);

        File webp = tempDir.resolve("src_" + width + "x" + height + ".webp").toFile();
        Process p = new ProcessBuilder("cwebp", png.getAbsolutePath(), "-o", webp.getAbsolutePath())
                .redirectErrorStream(true).start();
        p.waitFor();
        assertEquals(0, p.exitValue(), "cwebp failed");
        return webp;
    }

    private File createAvifImage(int width, int height) throws IOException, InterruptedException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        File png = tempDir.resolve("src_" + width + "x" + height + ".png").toFile();
        ImageIO.write(img, "png", png);

        File avif = tempDir.resolve("src_" + width + "x" + height + ".avif").toFile();
        Process p = new ProcessBuilder("heif-enc", png.getAbsolutePath(), "-o", avif.getAbsolutePath())
                .redirectErrorStream(true).start();
        p.waitFor();
        assertEquals(0, p.exitValue(), "heif-enc failed");
        return avif;
    }

    private static boolean isToolAvailable(String tool) {
        try {
            Process p = new ProcessBuilder("which", tool)
                    .redirectErrorStream(true).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
