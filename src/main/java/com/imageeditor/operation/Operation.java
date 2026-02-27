package com.imageeditor.operation;

import java.awt.image.BufferedImage;

/**
 * A single image transformation step in the processing pipeline.
 *
 * <p>Implementations must be safe to apply from multiple threads concurrently
 * as long as different {@link BufferedImage} instances are passed.
 */
public interface Operation {

    /**
     * Applies this operation to the given image and returns the result.
     *
     * @param image the source image (never {@code null})
     * @return the transformed image (never {@code null})
     * @throws com.imageeditor.exception.ImageEditorException if the operation
     *         cannot be applied (e.g. crop region out of bounds)
     */
    BufferedImage apply(BufferedImage image);
}
