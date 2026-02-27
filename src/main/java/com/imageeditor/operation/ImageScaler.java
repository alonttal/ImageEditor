package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

class ImageScaler {

    private ImageScaler() {
    }

    static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, source.getType());
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    static BufferedImage copyRegion(BufferedImage source, int x, int y, int width, int height) {
        BufferedImage copy = new BufferedImage(width, height, source.getType());
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, width, height, x, y, x + width, y + height, null);
        g.dispose();
        return copy;
    }

    static void requirePositiveDimensions(int width, int height, String operationName) {
        if (width <= 0 || height <= 0) {
            throw new ImageEditorException(operationName + " dimensions must be positive: " + width + "x" + height);
        }
    }
}
