package com.imageeditor.operation;

import com.imageeditor.exception.ImageEditorException;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Utility class for scaling and copying regions of {@link BufferedImage}s.
 *
 * <p>All methods are package-private and used by the various {@code Operation}
 * implementations.
 */
class ImageScaler {

    private ImageScaler() {
    }

    /**
     * Scales the source image to the given dimensions using bilinear
     * interpolation.
     *
     * @param source the image to scale
     * @param width  target width in pixels
     * @param height target height in pixels
     * @return a new {@link BufferedImage} with the requested dimensions
     */
    static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, safeType(source));
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    /**
     * Copies a rectangular region from the source image into a new image.
     *
     * @param source the source image
     * @param x      left edge of the region
     * @param y      top edge of the region
     * @param width  width of the region in pixels
     * @param height height of the region in pixels
     * @return a new {@link BufferedImage} containing the copied region
     */
    static BufferedImage copyRegion(BufferedImage source, int x, int y, int width, int height) {
        BufferedImage copy = new BufferedImage(width, height, safeType(source));
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, width, height, x, y, x + width, y + height, null);
        g.dispose();
        return copy;
    }

    /**
     * Validates that the given dimensions are positive, throwing an
     * {@link ImageEditorException} otherwise.
     *
     * @param width         the width to validate
     * @param height        the height to validate
     * @param operationName name of the operation, included in the error message
     * @throws ImageEditorException if width or height is &le; 0
     */
    static void requirePositiveDimensions(int width, int height, String operationName) {
        if (width <= 0 || height <= 0) {
            throw new ImageEditorException(operationName + " dimensions must be positive: " + width + "x" + height);
        }
    }

    /**
     * Returns a safe image type for creating new {@link BufferedImage}s.
     *
     * <p>{@link BufferedImage#TYPE_CUSTOM} (0) cannot be used as the type
     * argument for the {@code BufferedImage} constructor, so this method
     * falls back to {@link BufferedImage#TYPE_INT_ARGB} in that case.
     *
     * @param image the source image
     * @return the image type, or {@code TYPE_INT_ARGB} if the type is
     *         {@code TYPE_CUSTOM}
     */
    private static int safeType(BufferedImage image) {
        int type = image.getType();
        return type == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : type;
    }
}
