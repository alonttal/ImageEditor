package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;

import java.awt.image.BufferedImage;

public class CropOperation implements Operation {

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public CropOperation(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new ImageEditorException("Crop dimensions must be positive: " + width + "x" + height);
        }
        if (x < 0 || y < 0) {
            throw new ImageEditorException("Crop origin must be non-negative: (" + x + ", " + y + ")");
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        if (x + width > image.getWidth() || y + height > image.getHeight()) {
            throw new ImageEditorException(
                    "Crop region (%d,%d %dx%d) exceeds image bounds (%dx%d)"
                            .formatted(x, y, width, height, image.getWidth(), image.getHeight()));
        }
        return image.getSubimage(x, y, width, height);
    }
}
