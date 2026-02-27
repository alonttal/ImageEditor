package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FormatDetector {

    private FormatDetector() {
    }

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
        // AVIF: ftyp at offset 4
        if (bytesRead >= 8 && header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70) {
            return ImageFormat.AVIF;
        }

        return null;
    }

    public static ImageFormat getFormat(String fileName) {
        String ext = getExtension(fileName);
        return ImageFormat.fromExtension(ext);
    }

    public static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            throw new ImageEditorException("Cannot determine format from file name: " + fileName);
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}
