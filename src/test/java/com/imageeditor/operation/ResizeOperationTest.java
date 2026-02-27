package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ResizeOperationTest {

    @Test
    void resizesToTargetDimensions() {
        BufferedImage input = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ResizeOperation op = new ResizeOperation(50, 50);

        BufferedImage result = op.apply(input);

        assertEquals(50, result.getWidth());
        assertEquals(50, result.getHeight());
    }

    @Test
    void scalesUp() {
        BufferedImage input = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        ResizeOperation op = new ResizeOperation(200, 150);

        BufferedImage result = op.apply(input);

        assertEquals(200, result.getWidth());
        assertEquals(150, result.getHeight());
    }

    @Test
    void preservesImageType() {
        BufferedImage input = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        ResizeOperation op = new ResizeOperation(50, 50);

        BufferedImage result = op.apply(input);

        assertEquals(BufferedImage.TYPE_INT_ARGB, result.getType());
    }

    @Test
    void rejectsZeroWidth() {
        assertThrows(ImageEditorException.class, () -> new ResizeOperation(0, 50));
    }

    @Test
    void rejectsNegativeHeight() {
        assertThrows(ImageEditorException.class, () -> new ResizeOperation(50, -1));
    }
}
