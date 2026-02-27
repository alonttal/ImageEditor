package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;

import java.util.HashMap;
import java.util.Map;

/**
 * Supported image formats.
 *
 * <p>Standard formats (PNG, JPEG, GIF, BMP, TIFF) are handled by Java's
 * built-in ImageIO. Non-standard formats (WEBP, AVIF) require external CLI
 * tools.
 */
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

    /**
     * Returns the canonical file extension for this format (e.g. {@code "png"},
     * {@code "jpeg"}).
     *
     * @return the lower-case extension without a leading dot
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Looks up a format by its file extension (case-insensitive). Aliases such
     * as {@code "jpg"} for {@link #JPEG} are recognised.
     *
     * @param ext the file extension (e.g. {@code "png"}, {@code "jpg"})
     * @return the matching format
     * @throws ImageEditorException if the extension is {@code null} or
     *         not recognised
     */
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

    /**
     * Returns whether this format is handled natively by Java ImageIO
     * (PNG, JPEG, GIF, BMP, TIFF) without external CLI tools.
     *
     * @return {@code true} if the format is a standard ImageIO format
     */
    public boolean isStandard() {
        return this == PNG || this == JPEG || this == GIF || this == BMP || this == TIFF;
    }
}
