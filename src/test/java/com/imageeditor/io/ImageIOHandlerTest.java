package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ImageIOHandlerTest {

    @TempDir
    Path tempDir;

    @Test
    void readAndWritePng() throws IOException {
        Path file = createTestImage(80, 60, "png");

        BufferedImage image = ImageIOHandler.read(file);
        assertEquals(80, image.getWidth());
        assertEquals(60, image.getHeight());

        Path output = tempDir.resolve("out.png");
        ImageIOHandler.write(image, output);

        BufferedImage result = ImageIO.read(output.toFile());
        assertEquals(80, result.getWidth());
        assertEquals(60, result.getHeight());
    }

    @Test
    void readAndWriteJpg() throws IOException {
        Path file = createTestImage(80, 60, "jpeg");
        Path jpgFile = tempDir.resolve("input.jpg");
        Files.move(file, jpgFile);

        BufferedImage image = ImageIOHandler.read(jpgFile);
        assertEquals(80, image.getWidth());

        Path output = tempDir.resolve("out.jpg");
        ImageIOHandler.write(image, output);

        BufferedImage result = ImageIO.read(output.toFile());
        assertEquals(80, result.getWidth());
    }

    @Test
    void readAndWriteWebp() throws IOException, InterruptedException {
        assumeTrue(isToolAvailable("cwebp"), "cwebp not installed, skipping");
        assumeTrue(isToolAvailable("dwebp"), "dwebp not installed, skipping");

        // Create a PNG, then convert to WebP via cwebp
        Path png = createTestImage(64, 48, "png");
        Path webp = tempDir.resolve("test.webp");
        new ProcessBuilder("cwebp", png.toAbsolutePath().toString(), "-o", webp.toAbsolutePath().toString())
                .redirectErrorStream(true).start().waitFor();

        BufferedImage image = ImageIOHandler.read(webp);
        assertEquals(64, image.getWidth());
        assertEquals(48, image.getHeight());

        Path outputWebp = tempDir.resolve("out.webp");
        ImageIOHandler.write(image, outputWebp);
        assertTrue(Files.exists(outputWebp));
        assertTrue(Files.size(outputWebp) > 0);

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
        Path png = createTestImage(64, 48, "png");
        Path avif = tempDir.resolve("test.avif");
        new ProcessBuilder("heif-enc", png.toAbsolutePath().toString(), "-o", avif.toAbsolutePath().toString())
                .redirectErrorStream(true).start().waitFor();

        BufferedImage image = ImageIOHandler.read(avif);
        assertEquals(64, image.getWidth());
        assertEquals(48, image.getHeight());

        Path outputAvif = tempDir.resolve("out.avif");
        ImageIOHandler.write(image, outputAvif);
        assertTrue(Files.exists(outputAvif));
        assertTrue(Files.size(outputAvif) > 0);

        // Verify round-trip by reading back
        BufferedImage roundTrip = ImageIOHandler.read(outputAvif);
        assertEquals(64, roundTrip.getWidth());
        assertEquals(48, roundTrip.getHeight());
    }

    @Test
    void unsupportedFormatThrows() {
        Path file = tempDir.resolve("image.xyz");
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.read(file));
    }

    @Test
    void noExtensionThrows() {
        Path file = tempDir.resolve("noext");
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.read(file));
    }

    @Test
    void standardFormatsAlwaysSupported() {
        assertTrue(ImageIOHandler.isFormatSupported(ImageFormat.PNG));
        assertTrue(ImageIOHandler.isFormatSupported(ImageFormat.JPEG));
        assertTrue(ImageIOHandler.isFormatSupported(ImageFormat.GIF));
        assertTrue(ImageIOHandler.isFormatSupported(ImageFormat.BMP));
    }

    @Test
    void webpSupportedWhenToolsInstalled() {
        assumeTrue(isToolAvailable("cwebp"), "cwebp not installed, skipping");
        assumeTrue(isToolAvailable("dwebp"), "dwebp not installed, skipping");
        assertTrue(ImageIOHandler.isFormatSupported(ImageFormat.WEBP));
    }

    @Test
    void avifSupportedWhenToolsInstalled() {
        assumeTrue(isToolAvailable("heif-enc"), "heif-enc not installed, skipping");
        assumeTrue(isToolAvailable("heif-dec"), "heif-dec not installed, skipping");
        assertTrue(ImageIOHandler.isFormatSupported(ImageFormat.AVIF));
    }

    // --- Quality tests ---

    @Test
    void writeJpegWithDifferentQualityProducesDifferentSizes() throws IOException {
        BufferedImage image = createRichTestImage(200, 200);

        Path highQuality = tempDir.resolve("high.jpg");
        Path lowQuality = tempDir.resolve("low.jpg");

        OutputOptions highOpts = OutputOptions.builder().quality(0.95f).build();
        OutputOptions lowOpts = OutputOptions.builder().quality(0.1f).build();

        ImageIOHandler.write(image, highQuality, highOpts);
        ImageIOHandler.write(image, lowQuality, lowOpts);

        assertTrue(Files.size(highQuality) > Files.size(lowQuality),
                "High quality file (" + Files.size(highQuality) + ") should be larger than low quality ("
                        + Files.size(lowQuality) + ")");
    }

    // --- Metadata stripping tests ---

    @Test
    void writeWithStripMetadata() throws IOException {
        BufferedImage image = createRichTestImage(100, 100);
        Path output = tempDir.resolve("stripped.png");

        OutputOptions opts = OutputOptions.builder().stripMetadata(true).build();
        ImageIOHandler.write(image, output, opts);

        BufferedImage result = ImageIO.read(output.toFile());
        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    // --- Format detection tests ---

    @Test
    void detectFormatPng() throws IOException {
        Path file = createTestImage(10, 10, "png");
        assertEquals(ImageFormat.PNG, ImageIOHandler.detectFormat(file));
    }

    @Test
    void detectFormatJpeg() throws IOException {
        Path file = createTestImage(10, 10, "jpeg");
        Path jpgFile = tempDir.resolve("detect.jpg");
        Files.move(file, jpgFile);
        assertEquals(ImageFormat.JPEG, ImageIOHandler.detectFormat(jpgFile));
    }

    @Test
    void detectFormatGif() throws IOException {
        Path file = tempDir.resolve("detect.gif");
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "gif", file.toFile());
        assertEquals(ImageFormat.GIF, ImageIOHandler.detectFormat(file));
    }

    @Test
    void detectFormatBmp() throws IOException {
        Path file = tempDir.resolve("detect.bmp");
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "bmp", file.toFile());
        assertEquals(ImageFormat.BMP, ImageIOHandler.detectFormat(file));
    }

    @Test
    void detectFormatWebp() throws IOException, InterruptedException {
        assumeTrue(isToolAvailable("cwebp"), "cwebp not installed, skipping");
        Path png = createTestImage(10, 10, "png");
        Path webp = tempDir.resolve("detect.webp");
        new ProcessBuilder("cwebp", png.toAbsolutePath().toString(), "-o", webp.toAbsolutePath().toString())
                .redirectErrorStream(true).start().waitFor();
        assertEquals(ImageFormat.WEBP, ImageIOHandler.detectFormat(webp));
    }

    @Test
    void detectFormatFromInputStream() throws IOException {
        Path file = createTestImage(10, 10, "png");
        try (InputStream is = new BufferedInputStream(Files.newInputStream(file))) {
            assertEquals(ImageFormat.PNG, ImageIOHandler.detectFormat(is));
            // Stream should still be readable after detection (mark/reset)
            BufferedImage img = ImageIO.read(is);
            assertNotNull(img);
        }
    }

    @Test
    void detectFormatFallsBackToExtension() throws IOException {
        // Create a file with unknown content but valid extension
        Path file = tempDir.resolve("fallback.png");
        Files.write(file, new byte[]{0x00, 0x00, 0x00}); // garbage bytes
        assertEquals(ImageFormat.PNG, ImageIOHandler.detectFormat(file));
    }

    // --- Stream-based I/O tests ---

    @Test
    void streamReadWriteRoundTrip() throws IOException {
        Path file = createTestImage(60, 40, "png");

        BufferedImage image;
        try (InputStream is = Files.newInputStream(file)) {
            image = ImageIOHandler.read(is, ImageFormat.PNG);
        }
        assertEquals(60, image.getWidth());
        assertEquals(40, image.getHeight());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIOHandler.write(image, baos, ImageFormat.PNG, OutputOptions.defaults());

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

        ImageIOHandler.write(image, highBaos, ImageFormat.JPEG, highOpts);
        ImageIOHandler.write(image, lowBaos, ImageFormat.JPEG, lowOpts);

        assertTrue(highBaos.size() > lowBaos.size(),
                "High quality (" + highBaos.size() + ") should be larger than low quality (" + lowBaos.size() + ")");
    }

    // --- Read edge cases ---

    @Test
    void readNonExistentFile() {
        Path noFile = tempDir.resolve("nonexistent.png");
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.read(noFile));
    }

    @Test
    void readEmptyFile() throws IOException {
        Path empty = tempDir.resolve("empty.png");
        Files.createFile(empty);
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.read(empty));
    }

    @Test
    void readCorruptedFile() throws IOException {
        Path corrupted = tempDir.resolve("corrupted.png");
        Files.write(corrupted, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05});
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.read(corrupted));
    }

    @Test
    void readMixedCaseExtension() throws IOException {
        Path png = createTestImage(20, 20, "png");
        Path mixed = tempDir.resolve("image.PNG");
        Files.move(png, mixed);
        BufferedImage image = ImageIOHandler.read(mixed);
        assertEquals(20, image.getWidth());
    }

    // --- Write edge cases ---

    @Test
    void writeToNonExistentParent() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Path badPath = tempDir.resolve("nonexistent").resolve("output.png");
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.write(img, badPath));
    }

    @Test
    void writeOverwritesExisting() throws IOException {
        Path file = createTestImage(20, 20, "png");
        BufferedImage bigger = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImageIOHandler.write(bigger, file);

        BufferedImage result = ImageIO.read(file.toFile());
        assertEquals(100, result.getWidth());
    }

    @Test
    void writeJpegQualityBoundaryValues() throws IOException {
        BufferedImage image = createRichTestImage(100, 100);

        Path q0 = tempDir.resolve("q0.jpg");
        Path q1 = tempDir.resolve("q1.jpg");

        ImageIOHandler.write(image, q0, OutputOptions.builder().quality(0.0f).build());
        ImageIOHandler.write(image, q1, OutputOptions.builder().quality(1.0f).build());

        assertTrue(Files.size(q0) > 0);
        assertTrue(Files.size(q1) > 0);
    }

    // --- getExtension edge cases ---

    @Test
    void getExtensionEmptyFilename() {
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.getExtension(""));
    }

    @Test
    void getExtensionDotOnly() {
        assertEquals("", ImageIOHandler.getExtension("."));
    }

    @Test
    void getExtensionMultipleDots() {
        assertEquals("png", ImageIOHandler.getExtension("image.backup.png"));
    }

    @Test
    void getExtensionHiddenFile() {
        assertEquals("gitignore", ImageIOHandler.getExtension(".gitignore"));
    }

    // --- detectFormat edge cases ---

    @Test
    void detectFormatNonExistentFile() {
        Path noFile = tempDir.resolve("missing.png");
        assertEquals(ImageFormat.PNG, ImageIOHandler.detectFormat(noFile));
    }

    @Test
    void detectFormatNoExtensionNoMagic() throws IOException {
        Path noExt = tempDir.resolve("noext");
        Files.write(noExt, new byte[]{0x01, 0x02, 0x03, 0x04});
        assertNull(ImageIOHandler.detectFormat(noExt));
    }

    @Test
    void detectFormatTiff() throws IOException {
        Path tiff = tempDir.resolve("test.tiff");
        // TIFF little-endian magic: 49 49 2A 00
        Files.write(tiff, new byte[]{0x49, 0x49, 0x2A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        assertEquals(ImageFormat.TIFF, ImageIOHandler.detectFormat(tiff));
    }

    @Test
    void detectFormatStreamTooShort() {
        InputStream is = new ByteArrayInputStream(new byte[]{0x01, 0x02});
        assertNull(ImageIOHandler.detectFormat(is));
    }

    // --- Stream I/O edge cases ---

    @Test
    void streamReadWebpWithoutTools() throws IOException {
        Path fakeDir = tempDir.resolve("no-tools-read");
        Files.createDirectory(fakeDir);
        try {
            ImageIOHandler.setToolDirectory(fakeDir);
            InputStream is = new ByteArrayInputStream(new byte[]{0x01});
            assertThrows(ImageEditorException.class, () -> ImageIOHandler.read(is, ImageFormat.WEBP));
        } finally {
            ImageIOHandler.setToolDirectory(null);
        }
    }

    @Test
    void streamWriteWebpWithoutTools() throws IOException {
        Path fakeDir = tempDir.resolve("no-tools-write");
        Files.createDirectory(fakeDir);
        try {
            ImageIOHandler.setToolDirectory(fakeDir);
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            assertThrows(ImageEditorException.class, () ->
                    ImageIOHandler.write(img, baos, ImageFormat.WEBP, OutputOptions.defaults()));
        } finally {
            ImageIOHandler.setToolDirectory(null);
        }
    }

    // --- Tool directory configuration tests ---

    @Test
    void setToolDirectoryUsesCustomPath() throws IOException {
        Path fakeDir = tempDir.resolve("tools");
        Files.createDirectory(fakeDir);
        try {
            ImageIOHandler.setToolDirectory(fakeDir);
            // With a fake directory containing no executables, CLI formats should not be supported
            assertFalse(ImageIOHandler.isFormatSupported(ImageFormat.WEBP));
            assertFalse(ImageIOHandler.isFormatSupported(ImageFormat.AVIF));
        } finally {
            ImageIOHandler.setToolDirectory(null);
        }
    }

    @Test
    void setToolDirectoryNullResetsToDefault() {
        ImageIOHandler.setToolDirectory(Path.of("/some/fake/path"));
        ImageIOHandler.setToolDirectory(null);
        assertNull(ImageIOHandler.getToolDirectory());
        // Standard formats should still work regardless
        assertTrue(ImageIOHandler.isFormatSupported(ImageFormat.PNG));
    }

    @Test
    void getToolDirectoryReturnsConfiguredPath() {
        Path dir = Path.of("/opt/image-tools");
        try {
            ImageIOHandler.setToolDirectory(dir);
            assertEquals(dir, ImageIOHandler.getToolDirectory());
        } finally {
            ImageIOHandler.setToolDirectory(null);
        }
    }

    // --- getFormat() direct tests ---

    @Test
    void getFormatPng() {
        assertEquals(ImageFormat.PNG, ImageIOHandler.getFormat("photo.png"));
    }

    @Test
    void getFormatJpgUpperCase() {
        assertEquals(ImageFormat.JPEG, ImageIOHandler.getFormat("IMG.JPG"));
    }

    @Test
    void getFormatTiffMultipleDots() {
        assertEquals(ImageFormat.TIFF, ImageIOHandler.getFormat("a.b.tiff"));
    }

    @Test
    void getFormatNoExtensionThrows() {
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.getFormat("noext"));
    }

    @Test
    void getFormatUnsupportedExtensionThrows() {
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.getFormat("file.xyz"));
    }

    // --- Additional detectFormat(InputStream) tests ---

    @Test
    void detectFormatStreamTiffBigEndian() {
        // TIFF big-endian magic: 4D 4D 00 2A
        byte[] tiffBE = new byte[]{0x4D, 0x4D, 0x00, 0x2A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        InputStream is = new BufferedInputStream(new ByteArrayInputStream(tiffBE));
        assertEquals(ImageFormat.TIFF, ImageIOHandler.detectFormat(is));
    }

    @Test
    void detectFormatStreamAvifMagicBytes() {
        // AVIF: ftyp at offset 4 (bytes: 00 00 00 xx 66 74 79 70)
        byte[] avif = new byte[]{0x00, 0x00, 0x00, 0x1C, 0x66, 0x74, 0x79, 0x70, 0x61, 0x76, 0x69, 0x66};
        InputStream is = new BufferedInputStream(new ByteArrayInputStream(avif));
        assertEquals(ImageFormat.AVIF, ImageIOHandler.detectFormat(is));
    }

    @Test
    void detectFormatStreamJpeg() throws IOException {
        Path file = createTestImage(10, 10, "jpeg");
        Path jpgFile = tempDir.resolve("detect_stream.jpg");
        Files.move(file, jpgFile);
        try (InputStream is = new BufferedInputStream(Files.newInputStream(jpgFile))) {
            assertEquals(ImageFormat.JPEG, ImageIOHandler.detectFormat(is));
            // Stream should still be readable after detection
            BufferedImage img = ImageIO.read(is);
            assertNotNull(img);
        }
    }

    @Test
    void detectFormatStreamGif() throws IOException {
        Path file = tempDir.resolve("detect_stream.gif");
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "gif", file.toFile());
        try (InputStream is = new BufferedInputStream(Files.newInputStream(file))) {
            assertEquals(ImageFormat.GIF, ImageIOHandler.detectFormat(is));
            BufferedImage result = ImageIO.read(is);
            assertNotNull(result);
        }
    }

    // --- Stream round-trip for GIF and BMP ---

    @Test
    void streamReadWriteRoundTripGif() throws IOException {
        Path file = tempDir.resolve("roundtrip.gif");
        BufferedImage img = new BufferedImage(30, 20, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "gif", file.toFile());

        BufferedImage image;
        try (InputStream is = Files.newInputStream(file)) {
            image = ImageIOHandler.read(is, ImageFormat.GIF);
        }
        assertEquals(30, image.getWidth());
        assertEquals(20, image.getHeight());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIOHandler.write(image, baos, ImageFormat.GIF, OutputOptions.defaults());

        byte[] bytes = baos.toByteArray();
        // GIF magic: 47 49 46 38
        assertEquals((byte) 0x47, bytes[0]);
        assertEquals((byte) 0x49, bytes[1]);
        assertEquals((byte) 0x46, bytes[2]);
        assertEquals((byte) 0x38, bytes[3]);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(bytes));
        assertEquals(30, result.getWidth());
        assertEquals(20, result.getHeight());
    }

    @Test
    void streamReadWriteRoundTripBmp() throws IOException {
        Path file = tempDir.resolve("roundtrip.bmp");
        BufferedImage img = new BufferedImage(30, 20, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "bmp", file.toFile());

        BufferedImage image;
        try (InputStream is = Files.newInputStream(file)) {
            image = ImageIOHandler.read(is, ImageFormat.BMP);
        }
        assertEquals(30, image.getWidth());
        assertEquals(20, image.getHeight());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIOHandler.write(image, baos, ImageFormat.BMP, OutputOptions.defaults());

        byte[] bytes = baos.toByteArray();
        // BMP magic: 42 4D
        assertEquals((byte) 0x42, bytes[0]);
        assertEquals((byte) 0x4D, bytes[1]);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(bytes));
        assertEquals(30, result.getWidth());
        assertEquals(20, result.getHeight());
    }

    // --- Stream read with empty/null image ---

    @Test
    void streamReadEmptyStreamThrows() {
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        assertThrows(ImageEditorException.class, () -> ImageIOHandler.read(emptyStream, ImageFormat.PNG));
    }

    // --- Alpha channel stripping tests ---

    @Test
    void writeArgbImageToJpegStripsAlpha() throws IOException {
        BufferedImage argb = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.setColor(new Color(255, 0, 0, 128));
        g.fillRect(0, 0, 50, 50);
        g.dispose();

        Path output = tempDir.resolve("alpha.jpg");
        ImageIOHandler.write(argb, output);

        BufferedImage result = ImageIO.read(output.toFile());
        assertNotNull(result);
        assertEquals(50, result.getWidth());
        assertFalse(result.getColorModel().hasAlpha());
    }

    @Test
    void writeArgbImageToPngPreservesAlpha() throws IOException {
        BufferedImage argb = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.setColor(new Color(255, 0, 0, 128));
        g.fillRect(0, 0, 50, 50);
        g.dispose();

        Path output = tempDir.resolve("alpha.png");
        ImageIOHandler.write(argb, output);

        BufferedImage result = ImageIO.read(output.toFile());
        assertNotNull(result);
        assertTrue(result.getColorModel().hasAlpha());
    }

    @Test
    void writeArgbImageToJpegStreamStripsAlpha() throws IOException {
        BufferedImage argb = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.setColor(new Color(255, 0, 0, 128));
        g.fillRect(0, 0, 50, 50);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIOHandler.write(argb, baos, ImageFormat.JPEG, OutputOptions.defaults());

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
        assertNotNull(result);
        assertFalse(result.getColorModel().hasAlpha());
    }

    // --- Helpers ---

    private Path createTestImage(int width, int height, String format) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Path file = tempDir.resolve("input." + format);
        ImageIO.write(img, format, file.toFile());
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
