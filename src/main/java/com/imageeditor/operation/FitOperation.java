package com.imageeditor.operation;

import java.awt.image.BufferedImage;

/**
 * Scales an image to fit within the given bounding box while preserving the
 * original aspect ratio. The result is never larger than the specified
 * dimensions, but may be smaller in one axis.
 *
 * @param maxWidth  maximum width in pixels (must be positive)
 * @param maxHeight maximum height in pixels (must be positive)
 * @throws ImageEditorException if maxWidth or maxHeight is not positive
 */
public record FitOperation(int maxWidth, int maxHeight) implements Operation {

    public FitOperation {
        ImageScaler.requirePositiveDimensions(maxWidth, maxHeight, "Fit");
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        double scaleX = (double) maxWidth / image.getWidth();
        double scaleY = (double) maxHeight / image.getHeight();
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) Math.round(image.getWidth() * scale);
        int scaledHeight = (int) Math.round(image.getHeight() * scale);

        return ImageScaler.scale(image, scaledWidth, scaledHeight);
    }
}
