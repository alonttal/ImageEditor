package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects image formats by inspecting file magic bytes or file extensions.
 */
public class FormatDetector {

    private FormatDetector() {
    }

    /**
     * Detects the image format of the file at the given path.
     *
     * <p>First attempts detection via magic bytes; falls back to the file
     * extension if magic-byte detection fails.
     *
     * @param path path to the image file
     * @return the detected format, or {@code null} if detection fails
     */
    public static ImageFormat detectFormat(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            ImageFormat detected = detectFromMagicBytes(is);
            if (detected != null) {
                return detected;
            }
        } catch (IOException ignored) {
        }
        try {
            return getFormat(path.getFileName().toString());
        } catch (ImageEditorException e) {
            return null;
        }
    }

    /**
     * Detects the image format by inspecting the leading bytes of the stream.
     *
     * <p>The stream is marked before reading and reset afterwards, so it can
     * still be consumed by the caller. If the stream does not support
     * {@link InputStream#mark(int)}, it is automatically wrapped in a
     * {@link BufferedInputStream}.
     *
     * @param input the image input stream
     * @return the detected format, or {@code null} if detection fails
     */
    public static ImageFormat detectFormat(InputStream input) {
        if (!input.markSupported()) {
            input = new BufferedInputStream(input);
        }
        try {
            input.mark(12);
            ImageFormat detected = detectFromMagicBytes(input);
            input.reset();
            return detected;
        } catch (IOException e) {
            return null;
        }
    }

    static ImageFormat detectFromMagicBytes(InputStream is) throws IOException {
        byte[] header = new byte[12];
        int bytesRead = 0;
        while (bytesRead < 12) {
            int r = is.read(header, bytesRead, 12 - bytesRead);
            if (r < 0) break;
            bytesRead += r;
        }
        if (bytesRead < 3) {
            return null;
        }

        // PNG: 89 50 4E 47
        if (bytesRead >= 4 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50
                && header[2] == 0x4E && header[3] == 0x47) {
            return ImageFormat.PNG;
        }
        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return ImageFormat.JPEG;
        }
        // GIF: 47 49 46 38
        if (bytesRead >= 4 && header[0] == 0x47 && header[1] == 0x49
                && header[2] == 0x46 && header[3] == 0x38) {
            return ImageFormat.GIF;
        }
        // BMP: 42 4D
        if (header[0] == 0x42 && header[1] == 0x4D) {
            return ImageFormat.BMP;
        }
        // TIFF: 49 49 2A 00 (little-endian) or 4D 4D 00 2A (big-endian)
        if (bytesRead >= 4) {
            if (header[0] == 0x49 && header[1] == 0x49 && header[2] == 0x2A && header[3] == 0x00) {
                return ImageFormat.TIFF;
            }
            if (header[0] == 0x4D && header[1] == 0x4D && header[2] == 0x00 && header[3] == 0x2A) {
                return ImageFormat.TIFF;
            }
        }
        // WebP: RIFF at 0 + WEBP at 8
        if (bytesRead >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
            return ImageFormat.WEBP;
        }
        // AVIF: ftyp at offset 4, major brand at offset 8 must be avif, avis, or mif1
        if (bytesRead >= 12 && header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70) {
            String brand = new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (brand.equals("avif") || brand.equals("avis") || brand.equals("mif1")) {
                return ImageFormat.AVIF;
            }
        }

        return null;
    }

    /**
     * Returns the {@link ImageFormat} for the given file name based on its
     * extension.
     *
     * @param fileName a file name with an extension (e.g. {@code "photo.png"})
     * @return the corresponding format
     * @throws ImageEditorException if the extension is missing or unsupported
     */
    public static ImageFormat getFormat(String fileName) {
        String ext = getExtension(fileName);
        return ImageFormat.fromExtension(ext);
    }

    /**
     * Extracts the lower-case file extension from a file name.
     *
     * @param fileName a file name (e.g. {@code "photo.PNG"})
     * @return the extension in lower case (e.g. {@code "png"})
     * @throws ImageEditorException if the file name has no extension
     */
    public static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            throw new ImageEditorException("Cannot determine format from file name: " + fileName);
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}
