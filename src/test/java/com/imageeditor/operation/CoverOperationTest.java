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
    void coverExactMatch() {
        BufferedImage input = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        CoverOperation op = new CoverOperation(100, 100);

        BufferedImage result = op.apply(input);

        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void cover1x1Target() {
        BufferedImage input = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        CoverOperation op = new CoverOperation(1, 1);

        BufferedImage result = op.apply(input);

        assertEquals(1, result.getWidth());
        assertEquals(1, result.getHeight());
    }

    @Test
    void coverExtremeAspectRatio() {
        BufferedImage input = new BufferedImage(1000, 1, BufferedImage.TYPE_INT_RGB);
        CoverOperation op = new CoverOperation(100, 100);

        BufferedImage result = op.apply(input);

        assertEquals(100, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    void coverDimensionsThatCouldRoundDown() {
        // Dimensions chosen so that Math.round(width * scale) could round below target.
        // E.g. 3x7 cover to 2x5: scaleX=0.6667, scaleY=0.7143, scale=0.7143
        // scaledWidth = round(3 * 0.7143) = round(2.1429) = 2 (exactly target)
        // scaledHeight = round(7 * 0.7143) = round(5.0) = 5 (exactly target)
        // Without clamping, a different pair could round below target.
        BufferedImage input = new BufferedImage(3, 7, BufferedImage.TYPE_INT_RGB);
        CoverOperation op = new CoverOperation(2, 5);

        BufferedImage result = op.apply(input);

        assertEquals(2, result.getWidth());
        assertEquals(5, result.getHeight());
    }

    @Test
    void coverOddDimensionsProducesExactTarget() {
        // 7x3 → cover 5x2: scale = max(5/7, 2/3) = max(0.7143, 0.6667) = 0.7143
        // scaledWidth = round(7 * 0.7143) = round(5.0001) = 5, scaledHeight = round(3 * 0.7143) = round(2.1429) = 2
        // Both exactly target, but with slightly different inputs rounding could differ.
        BufferedImage input = new BufferedImage(7, 3, BufferedImage.TYPE_INT_RGB);
        CoverOperation op = new CoverOperation(5, 2);

        BufferedImage result = op.apply(input);

        assertEquals(5, result.getWidth());
        assertEquals(2, result.getHeight());
    }

    @Test
    void coverPrimeDimensionsProducesExactTarget() {
        // Prime numbers are more likely to cause rounding issues.
        // 13x17 → cover 11x7
        BufferedImage input = new BufferedImage(13, 17, BufferedImage.TYPE_INT_RGB);
        CoverOperation op = new CoverOperation(11, 7);

        BufferedImage result = op.apply(input);

        assertEquals(11, result.getWidth());
        assertEquals(7, result.getHeight());
    }

    @Test
    void rejectsZeroWidth() {
        assertThrows(ImageEditorException.class, () -> new CoverOperation(0, 100));
    }

    @Test
    void rejectsZeroHeight() {
        assertThrows(ImageEditorException.class, () -> new CoverOperation(100, 0));
    }

    @Test
    void rejectsNegativeWidth() {
        assertThrows(ImageEditorException.class, () -> new CoverOperation(-1, 100));
    }

    @Test
    void rejectsNegativeHeight() {
        assertThrows(ImageEditorException.class, () -> new CoverOperation(100, -1));
    }
}
