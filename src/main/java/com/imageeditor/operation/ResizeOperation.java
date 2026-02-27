package com.imageeditor.operation;

import java.awt.image.BufferedImage;

/**
 * Scales an image to exact dimensions, ignoring the original aspect ratio.
 *
 * @param width  target width in pixels (must be positive)
 * @param height target height in pixels (must be positive)
 * @throws ImageEditorException if width or height is not positive
 */
public record ResizeOperation(int width, int height) implements Operation {

    public ResizeOperation {
        ImageScaler.requirePositiveDimensions(width, height, "Resize");
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        return ImageScaler.scale(image, width, height);
    }
}
