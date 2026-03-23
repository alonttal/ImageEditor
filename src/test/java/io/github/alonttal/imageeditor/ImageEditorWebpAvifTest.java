package io.github.alonttal.imageeditor;

import io.github.alonttal.imageeditor.io.CliToolRunner;
import io.github.alonttal.imageeditor.io.ImageFormat;
import io.github.alonttal.imageeditor.io.ImageIOHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
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
    private static boolean heifInfoAvailable;

    @BeforeAll
    static void checkTools() {
        webpAvailable = CliToolRunner.isToolAvailable("cwebp") && CliToolRunner.isToolAvailable("dwebp");
        avifAvailable = CliToolRunner.isToolAvailable("heif-enc") && CliToolRunner.resolveHeifDecoder() != null;
        heifInfoAvailable = CliToolRunner.isToolAvailable("heif-info");
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

    @Test
    void avifOutput10BitAnd444Chroma() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");
        assumeTrue(heifInfoAvailable, "heif-info not installed, skipping");

        // Create a colorful gradient image to encode
        BufferedImage img = createGradientImage(200, 150);
        Path pngInput = tempDir.resolve("gradient.png");
        ImageIO.write(img, "png", pngInput.toFile());
        Path avifOutput = tempDir.resolve("gradient.avif");

        ImageEditor.builder()
                .quality(ImageEditor.AVIF_QUALITY)
                .outputFormat(ImageFormat.AVIF)
                .build()
                .process(pngInput, avifOutput);

        // Verify 10-bit depth and 4:4:4 chroma via heif-info
        Process p = new ProcessBuilder("heif-info", avifOutput.toAbsolutePath().toString())
                .redirectErrorStream(true).start();
        String info = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        assertEquals(0, p.exitValue(), "heif-info failed: " + info);
        assertTrue(info.contains("bit depth: 10"), "Expected 10-bit depth, got: " + info);
        assertTrue(info.contains("4:4:4"), "Expected 4:4:4 chroma, got: " + info);
    }

    @Test
    void avifQualityAffectsFileSize() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        BufferedImage img = createGradientImage(200, 150);
        Path pngInput = tempDir.resolve("gradient_q.png");
        ImageIO.write(img, "png", pngInput.toFile());

        Path lowQ = tempDir.resolve("low.avif");
        Path highQ = tempDir.resolve("high.avif");

        ImageEditor.builder()
                .quality(0.20f)
                .outputFormat(ImageFormat.AVIF)
                .build()
                .process(pngInput, lowQ);

        ImageEditor.builder()
                .quality(0.90f)
                .outputFormat(ImageFormat.AVIF)
                .build()
                .process(pngInput, highQ);

        assertTrue(Files.size(lowQ) < Files.size(highQ),
                "Low quality (" + Files.size(lowQ) + "B) should be smaller than high quality (" + Files.size(highQ) + "B)");
    }

    @Test
    void avifFromRgbaSource() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        // Create an RGBA image with semi-transparent pixels
        BufferedImage rgba = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = rgba.createGraphics();
        g.setColor(new Color(255, 0, 0, 128));
        g.fillRect(0, 0, 50, 100);
        g.setColor(new Color(0, 0, 255, 200));
        g.fillRect(50, 0, 50, 100);
        g.dispose();

        Path pngInput = tempDir.resolve("rgba.png");
        ImageIO.write(rgba, "png", pngInput.toFile());
        Path avifOutput = tempDir.resolve("rgba.avif");

        ImageEditor.builder()
                .quality(0.5f)
                .outputFormat(ImageFormat.AVIF)
                .build()
                .process(pngInput, avifOutput);

        assertTrue(Files.exists(avifOutput));
        assertTrue(Files.size(avifOutput) > 0);

        BufferedImage result = ImageIOHandler.read(avifOutput);
        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void avifWriteWithNonAvifExtension() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        // The -A flag tells heif-enc to produce AVIF regardless of the output extension.
        // Without -A, heif-enc may produce HEIC when the extension is not .avif.
        BufferedImage img = createGradientImage(100, 80);
        Path pngInput = tempDir.resolve("non_avif_ext.png");
        ImageIO.write(img, "png", pngInput.toFile());
        Path heicOutput = tempDir.resolve("output.heic");

        ImageEditor.builder()
                .quality(0.5f)
                .outputFormat(ImageFormat.AVIF)
                .build()
                .process(pngInput, heicOutput);

        assertTrue(Files.exists(heicOutput));
        assertTrue(Files.size(heicOutput) > 0);

        // Verify the file contains AVIF data (ftyp box with 'avif' brand) despite the .heic extension
        byte[] header = Files.readAllBytes(heicOutput);
        assertTrue(header.length >= 12, "File too small to be valid AVIF");
        // ISOBMFF ftyp box: bytes 4-7 = "ftyp", bytes 8-11 = major brand
        String ftyp = new String(header, 4, 4);
        assertEquals("ftyp", ftyp, "Expected ISOBMFF ftyp box");
        String brand = new String(header, 8, 4);
        assertTrue(brand.equals("avif") || brand.equals("avis"),
                "Expected AVIF brand in ftyp box, got: " + brand);
    }

    @Test
    void avifWriteWithNoExtension() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        // Verify -A flag allows writing AVIF to a path with no file extension
        BufferedImage img = createGradientImage(100, 80);
        Path pngInput = tempDir.resolve("no_ext_input.png");
        ImageIO.write(img, "png", pngInput.toFile());
        Path noExtOutput = tempDir.resolve("output_no_ext");

        ImageEditor.builder()
                .quality(0.5f)
                .outputFormat(ImageFormat.AVIF)
                .build()
                .process(pngInput, noExtOutput);

        assertTrue(Files.exists(noExtOutput));
        assertTrue(Files.size(noExtOutput) > 0);

        // Verify the output is valid AVIF
        byte[] header = Files.readAllBytes(noExtOutput);
        assertTrue(header.length >= 12);
        String ftyp = new String(header, 4, 4);
        assertEquals("ftyp", ftyp);
        String brand = new String(header, 8, 4);
        assertTrue(brand.equals("avif") || brand.equals("avis"),
                "Expected AVIF brand, got: " + brand);
    }

    @Tag("slow")
    @Test
    void avifConversionDoesNotLeakMemory() throws Exception {
        assumeTrue(avifAvailable, "AVIF tools not installed, skipping");

        int iterations = 2000;
        Path pngInput = tempDir.resolve("leak_test.png");
        ImageIO.write(createGradientImage(400, 300), "png", pngInput.toFile());

        ImageEditor editor = ImageEditor.builder()
                .resize(200, 150)
                .quality(0.5f)
                .outputFormat(ImageFormat.AVIF)
                .build();

        // Warm up and establish baseline
        for (int i = 0; i < 10; i++) {
            Path out = tempDir.resolve("warmup_" + i + ".avif");
            editor.process(pngInput, out);
            Files.delete(out);
        }
        System.gc();
        Thread.sleep(200);
        long baselineMemory = usedMemory();

        // Run many conversions, checking memory periodically
        long peakMemory = baselineMemory;
        for (int i = 0; i < iterations; i++) {
            Path out = tempDir.resolve("leak_" + i + ".avif");
            editor.process(pngInput, out);
            Files.delete(out);

            if (i % 200 == 199) {
                System.gc();
                Thread.sleep(100);
                long currentMemory = usedMemory();
                peakMemory = Math.max(peakMemory, currentMemory);
            }
        }

        System.gc();
        Thread.sleep(200);
        long finalMemory = usedMemory();

        // Memory growth should stay within 30 MB of baseline.
        // Without the leak fixes, hundreds of unclosed streams and unflushed
        // BufferedImages would push this well beyond the threshold.
        long growthBytes = finalMemory - baselineMemory;
        long thresholdBytes = 30L * 1024 * 1024;
        assertTrue(growthBytes < thresholdBytes,
                "Memory grew by " + (growthBytes / (1024 * 1024)) + " MB after " + iterations
                        + " AVIF conversions (baseline=" + (baselineMemory / (1024 * 1024))
                        + " MB, final=" + (finalMemory / (1024 * 1024))
                        + " MB, threshold=" + (thresholdBytes / (1024 * 1024)) + " MB)");
    }

    private static long usedMemory() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    @Test
    void avifQualityConstant() {
        assertEquals(0.46f, ImageEditor.AVIF_QUALITY);
    }

    private BufferedImage createGradientImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = (x * 255) / width;
                int g = (y * 255) / height;
                int b = ((x + y) * 255) / (width + height);
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return img;
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

}
