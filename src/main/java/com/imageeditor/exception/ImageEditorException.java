package com.imageeditor.exception;

public class ImageEditorException extends RuntimeException {

    public ImageEditorException(String message) {
        super(message);
    }

    public ImageEditorException(String message, Throwable cause) {
        super(message, cause);
    }
}
