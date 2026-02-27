package com.imageeditor.operation;

import java.awt.image.BufferedImage;

public record ResizeOperation(int width, int height) implements Operation {

    public ResizeOperation {
        ImageScaler.requirePositiveDimensions(width, height, "Resize");
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        return ImageScaler.scale(image, width, height);
    }
}
