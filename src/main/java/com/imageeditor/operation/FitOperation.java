package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class FitOperation implements Operation {

    private final int maxWidth;
    private final int maxHeight;

    public FitOperation(int maxWidth, int maxHeight) {
        if (maxWidth <= 0 || maxHeight <= 0) {
            throw new ImageEditorException("Fit dimensions must be positive: " + maxWidth + "x" + maxHeight);
        }
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        double scaleX = (double) maxWidth / image.getWidth();
        double scaleY = (double) maxHeight / image.getHeight();
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) Math.round(image.getWidth() * scale);
        int scaledHeight = (int) Math.round(image.getHeight() * scale);

        BufferedImage result = new BufferedImage(scaledWidth, scaledHeight, image.getType());
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return result;
    }
}
