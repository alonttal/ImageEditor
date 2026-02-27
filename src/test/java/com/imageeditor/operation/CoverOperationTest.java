package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class CoverOperationTest {

    @Test
    void coversWiderTarget() {
        BufferedImage input = new BufferedImage(100, 200, BufferedImage.TYPE_INT_RGB);
        CoverOperation op = new CoverOperation(200, 200);

        BufferedImage result = op.apply(input);

        assertEquals(200, result.getWidth());
        assertEquals(200, result.getHeight());
    }

    @Test
    void coversTallerTarget() {
        BufferedImage input = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        CoverOperation op = new CoverOperation(100, 100);

        BufferedImage result = op.apply(input);

        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void coversSquareToSquare() {
        BufferedImage input = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        CoverOperation op = new CoverOperation(150, 150);

        BufferedImage result = op.apply(input);

        assertEquals(150, result.getWidth());
        assertEquals(150, result.getHeight());
    }

    @Test
    void coversLandscapeToPortrait() {
        BufferedImage input = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        CoverOperation op = new CoverOperation(100, 200);

        BufferedImage result = op.apply(input);

        assertEquals(100, result.getWidth());
        assertEquals(200, result.getHeight());
    }

    @Test
    void rejectsZeroDimensions() {
        assertThrows(ImageEditorException.class, () -> new CoverOperation(0, 100));
    }
}
