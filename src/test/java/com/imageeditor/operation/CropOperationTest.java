package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class CropOperationTest {

    @Test
    void cropsToSpecifiedRegion() {
        BufferedImage input = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        CropOperation op = new CropOperation(10, 10, 50, 50);

        BufferedImage result = op.apply(input);

        assertEquals(50, result.getWidth());
        assertEquals(50, result.getHeight());
    }

    @Test
    void cropFromOrigin() {
        BufferedImage input = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        CropOperation op = new CropOperation(0, 0, 100, 150);

        BufferedImage result = op.apply(input);

        assertEquals(100, result.getWidth());
        assertEquals(150, result.getHeight());
    }

    @Test
    void rejectsOutOfBoundsCrop() {
        BufferedImage input = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        CropOperation op = new CropOperation(50, 50, 60, 60);

        assertThrows(ImageEditorException.class, () -> op.apply(input));
    }

    @Test
    void rejectsNegativeOrigin() {
        assertThrows(ImageEditorException.class, () -> new CropOperation(-1, 0, 50, 50));
    }

    @Test
    void rejectsZeroDimensions() {
        assertThrows(ImageEditorException.class, () -> new CropOperation(0, 0, 0, 50));
    }
}
