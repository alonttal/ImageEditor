package com.imageeditor;

import com.imageeditor.io.ImageFormat;
import com.imageeditor.io.ImageIOHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
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

        Path webpInput = createWebpImage(200, 150);
        Path webpOutput = tempDir.resolve("resized.webp");

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

        Path webpInput = createWebpImage(200, 200);
        Path webpOutput = tempDir.resolve("cropped.webp");

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

        Path webpInput = createWebpImage(300, 200);
        Path webpOutput = tempDir.resolve("covered.webp");

        ImageEditor.builder()
                .cover(100, 100)
                .build()
                .process(webpInput, webpOutput);

        BufferedImage result = ImageIOHandler.read(webpOutput);
        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void coverAvif() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        Path avifInput = createAvifImage(300, 200);
        Path avifOutput = tempDir.resolve("covered.avif");

        ImageEditor.builder()
                .cover(100, 100)
                .build()
                .process(avifInput, avifOutput);

        BufferedImage result = ImageIOHandler.read(avifOutput);
        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void fitWebp() throws Exception {
        assumeTrue(webpAvailable, "WebP tools not installed, skipping");

        Path webpInput = createWebpImage(300, 200);
        Path webpOutput = tempDir.resolve("fitted.webp");

        ImageEditor.builder()
                .fit(100, 100)
                .build()
                .process(webpInput, webpOutput);

        BufferedImage result = ImageIOHandler.read(webpOutput);
        assertEquals(100, result.getWidth());
        assertEquals(67, result.getHeight());
    }

    @Test
    void fitAvif() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        Path avifInput = createAvifImage(300, 200);
        Path avifOutput = tempDir.resolve("fitted.avif");

        ImageEditor.builder()
                .fit(100, 100)
                .build()
                .process(avifInput, avifOutput);

        BufferedImage result = ImageIOHandler.read(avifOutput);
        assertEquals(100, result.getWidth());
        assertEquals(67, result.getHeight());
    }

    @Test
    void scaleDownWebp() throws Exception {
        assumeTrue(webpAvailable, "WebP tools not installed, skipping");

        Path webpInput = createWebpImage(300, 200);
        Path webpOutput = tempDir.resolve("scaleddown.webp");

        ImageEditor.builder()
                .scaleDown(100, 100)
                .build()
                .process(webpInput, webpOutput);

        BufferedImage result = ImageIOHandler.read(webpOutput);
        assertEquals(100, result.getWidth());
        assertEquals(67, result.getHeight());
    }

    @Test
    void scaleDownAvif() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        Path avifInput = createAvifImage(300, 200);
        Path avifOutput = tempDir.resolve("scaleddown.avif");

        ImageEditor.builder()
                .scaleDown(100, 100)
                .build()
                .process(avifInput, avifOutput);

        BufferedImage result = ImageIOHandler.read(avifOutput);
        assertEquals(100, result.getWidth());
        assertEquals(67, result.getHeight());
    }

    @Test
    void resizeAvif() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        Path avifInput = createAvifImage(200, 150);
        Path avifOutput = tempDir.resolve("resized.avif");

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

        Path avifInput = createAvifImage(200, 200);
        Path avifOutput = tempDir.resolve("cropped.avif");

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

        Path webpInput = createWebpImage(120, 90);
        Path pngOutput = tempDir.resolve("output.png");

        ImageEditor.builder()
                .resize(60, 45)
                .outputFormat(ImageFormat.PNG)
                .build()
                .process(webpInput, pngOutput);

        BufferedImage result = ImageIO.read(pngOutput.toFile());
        assertEquals(60, result.getWidth());
        assertEquals(45, result.getHeight());
    }

    @Test
    void webpQualityControl() throws Exception {
        assumeTrue(webpAvailable, "WebP tools not installed, skipping");

        Path webpInput = createWebpImage(100, 100);
        Path webpOutput = tempDir.resolve("quality.webp");

        ImageEditor.builder()
                .quality(0.5f)
                .build()
                .process(webpInput, webpOutput);

        assertTrue(Files.exists(webpOutput));
        assertTrue(Files.size(webpOutput) > 0);
    }

    @Test
    void avifQualityControl() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        Path avifInput = createAvifImage(100, 100);
        Path avifOutput = tempDir.resolve("quality.avif");

        ImageEditor.builder()
                .quality(0.5f)
                .build()
                .process(avifInput, avifOutput);

        assertTrue(Files.exists(avifOutput));
        assertTrue(Files.size(avifOutput) > 0);
    }

    private Path createWebpImage(int width, int height) throws IOException, InterruptedException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Path png = tempDir.resolve("src_" + width + "x" + height + ".png");
        ImageIO.write(img, "png", png.toFile());

        Path webp = tempDir.resolve("src_" + width + "x" + height + ".webp");
        Process p = new ProcessBuilder("cwebp", png.toAbsolutePath().toString(), "-o", webp.toAbsolutePath().toString())
                .redirectErrorStream(true).start();
        p.waitFor();
        assertEquals(0, p.exitValue(), "cwebp failed");
        return webp;
    }

    private Path createAvifImage(int width, int height) throws IOException, InterruptedException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Path png = tempDir.resolve("src_" + width + "x" + height + ".png");
        ImageIO.write(img, "png", png.toFile());

        Path avif = tempDir.resolve("src_" + width + "x" + height + ".avif");
        Process p = new ProcessBuilder("heif-enc", png.toAbsolutePath().toString(), "-o", avif.toAbsolutePath().toString())
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
