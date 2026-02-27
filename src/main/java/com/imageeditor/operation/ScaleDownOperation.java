package com.imageeditor.operation;

import java.awt.image.BufferedImage;

/**
 * Scales an image down to fit within the given bounding box while preserving
 * the original aspect ratio. Unlike {@link FitOperation}, this operation never
 * enlarges an image — if the image already fits within the bounds it is
 * returned unchanged. This mirrors the CSS {@code object-fit: scale-down}
 * behaviour.
 *
 * @param maxWidth  maximum width in pixels (must be positive)
 * @param maxHeight maximum height in pixels (must be positive)
 * @throws com.imageeditor.exception.ImageEditorException if maxWidth or maxHeight is not positive
 */
public record ScaleDownOperation(int maxWidth, int maxHeight) implements Operation {

    public ScaleDownOperation {
        ImageScaler.requirePositiveDimensions(maxWidth, maxHeight, "ScaleDown");
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        if (image.getWidth() <= maxWidth && image.getHeight() <= maxHeight) {
            return image;
        }

        double scaleX = (double) maxWidth / image.getWidth();
        double scaleY = (double) maxHeight / image.getHeight();
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) Math.round(image.getWidth() * scale);
        int scaledHeight = (int) Math.round(image.getHeight() * scale);

        return ImageScaler.scale(image, scaledWidth, scaledHeight);
    }
}
