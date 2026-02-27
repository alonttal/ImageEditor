package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
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

    // --- Quality tests ---

    @Test
    void writeJpegWithDifferentQualityProducesDifferentSizes() throws IOException {
        BufferedImage image = createRichTestImage(200, 200);

        File highQuality = tempDir.resolve("high.jpg").toFile();
        File lowQuality = tempDir.resolve("low.jpg").toFile();

        OutputOptions highOpts = OutputOptions.builder().quality(0.95f).build();
        OutputOptions lowOpts = OutputOptions.builder().quality(0.1f).build();

        ImageIOHandler.write(image, highQuality, highOpts);
        ImageIOHandler.write(image, lowQuality, lowOpts);

        assertTrue(highQuality.length() > lowQuality.length(),
                "High quality file (" + highQuality.length() + ") should be larger than low quality ("
                        + lowQuality.length() + ")");
    }

    // --- Metadata stripping tests ---

    @Test
    void writeWithStripMetadata() throws IOException {
        BufferedImage image = createRichTestImage(100, 100);
        File output = tempDir.resolve("stripped.png").toFile();

        OutputOptions opts = OutputOptions.builder().stripMetadata(true).build();
        ImageIOHandler.write(image, output, opts);

        BufferedImage result = ImageIO.read(output);
        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    // --- Format detection tests ---

    @Test
    void detectFormatPng() throws IOException {
        File file = createTestImage(10, 10, "png");
        assertEquals("png", ImageIOHandler.detectFormat(file));
    }

    @Test
    void detectFormatJpeg() throws IOException {
        File file = createTestImage(10, 10, "jpeg");
        File jpgFile = tempDir.resolve("detect.jpg").toFile();
        file.renameTo(jpgFile);
        assertEquals("jpeg", ImageIOHandler.detectFormat(jpgFile));
    }

    @Test
    void detectFormatGif() throws IOException {
        File file = tempDir.resolve("detect.gif").toFile();
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "gif", file);
        assertEquals("gif", ImageIOHandler.detectFormat(file));
    }

    @Test
    void detectFormatBmp() throws IOException {
        File file = tempDir.resolve("detect.bmp").toFile();
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "bmp", file);
        assertEquals("bmp", ImageIOHandler.detectFormat(file));
    }

    @Test
    void detectFormatWebp() throws IOException, InterruptedException {
        assumeTrue(isToolAvailable("cwebp"), "cwebp not installed, skipping");
        File png = createTestImage(10, 10, "png");
        File webp = tempDir.resolve("detect.webp").toFile();
        new ProcessBuilder("cwebp", png.getAbsolutePath(), "-o", webp.getAbsolutePath())
                .redirectErrorStream(true).start().waitFor();
        assertEquals("webp", ImageIOHandler.detectFormat(webp));
    }

    @Test
    void detectFormatFromInputStream() throws IOException {
        File file = createTestImage(10, 10, "png");
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            assertEquals("png", ImageIOHandler.detectFormat(is));
            // Stream should still be readable after detection (mark/reset)
            BufferedImage img = ImageIO.read(is);
            assertNotNull(img);
        }
    }

    @Test
    void detectFormatFallsBackToExtension() throws IOException {
        // Create a file with unknown content but valid extension
        File file = tempDir.resolve("fallback.png").toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[]{0x00, 0x00, 0x00}); // garbage bytes
        }
        assertEquals("png", ImageIOHandler.detectFormat(file));
    }

    // --- Stream-based I/O tests ---

    @Test
    void streamReadWriteRoundTrip() throws IOException {
        File file = createTestImage(60, 40, "png");

        BufferedImage image;
        try (InputStream is = new FileInputStream(file)) {
            image = ImageIOHandler.read(is, "png");
        }
        assertEquals(60, image.getWidth());
        assertEquals(40, image.getHeight());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIOHandler.write(image, baos, "png", OutputOptions.defaults());

        // Read back from byte array
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(60, result.getWidth());
        assertEquals(40, result.getHeight());
    }

    @Test
    void streamReadWriteJpegWithQuality() throws IOException {
        BufferedImage image = createRichTestImage(100, 100);

        ByteArrayOutputStream highBaos = new ByteArrayOutputStream();
        ByteArrayOutputStream lowBaos = new ByteArrayOutputStream();

        OutputOptions highOpts = OutputOptions.builder().quality(0.95f).build();
        OutputOptions lowOpts = OutputOptions.builder().quality(0.1f).build();

        ImageIOHandler.write(image, highBaos, "jpeg", highOpts);
        ImageIOHandler.write(image, lowBaos, "jpeg", lowOpts);

        assertTrue(highBaos.size() > lowBaos.size(),
                "High quality (" + highBaos.size() + ") should be larger than low quality (" + lowBaos.size() + ")");
    }

    // --- Helpers ---

    private File createTestImage(int width, int height, String format) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        File file = tempDir.resolve("input." + format).toFile();
        ImageIO.write(img, format, file);
        return file;
    }

    private BufferedImage createRichTestImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        // Draw some varied content so JPEG compression produces visible size differences
        for (int y = 0; y < height; y += 10) {
            for (int x = 0; x < width; x += 10) {
                g.setColor(new Color((x * 7 + y * 13) % 256, (x * 11 + y * 3) % 256, (x * 5 + y * 17) % 256));
                g.fillRect(x, y, 10, 10);
            }
        }
        g.dispose();
        return img;
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
