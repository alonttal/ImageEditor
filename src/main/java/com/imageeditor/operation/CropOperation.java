package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;

import java.awt.image.BufferedImage;

/**
 * Extracts a rectangular region from an image.
 *
 * @param x      left edge of the crop region (must be non-negative)
 * @param y      top edge of the crop region (must be non-negative)
 * @param width  width of the crop region in pixels (must be positive)
 * @param height height of the crop region in pixels (must be positive)
 * @throws ImageEditorException if coordinates are negative, dimensions are not
 *         positive, or the region extends beyond the image bounds at apply time
 */
public record CropOperation(int x, int y, int width, int height) implements Operation {

    public CropOperation {
        ImageScaler.requirePositiveDimensions(width, height, "Crop");
        if (x < 0 || y < 0) {
            throw new ImageEditorException("Crop origin must be non-negative: (" + x + ", " + y + ")");
        }
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        if (x + width > image.getWidth() || y + height > image.getHeight()) {
            throw new ImageEditorException(
                    "Crop region (%d,%d %dx%d) exceeds image bounds (%dx%d)"
                            .formatted(x, y, width, height, image.getWidth(), image.getHeight()));
        }
        return ImageScaler.copyRegion(image, x, y, width, height);
    }
}
