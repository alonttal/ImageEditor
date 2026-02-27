package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;

import java.util.HashMap;
import java.util.Map;

public enum ImageFormat {
    PNG("png"),
    JPEG("jpeg", "jpg"),
    GIF("gif"),
    BMP("bmp"),
    TIFF("tiff"),
    WEBP("webp"),
    AVIF("avif");

    private final String extension;
    private final String[] aliases;

    private static final Map<String, ImageFormat> LOOKUP = new HashMap<>();

    static {
        for (ImageFormat format : values()) {
            LOOKUP.put(format.extension, format);
            for (String alias : format.aliases) {
                LOOKUP.put(alias, format);
            }
        }
    }

    ImageFormat(String extension, String... aliases) {
        this.extension = extension;
        this.aliases = aliases;
    }

    public String getExtension() {
        return extension;
    }

    public static ImageFormat fromExtension(String ext) {
        if (ext == null) {
            throw new ImageEditorException("Image format extension must not be null");
        }
        ImageFormat format = LOOKUP.get(ext.toLowerCase());
        if (format == null) {
            throw new ImageEditorException("Unsupported image format: " + ext);
        }
        return format;
    }

    public boolean isStandard() {
        return this == PNG || this == JPEG || this == GIF || this == BMP || this == TIFF;
    }
}
