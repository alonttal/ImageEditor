package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class FitOperationTest {

    @Test
    void fitsWideImageIntoSquare() {
        BufferedImage input = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        FitOperation op = new FitOperation(200, 200);

        BufferedImage result = op.apply(input);

        assertEquals(200, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void fitsTallImageIntoSquare() {
        BufferedImage input = new BufferedImage(200, 400, BufferedImage.TYPE_INT_RGB);
        FitOperation op = new FitOperation(200, 200);

        BufferedImage result = op.apply(input);

        assertEquals(100, result.getWidth());
        assertEquals(200, result.getHeight());
    }

    @Test
    void fitsSquareIntoSquare() {
        BufferedImage input = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        FitOperation op = new FitOperation(150, 150);

        BufferedImage result = op.apply(input);

        assertEquals(150, result.getWidth());
        assertEquals(150, result.getHeight());
    }

    @Test
    void fitsLandscapeIntoPortrait() {
        BufferedImage input = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        FitOperation op = new FitOperation(100, 200);

        BufferedImage result = op.apply(input);

        assertEquals(100, result.getWidth());
        assertEquals(50, result.getHeight());
    }

    @Test
    void scalesUpSmallImage() {
        BufferedImage input = new BufferedImage(50, 100, BufferedImage.TYPE_INT_RGB);
        FitOperation op = new FitOperation(200, 400);

        BufferedImage result = op.apply(input);

        assertEquals(200, result.getWidth());
        assertEquals(400, result.getHeight());
    }

    @Test
    void fitExactMatch() {
        BufferedImage input = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        FitOperation op = new FitOperation(100, 100);

        BufferedImage result = op.apply(input);

        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void fitImageAlreadySmaller() {
        BufferedImage input = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        FitOperation op = new FitOperation(50, 50);

        BufferedImage result = op.apply(input);

        assertEquals(50, result.getWidth());
        assertEquals(50, result.getHeight());
    }

    @Test
    void fitExtremeAspectRatio() {
        BufferedImage input = new BufferedImage(10, 1000, BufferedImage.TYPE_INT_RGB);
        FitOperation op = new FitOperation(100, 100);

        BufferedImage result = op.apply(input);

        assertEquals(1, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void rejectsZeroWidth() {
        assertThrows(ImageEditorException.class, () -> new FitOperation(0, 100));
    }

    @Test
    void rejectsZeroHeight() {
        assertThrows(ImageEditorException.class, () -> new FitOperation(100, 0));
    }

    @Test
    void rejectsNegativeWidth() {
        assertThrows(ImageEditorException.class, () -> new FitOperation(-1, 100));
    }

    @Test
    void rejectsNegativeHeight() {
        assertThrows(ImageEditorException.class, () -> new FitOperation(100, -1));
    }
}
