package com.imageeditor;

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

class ImageEditorTest {

    @TempDir
    Path tempDir;

    @Test
    void resizeImage() throws IOException {
        File input = createTestImage(100, 100, "png");
        File output = tempDir.resolve("output.png").toFile();

        ImageEditor.builder()
                .resize(50, 50)
                .build()
                .process(input, output);

        BufferedImage result = ImageIO.read(output);
        assertEquals(50, result.getWidth());
        assertEquals(50, result.getHeight());
    }

    @Test
    void chainMultipleResizes() throws IOException {
        File input = createTestImage(200, 200, "png");
        File output = tempDir.resolve("output.png").toFile();

        ImageEditor.builder()
                .resize(100, 100)
                .resize(50, 25)
                .build()
                .process(input, output);

        BufferedImage result = ImageIO.read(output);
        assertEquals(50, result.getWidth());
        assertEquals(25, result.getHeight());
    }

    @Test
    void editorIsReusable() throws IOException {
        ImageEditor editor = ImageEditor.builder()
                .resize(30, 30)
                .build();

        File input1 = createTestImage(100, 100, "png");
        File output1 = tempDir.resolve("out1.png").toFile();
        editor.process(input1, output1);

        File input2 = createTestImage(200, 150, "png");
        File output2 = tempDir.resolve("out2.png").toFile();
        editor.process(input2, output2);

        assertEquals(30, ImageIO.read(output1).getWidth());
        assertEquals(30, ImageIO.read(output2).getWidth());
    }

    // --- Quality + metadata builder tests ---

    @Test
    void qualityBuilderProducesDifferentSizes() throws IOException {
        BufferedImage rich = createRichTestImage(200, 200);
        File richInput = tempDir.resolve("rich.png").toFile();
        ImageIO.write(rich, "png", richInput);

        File highOutput = tempDir.resolve("high.jpg").toFile();
        File lowOutput = tempDir.resolve("low.jpg").toFile();

        ImageEditor.builder()
                .quality(0.95f)
                .build()
                .process(richInput, highOutput);

        ImageEditor.builder()
                .quality(0.1f)
                .build()
                .process(richInput, lowOutput);

        assertTrue(highOutput.length() > lowOutput.length(),
                "High quality (" + highOutput.length() + ") should be larger than low quality ("
                        + lowOutput.length() + ")");
    }

    @Test
    void stripMetadataBuilder() throws IOException {
        File input = createTestImage(50, 50, "png");
        File output = tempDir.resolve("stripped.png").toFile();

        ImageEditor.builder()
                .stripMetadata()
                .build()
                .process(input, output);

        BufferedImage result = ImageIO.read(output);
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
        File input = createTestImage(100, 80, "png");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = new FileInputStream(input)) {
            ImageEditor.builder()
                    .resize(50, 40)
                    .build()
                    .process(is, baos, "png");
        }

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(50, result.getWidth());
        assertEquals(40, result.getHeight());
    }

    @Test
    void processStreamWithQuality() throws IOException {
        BufferedImage rich = createRichTestImage(200, 200);
        File richInput = tempDir.resolve("rich_stream.png").toFile();
        ImageIO.write(rich, "png", richInput);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = new FileInputStream(richInput)) {
            ImageEditor.builder()
                    .quality(0.5f)
                    .build()
                    .process(is, baos, "jpeg");
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
                .processDirectory(inputDir.toFile(), outputDir.toFile());

        assertTrue(outputDir.toFile().exists());
        File[] outputs = outputDir.toFile().listFiles();
        assertNotNull(outputs);
        assertEquals(3, outputs.length);

        for (File out : outputs) {
            BufferedImage img = ImageIO.read(out);
            assertEquals(50, img.getWidth());
            assertEquals(50, img.getHeight());
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
                .processDirectory(inputDir.toFile(), outputDir.toFile(), 2);

        File[] outputs = outputDir.toFile().listFiles();
        assertNotNull(outputs);
        assertEquals(2, outputs.length);

        for (File out : outputs) {
            BufferedImage img = ImageIO.read(out);
            assertEquals(40, img.getWidth());
            assertEquals(40, img.getHeight());
        }
    }

    // --- Helpers ---

    private File createTestImage(int width, int height, String format) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        File file = tempDir.resolve("input." + format).toFile();
        ImageIO.write(img, format, file);
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
