package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class CoverOperation implements Operation {

    private final int targetWidth;
    private final int targetHeight;

    public CoverOperation(int targetWidth, int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new ImageEditorException("Cover dimensions must be positive: " + targetWidth + "x" + targetHeight);
        }
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        double scaleX = (double) targetWidth / image.getWidth();
        double scaleY = (double) targetHeight / image.getHeight();
        double scale = Math.max(scaleX, scaleY);

        int scaledWidth = (int) Math.round(image.getWidth() * scale);
        int scaledHeight = (int) Math.round(image.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, image.getType());
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();

        int cropX = (scaledWidth - targetWidth) / 2;
        int cropY = (scaledHeight - targetHeight) / 2;

        return scaled.getSubimage(cropX, cropY, targetWidth, targetHeight);
    }
}
