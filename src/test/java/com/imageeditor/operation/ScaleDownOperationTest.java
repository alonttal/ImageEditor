package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ScaleDownOperationTest {

    @Test
    void scalesDownLargeImage() {
        BufferedImage input = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        ScaleDownOperation op = new ScaleDownOperation(200, 200);

        BufferedImage result = op.apply(input);

        assertEquals(200, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void returnsSmallImageUnchanged() {
        BufferedImage input = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        ScaleDownOperation op = new ScaleDownOperation(200, 200);

        BufferedImage result = op.apply(input);

        assertSame(input, result);
    }

    @Test
    void returnsExactFitUnchanged() {
        BufferedImage input = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        ScaleDownOperation op = new ScaleDownOperation(200, 200);

        BufferedImage result = op.apply(input);

        assertSame(input, result);
    }

    @Test
    void scalesDownWhenOneDimensionExceeds() {
        BufferedImage input = new BufferedImage(400, 100, BufferedImage.TYPE_INT_RGB);
        ScaleDownOperation op = new ScaleDownOperation(200, 200);

        BufferedImage result = op.apply(input);

        assertEquals(200, result.getWidth());
        assertEquals(50, result.getHeight());
    }

    @Test
    void scalesDownTallImage() {
        BufferedImage input = new BufferedImage(100, 400, BufferedImage.TYPE_INT_RGB);
        ScaleDownOperation op = new ScaleDownOperation(200, 200);

        BufferedImage result = op.apply(input);

        assertEquals(50, result.getWidth());
        assertEquals(200, result.getHeight());
    }

    @Test
    void rejectsZeroWidth() {
        assertThrows(ImageEditorException.class, () -> new ScaleDownOperation(0, 100));
    }

    @Test
    void rejectsZeroHeight() {
        assertThrows(ImageEditorException.class, () -> new ScaleDownOperation(100, 0));
    }

    @Test
    void rejectsNegativeDimensions() {
        assertThrows(ImageEditorException.class, () -> new ScaleDownOperation(-1, -1));
    }
}
