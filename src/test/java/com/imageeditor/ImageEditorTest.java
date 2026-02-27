package com.imageeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

    private File createTestImage(int width, int height, String format) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        File file = tempDir.resolve("input." + format).toFile();
        ImageIO.write(img, format, file);
        return file;
    }
}
