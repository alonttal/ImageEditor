package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ImageIOHandler {

    private static final Set<String> STANDARD_FORMATS = Set.of("png", "jpg", "jpeg", "gif", "bmp", "tiff");
    private static final Map<String, List<String>> CLI_TOOLS = Map.of(
            "webp", List.of("cwebp", "dwebp"),
            "avif", List.of("heif-enc", "heif-dec")
    );
    private static final long TIMEOUT_SECONDS = 30;

    // --- File-based read/write ---

    public static BufferedImage read(File file) {
        String ext = getExtension(file.getName());

        try {
            if (STANDARD_FORMATS.contains(ext)) {
                return readStandard(file);
            } else if ("webp".equals(ext)) {
                return readViaCliToPng(file, "dwebp", file.getAbsolutePath(), "-o");
            } else if ("avif".equals(ext)) {
                return readViaCliToPng(file, "heif-dec", file.getAbsolutePath());
            } else {
                throw new ImageEditorException("Unsupported image format: " + ext);
            }
        } catch (IOException e) {
            throw new ImageEditorException("Failed to read image: " + file, e);
        }
    }

    public static void write(BufferedImage image, File file) {
        write(image, file, OutputOptions.defaults());
    }

    public static void write(BufferedImage image, File file, OutputOptions options) {
        String ext = getExtension(file.getName());

        try {
            if (STANDARD_FORMATS.contains(ext)) {
                writeStandard(image, ext, file, options);
            } else if ("webp".equals(ext)) {
                writeViaCliFromPng(image, file, options, "cwebp", null, "-o", file.getAbsolutePath());
            } else if ("avif".equals(ext)) {
                writeViaCliFromPng(image, file, options, "heif-enc", null, "-o", file.getAbsolutePath());
            } else {
                throw new ImageEditorException("Unsupported image format: " + ext);
            }
        } catch (IOException e) {
            throw new ImageEditorException("Failed to write image: " + file, e);
        }
    }

    // --- Stream-based read/write ---

    public static BufferedImage read(InputStream input, String formatHint) {
        String ext = formatHint.toLowerCase();

        try {
            if (STANDARD_FORMATS.contains(ext)) {
                BufferedImage image = ImageIO.read(input);
                if (image == null) {
                    throw new ImageEditorException("Could not read image from stream with format hint: " + formatHint);
                }
                return image;
            } else if ("webp".equals(ext) || "avif".equals(ext)) {
                File tmpInput = File.createTempFile("imageeditor-in-", "." + ext);
                try {
                    try (OutputStream out = new FileOutputStream(tmpInput)) {
                        input.transferTo(out);
                    }
                    return read(tmpInput);
                } finally {
                    tmpInput.delete();
                }
            } else {
                throw new ImageEditorException("Unsupported image format: " + ext);
            }
        } catch (IOException e) {
            throw new ImageEditorException("Failed to read image from stream", e);
        }
    }

    public static void write(BufferedImage image, OutputStream output, String format, OutputOptions options) {
        String ext = format.toLowerCase();

        try {
            if (STANDARD_FORMATS.contains(ext)) {
                writeStandardToStream(image, ext, output, options);
            } else if ("webp".equals(ext) || "avif".equals(ext)) {
                File tmpOutput = File.createTempFile("imageeditor-out-", "." + ext);
                try {
                    write(image, tmpOutput, options);
                    try (InputStream in = new FileInputStream(tmpOutput)) {
                        in.transferTo(output);
                    }
                } finally {
                    tmpOutput.delete();
                }
            } else {
                throw new ImageEditorException("Unsupported image format: " + ext);
            }
        } catch (IOException e) {
            throw new ImageEditorException("Failed to write image to stream", e);
        }
    }

    // --- Format detection ---

    public static String detectFormat(File file) {
        try (InputStream is = new FileInputStream(file)) {
            String detected = detectFromMagicBytes(is);
            if (detected != null) {
                return detected;
            }
        } catch (IOException ignored) {
        }
        try {
            return getExtension(file.getName());
        } catch (ImageEditorException e) {
            return null;
        }
    }

    public static String detectFormat(InputStream input) {
        if (!input.markSupported()) {
            input = new BufferedInputStream(input);
        }
        try {
            input.mark(12);
            String detected = detectFromMagicBytes(input);
            input.reset();
            return detected;
        } catch (IOException e) {
            return null;
        }
    }

    private static String detectFromMagicBytes(InputStream is) throws IOException {
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
            return "png";
        }
        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        // GIF: 47 49 46 38
        if (bytesRead >= 4 && header[0] == 0x47 && header[1] == 0x49
                && header[2] == 0x46 && header[3] == 0x38) {
            return "gif";
        }
        // BMP: 42 4D
        if (header[0] == 0x42 && header[1] == 0x4D) {
            return "bmp";
        }
        // TIFF: 49 49 2A 00 (little-endian) or 4D 4D 00 2A (big-endian)
        if (bytesRead >= 4) {
            if (header[0] == 0x49 && header[1] == 0x49 && header[2] == 0x2A && header[3] == 0x00) {
                return "tiff";
            }
            if (header[0] == 0x4D && header[1] == 0x4D && header[2] == 0x00 && header[3] == 0x2A) {
                return "tiff";
            }
        }
        // WebP: RIFF at 0 + WEBP at 8
        if (bytesRead >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
            return "webp";
        }
        // AVIF: ftyp at offset 4
        if (bytesRead >= 8 && header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70) {
            return "avif";
        }

        return null;
    }

    // --- Format support check ---

    public static boolean isFormatSupported(String format) {
        String ext = format.toLowerCase();
        if (STANDARD_FORMATS.contains(ext)) {
            return true;
        }
        List<String> tools = CLI_TOOLS.get(ext);
        if (tools == null) {
            return false;
        }
        return tools.stream().allMatch(ImageIOHandler::isToolAvailable);
    }

    // --- Private helpers ---

    private static boolean isToolAvailable(String tool) {
        try {
            String lookupCommand = System.getProperty("os.name", "")
                    .toLowerCase().contains("win") ? "where" : "which";
            Process p = new ProcessBuilder(lookupCommand, tool)
                    .redirectErrorStream(true).start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static BufferedImage readStandard(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new ImageEditorException("Could not read image: " + file);
        }
        return image;
    }

    private static void writeStandard(BufferedImage image, String format, File file, OutputOptions options)
            throws IOException {
        String writeFormat = "jpg".equals(format) ? "jpeg" : format;

        if (options.getQuality() != null && ("jpeg".equals(writeFormat) || "jpg".equals(format))) {
            writeWithQuality(image, writeFormat, file, options);
        } else if (options.isStripMetadata()) {
            writeWithWriter(image, writeFormat, file, null);
        } else {
            boolean written = ImageIO.write(image, writeFormat, file);
            if (!written) {
                throw new ImageEditorException("No writer found for format: " + format);
            }
        }
    }

    private static void writeWithQuality(BufferedImage image, String writeFormat, File file, OutputOptions options)
            throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(writeFormat);
        if (!writers.hasNext()) {
            throw new ImageEditorException("No writer found for format: " + writeFormat);
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] types = param.getCompressionTypes();
                if (types != null && types.length > 0) {
                    param.setCompressionType(types[0]);
                }
                param.setCompressionQuality(options.getQuality());
            }
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(file)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }
    }

    private static void writeWithWriter(BufferedImage image, String writeFormat, File file,
                                        ImageWriteParam param) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(writeFormat);
        if (!writers.hasNext()) {
            throw new ImageEditorException("No writer found for format: " + writeFormat);
        }
        ImageWriter writer = writers.next();
        try {
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(file)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }
    }

    private static void writeStandardToStream(BufferedImage image, String format, OutputStream output,
                                              OutputOptions options) throws IOException {
        String writeFormat = "jpg".equals(format) ? "jpeg" : format;

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(writeFormat);
        if (!writers.hasNext()) {
            throw new ImageEditorException("No writer found for format: " + format);
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (options.getQuality() != null && param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] types = param.getCompressionTypes();
                if (types != null && types.length > 0) {
                    param.setCompressionType(types[0]);
                }
                param.setCompressionQuality(options.getQuality());
            }
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }
    }

    private static BufferedImage readViaCliToPng(File inputFile, String... commandParts) throws IOException {
        File tmpPng = File.createTempFile("imageeditor-", ".png");
        try {
            String[] command = appendArg(commandParts, tmpPng.getAbsolutePath());
            runProcess(command);
            return readStandard(tmpPng);
        } finally {
            tmpPng.delete();
        }
    }

    private static void writeViaCliFromPng(BufferedImage image, File outputFile, OutputOptions options,
                                           String... commandParts) throws IOException {
        File tmpPng = File.createTempFile("imageeditor-", ".png");
        try {
            ImageIO.write(image, "png", tmpPng);
            // Build command, replacing null placeholder with tmp png path
            java.util.ArrayList<String> command = new java.util.ArrayList<>();
            for (String part : commandParts) {
                command.add(part == null ? tmpPng.getAbsolutePath() : part);
            }
            // Insert quality flag if specified
            if (options.getQuality() != null) {
                int qValue = Math.round(options.getQuality() * 100);
                // Insert -q <value> right after the tool name
                command.add(1, "-q");
                command.add(2, String.valueOf(qValue));
            }
            runProcess(command.toArray(new String[0]));
        } finally {
            tmpPng.delete();
        }
    }

    private static void runProcess(String[] command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            byte[] output = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new ImageEditorException(
                        "CLI tool timed out after " + TIMEOUT_SECONDS + "s: " + command[0]);
            }

            if (process.exitValue() != 0) {
                String errorOutput = new String(output).trim();
                throw new ImageEditorException(
                        "CLI tool failed (exit " + process.exitValue() + "): " + command[0]
                                + (errorOutput.isEmpty() ? "" : "\n" + errorOutput));
            }
        } catch (IOException e) {
            throw new ImageEditorException("CLI tool not found or failed to execute: " + command[0], e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImageEditorException("Interrupted while running CLI tool: " + command[0], e);
        }
    }

    private static String[] appendArg(String[] parts, String arg) {
        String[] result = new String[parts.length + 1];
        System.arraycopy(parts, 0, result, 0, parts.length);
        result[parts.length] = arg;
        return result;
    }

    public static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            throw new ImageEditorException("Cannot determine format from file name: " + fileName);
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}
