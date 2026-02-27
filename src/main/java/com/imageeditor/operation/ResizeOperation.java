package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class ResizeOperation implements Operation {

    private final int width;
    private final int height;

    public ResizeOperation(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new ImageEditorException("Resize dimensions must be positive: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        BufferedImage resized = new BufferedImage(width, height, image.getType());
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }
}
