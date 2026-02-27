package com.imageeditor;

import com.imageeditor.io.ImageFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageEditorTest {

    @TempDir
    Path tempDir;

    @Test
    void resizeImage() throws IOException {
        Path input = createTestImage(100, 100, "png");
        Path output = tempDir.resolve("output.png");

        ImageEditor.builder()
                .resize(50, 50)
                .build()
                .process(input, output);

        BufferedImage result = ImageIO.read(output.toFile());
        assertEquals(50, result.getWidth());
        assertEquals(50, result.getHeight());
    }

    @Test
    void chainMultipleResizes() throws IOException {
        Path input = createTestImage(200, 200, "png");
        Path output = tempDir.resolve("output.png");

        ImageEditor.builder()
                .resize(100, 100)
                .resize(50, 25)
                .build()
                .process(input, output);

        BufferedImage result = ImageIO.read(output.toFile());
        assertEquals(50, result.getWidth());
        assertEquals(25, result.getHeight());
    }

    @Test
    void editorIsReusable() throws IOException {
        ImageEditor editor = ImageEditor.builder()
                .resize(30, 30)
                .build();

        Path input1 = createTestImage(100, 100, "png");
        Path output1 = tempDir.resolve("out1.png");
        editor.process(input1, output1);

        Path input2 = createTestImage(200, 150, "png");
        Path output2 = tempDir.resolve("out2.png");
        editor.process(input2, output2);

        assertEquals(30, ImageIO.read(output1.toFile()).getWidth());
        assertEquals(30, ImageIO.read(output2.toFile()).getWidth());
    }

    // --- Quality + metadata builder tests ---

    @Test
    void qualityBuilderProducesDifferentSizes() throws IOException {
        BufferedImage rich = createRichTestImage(200, 200);
        Path richInput = tempDir.resolve("rich.png");
        ImageIO.write(rich, "png", richInput.toFile());

        Path highOutput = tempDir.resolve("high.jpg");
        Path lowOutput = tempDir.resolve("low.jpg");

        ImageEditor.builder()
                .quality(0.95f)
                .outputFormat(ImageFormat.JPEG)
                .build()
                .process(richInput, highOutput);

        ImageEditor.builder()
                .quality(0.1f)
                .outputFormat(ImageFormat.JPEG)
                .build()
                .process(richInput, lowOutput);

        assertTrue(Files.size(highOutput) > Files.size(lowOutput),
                "High quality (" + Files.size(highOutput) + ") should be larger than low quality ("
                        + Files.size(lowOutput) + ")");
    }

    @Test
    void stripMetadataBuilder() throws IOException {
        Path input = createTestImage(50, 50, "png");
        Path output = tempDir.resolve("stripped.png");

        ImageEditor.builder()
                .stripMetadata()
                .build()
                .process(input, output);

        BufferedImage result = ImageIO.read(output.toFile());
        assertEquals(50, result.getWidth());
        assertEquals(50, result.getHeight());
    }

    @Test
    void qualityValidation() {
        assertThrows(IllegalArgumentException.class, () -> ImageEditor.builder().quality(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> ImageEditor.builder().quality(1.1f));
    }

    // --- Stream-based process tests ---

    @Test
    void processStream() throws IOException {
        Path input = createTestImage(100, 80, "png");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = Files.newInputStream(input)) {
            ImageEditor.builder()
                    .resize(50, 40)
                    .build()
                    .process(is, baos);
        }

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(50, result.getWidth());
        assertEquals(40, result.getHeight());
    }

    @Test
    void processStreamWithQuality() throws IOException {
        BufferedImage rich = createRichTestImage(200, 200);
        Path richInput = tempDir.resolve("rich_stream.png");
        ImageIO.write(rich, "png", richInput.toFile());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = Files.newInputStream(richInput)) {
            ImageEditor.builder()
                    .quality(0.5f)
                    .outputFormat(ImageFormat.JPEG)
                    .build()
                    .process(is, baos);
        }

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(200, result.getWidth());
        assertEquals(200, result.getHeight());
    }

    // --- Batch processing tests ---

    @Test
    void processDirectorySequential() throws IOException {
        Path inputDir = tempDir.resolve("batch_input");
        Path outputDir = tempDir.resolve("batch_output");
        Files.createDirectories(inputDir);

        // Create multiple test images
        createTestImageAt(inputDir.resolve("a.png"), 100, 80);
        createTestImageAt(inputDir.resolve("b.png"), 120, 90);
        createTestImageAt(inputDir.resolve("c.png"), 80, 60);
        // Non-image file should be skipped
        Files.writeString(inputDir.resolve("readme.txt"), "not an image");

        ImageEditor.builder()
                .resize(50, 50)
                .build()
                .processDirectory(inputDir, outputDir);

        assertTrue(Files.exists(outputDir));
        long count = Files.list(outputDir).count();
        assertEquals(3, count);

        try (var stream = Files.list(outputDir)) {
            stream.forEach(out -> {
                try {
                    BufferedImage img = ImageIO.read(out.toFile());
                    assertEquals(50, img.getWidth());
                    assertEquals(50, img.getHeight());
                } catch (IOException e) {
                    fail("Failed to read output image: " + out);
                }
            });
        }
    }

    @Test
    void processDirectoryParallel() throws IOException {
        Path inputDir = tempDir.resolve("batch_par_input");
        Path outputDir = tempDir.resolve("batch_par_output");
        Files.createDirectories(inputDir);

        createTestImageAt(inputDir.resolve("x.png"), 100, 80);
        createTestImageAt(inputDir.resolve("y.png"), 120, 90);

        ImageEditor.builder()
                .resize(40, 40)
                .build()
                .processDirectory(inputDir, outputDir, 2);

        long count = Files.list(outputDir).count();
        assertEquals(2, count);

        try (var stream = Files.list(outputDir)) {
            stream.forEach(out -> {
                try {
                    BufferedImage img = ImageIO.read(out.toFile());
                    assertEquals(40, img.getWidth());
                    assertEquals(40, img.getHeight());
                } catch (IOException e) {
                    fail("Failed to read output image: " + out);
                }
            });
        }
    }

    // --- process() edge cases ---

    @Test
    void processNoOperations() throws IOException {
        Path input = createTestImage(80, 60, "png");
        Path output = tempDir.resolve("copy.png");

        ImageEditor.builder().build().process(input, output);

        BufferedImage result = ImageIO.read(output.toFile());
        assertEquals(80, result.getWidth());
        assertEquals(60, result.getHeight());
    }

    @Test
    void processConvertsPngToJpg() throws IOException {
        Path input = createTestImage(50, 50, "png");
        Path output = tempDir.resolve("output.jpg");

        ImageEditor.builder()
                .outputFormat(ImageFormat.JPEG)
                .build()
                .process(input, output);

        BufferedImage result = ImageIO.read(output.toFile());
        assertEquals(50, result.getWidth());
    }

    @Test
    void processConvertsJpgToPng() throws IOException {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        Path jpgInput = tempDir.resolve("convert.jpg");
        ImageIO.write(img, "jpeg", jpgInput.toFile());
        Path output = tempDir.resolve("converted.png");

        ImageEditor.builder()
                .outputFormat(ImageFormat.PNG)
                .build()
                .process(jpgInput, output);

        BufferedImage result = ImageIO.read(output.toFile());
        assertEquals(50, result.getWidth());
    }

    @Test
    void processNonExistentInput() {
        Path noFile = tempDir.resolve("missing.png");
        Path output = tempDir.resolve("output.png");

        assertThrows(com.imageeditor.exception.ImageEditorException.class, () ->
                ImageEditor.builder().build().process(noFile, output));
    }

    // --- processDirectory() edge cases ---

    @Test
    void processDirectoryNotADirectory() throws IOException {
        Path file = createTestImage(10, 10, "png");
        Path outputDir = tempDir.resolve("out");

        assertThrows(com.imageeditor.exception.ImageEditorException.class, () ->
                ImageEditor.builder().build().processDirectory(file, outputDir));
    }

    @Test
    void processDirectoryEmpty() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Path outputDir = tempDir.resolve("empty_output");
        Files.createDirectories(emptyDir);

        ImageEditor.builder().resize(50, 50).build().processDirectory(emptyDir, outputDir);

        assertTrue(Files.isDirectory(outputDir));
        try (var stream = Files.list(outputDir)) {
            assertEquals(0, stream.count());
        }
    }

    @Test
    void processDirectorySkipsSubdirectories() throws IOException {
        Path inputDir = tempDir.resolve("with_subdir");
        Files.createDirectories(inputDir);
        Files.createDirectories(inputDir.resolve("subdir"));
        createTestImageAt(inputDir.resolve("image.png"), 100, 100);

        Path outputDir = tempDir.resolve("output_skip");
        ImageEditor.builder().resize(50, 50).build().processDirectory(inputDir, outputDir);

        try (var stream = Files.list(outputDir)) {
            assertEquals(1, stream.count());
        }
    }

    @Test
    void processDirectoryWithMixedFormats() throws IOException {
        Path inputDir = tempDir.resolve("mixed");
        Files.createDirectories(inputDir);

        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", inputDir.resolve("a.png").toFile());
        ImageIO.write(img, "jpeg", inputDir.resolve("b.jpg").toFile());
        ImageIO.write(img, "gif", inputDir.resolve("c.gif").toFile());

        Path outputDir = tempDir.resolve("mixed_out");
        ImageEditor.builder().resize(50, 50).build().processDirectory(inputDir, outputDir);

        try (var stream = Files.list(outputDir)) {
            assertEquals(3, stream.count());
        }
    }

    @Test
    void processDirectoryZeroParallelism() throws IOException {
        Path inputDir = tempDir.resolve("zero_par");
        Files.createDirectories(inputDir);
        createTestImageAt(inputDir.resolve("a.png"), 100, 100);

        Path outputDir = tempDir.resolve("zero_par_out");
        ImageEditor.builder().resize(50, 50).build().processDirectory(inputDir, outputDir, 0);

        try (var stream = Files.list(outputDir)) {
            assertEquals(1, stream.count());
        }
    }

    @Test
    void processDirectoryNegativeParallelism() throws IOException {
        Path inputDir = tempDir.resolve("neg_par");
        Files.createDirectories(inputDir);
        createTestImageAt(inputDir.resolve("a.png"), 100, 100);

        Path outputDir = tempDir.resolve("neg_par_out");
        ImageEditor.builder().resize(50, 50).build().processDirectory(inputDir, outputDir, -1);

        try (var stream = Files.list(outputDir)) {
            assertEquals(1, stream.count());
        }
    }

    // --- Builder edge cases ---

    @Test
    void builderQualityNaN() {
        assertThrows(IllegalArgumentException.class, () -> ImageEditor.builder().quality(Float.NaN));
    }

    @Test
    void builderQualityInfinity() {
        assertThrows(IllegalArgumentException.class, () -> ImageEditor.builder().quality(Float.POSITIVE_INFINITY));
    }

    // --- Output format tests ---

    @Test
    void processWithOutputFormat() throws IOException {
        Path input = createTestImage(50, 50, "png");
        Path output = tempDir.resolve("output.png"); // extension says png

        ImageEditor.builder()
                .outputFormat(ImageFormat.JPEG)
                .build()
                .process(input, output);

        // Verify the file is actually JPEG by checking magic bytes (FF D8 FF)
        byte[] bytes = Files.readAllBytes(output);
        assertEquals((byte) 0xFF, bytes[0]);
        assertEquals((byte) 0xD8, bytes[1]);
        assertEquals((byte) 0xFF, bytes[2]);
    }

    @Test
    void processDirectoryWithOutputFormat() throws IOException {
        Path inputDir = tempDir.resolve("fmt_input");
        Path outputDir = tempDir.resolve("fmt_output");
        Files.createDirectories(inputDir);

        createTestImageAt(inputDir.resolve("a.png"), 80, 60);
        createTestImageAt(inputDir.resolve("b.png"), 100, 80);

        ImageEditor.builder()
                .outputFormat(ImageFormat.JPEG)
                .build()
                .processDirectory(inputDir, outputDir);

        // All output files should have .jpeg extension (canonical)
        try (var stream = Files.list(outputDir)) {
            List<String> names = stream.map(p -> p.getFileName().toString()).sorted().toList();
            assertEquals(List.of("a.jpeg", "b.jpeg"), names);
        }

        // Verify they are actual JPEGs
        byte[] bytes = Files.readAllBytes(outputDir.resolve("a.jpeg"));
        assertEquals((byte) 0xFF, bytes[0]);
        assertEquals((byte) 0xD8, bytes[1]);
    }

    @Test
    void outputFormatNullPreservesExtension() throws IOException {
        Path input = createTestImage(50, 50, "png");
        Path output = tempDir.resolve("stays.png");

        ImageEditor.builder()
                .build()
                .process(input, output);

        // Verify the file is actually PNG by checking magic bytes (89 50 4E 47)
        byte[] bytes = Files.readAllBytes(output);
        assertEquals((byte) 0x89, bytes[0]);
        assertEquals((byte) 0x50, bytes[1]);
        assertEquals((byte) 0x4E, bytes[2]);
        assertEquals((byte) 0x47, bytes[3]);
    }

    // --- Helpers ---

    private Path createTestImage(int width, int height, String format) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Path file = tempDir.resolve("input." + format);
        ImageIO.write(img, format, file.toFile());
        return file;
    }

    private void createTestImageAt(Path path, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", path.toFile());
    }

    private BufferedImage createRichTestImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        for (int y = 0; y < height; y += 10) {
            for (int x = 0; x < width; x += 10) {
                g.setColor(new Color((x * 7 + y * 13) % 256, (x * 11 + y * 3) % 256, (x * 5 + y * 17) % 256));
                g.fillRect(x, y, 10, 10);
            }
        }
        g.dispose();
        return img;
    }
}
