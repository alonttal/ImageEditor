package com.imageeditor.exception;

/**
 * Unchecked exception thrown when an image-editing operation fails.
 *
 * <p>Common causes include unsupported formats, I/O errors, missing CLI tools,
 * and invalid operation parameters.
 */
public class ImageEditorException extends RuntimeException {

    /**
     * Creates an exception with the given detail message.
     *
     * @param message description of the failure
     */
    public ImageEditorException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the given detail message and underlying cause.
     *
     * @param message description of the failure
     * @param cause   the underlying exception
     */
    public ImageEditorException(String message, Throwable cause) {
        super(message, cause);
    }
}
