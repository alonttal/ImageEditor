package io.github.alonttal.imageeditor.operation;

import io.github.alonttal.imageeditor.exception.ImageEditorException;

import java.awt.image.BufferedImage;

/**
 * Extracts a rectangular region from an image.
 *
 * @param x      left edge of the crop region (must be non-negative)
 * @param y      top edge of the crop region (must be non-negative)
 * @param width  width of the crop region in pixels (must be positive)
 * @param height height of the crop region in pixels (must be positive)
 */
public record CropOperation(int x, int y, int width, int height) implements Operation {

    /**
     * @throws ImageEditorException if coordinates are negative or dimensions are not positive
     */
    public CropOperation {
        ImageScaler.requirePositiveDimensions(width, height, "Crop");
        if (x < 0 || y < 0) {
            throw new ImageEditorException("Crop origin must be non-negative: (" + x + ", " + y + ")");
        }
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        if ((long) x + width > image.getWidth() || (long) y + height > image.getHeight()) {
            throw new ImageEditorException(
                    "Crop region (%d,%d %dx%d) exceeds image bounds (%dx%d)"
                            .formatted(x, y, width, height, image.getWidth(), image.getHeight()));
        }
        return ImageScaler.copyRegion(image, x, y, width, height);
    }
}
