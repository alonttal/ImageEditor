package com.imageeditor.operation;

import java.awt.image.BufferedImage;

public record CoverOperation(int targetWidth, int targetHeight) implements Operation {

    public CoverOperation {
        ImageScaler.requirePositiveDimensions(targetWidth, targetHeight, "Cover");
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        double scaleX = (double) targetWidth / image.getWidth();
        double scaleY = (double) targetHeight / image.getHeight();
        double scale = Math.max(scaleX, scaleY);

        int scaledWidth = (int) Math.round(image.getWidth() * scale);
        int scaledHeight = (int) Math.round(image.getHeight() * scale);

        BufferedImage scaled = ImageScaler.scale(image, scaledWidth, scaledHeight);

        int cropX = (scaledWidth - targetWidth) / 2;
        int cropY = (scaledHeight - targetHeight) / 2;

        return ImageScaler.copyRegion(scaled, cropX, cropY, targetWidth, targetHeight);
    }
}
