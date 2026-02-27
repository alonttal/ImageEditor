package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;

import java.awt.image.BufferedImage;

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
