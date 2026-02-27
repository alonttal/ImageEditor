package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ImageIOHandler {

    private static final Map<ImageFormat, List<String>> CLI_TOOLS = Map.of(
            ImageFormat.WEBP, List.of("cwebp", "dwebp"),
            ImageFormat.AVIF, List.of("heif-enc", "heif-dec")
    );
    private static final long TIMEOUT_SECONDS = 30;

    private static volatile Path toolDirectory;

    public static void setToolDirectory(Path directory) {
        toolDirectory = directory;
    }

    public static Path getToolDirectory() {
        return toolDirectory;
    }

    private static String resolveToolPath(String toolName) {
        Path dir = toolDirectory;
        if (dir != null) {
            return dir.resolve(toolName).toString();
        }
        return toolName;
    }

    // --- Path-based read/write ---

    public static BufferedImage read(Path path) {
        ImageFormat format = getFormat(path.getFileName().toString());

        try {
            if (format.isStandard()) {
                return readStandard(path);
            } else if (format == ImageFormat.WEBP) {
                return readViaCliToPng(path, resolveToolPath("dwebp"), path.toAbsolutePath().toString(), "-o");
            } else if (format == ImageFormat.AVIF) {
                return readViaCliToPng(path, resolveToolPath("heif-dec"), path.toAbsolutePath().toString());
            } else {
                throw new ImageEditorException("Unsupported image format: " + format.getExtension());
            }
        } catch (IOException e) {
            throw new ImageEditorException("Failed to read image: " + path, e);
        }
    }

    public static void write(BufferedImage image, Path path) {
        write(image, path, OutputOptions.defaults());
    }

    public static void write(BufferedImage image, Path path, OutputOptions options) {
        ImageFormat format = getFormat(path.getFileName().toString());
        write(image, path, format, options);
    }

    public static void write(BufferedImage image, Path path, ImageFormat format, OutputOptions options) {
        try {
            if (format.isStandard()) {
                writeStandard(image, format, path, options);
            } else if (format == ImageFormat.WEBP) {
                writeViaCliFromPng(image, path, options, resolveToolPath("cwebp"), null, "-o", path.toAbsolutePath().toString());
            } else if (format == ImageFormat.AVIF) {
                writeViaCliFromPng(image, path, options, resolveToolPath("heif-enc"), null, "-o", path.toAbsolutePath().toString());
            } else {
                throw new ImageEditorException("Unsupported image format: " + format.getExtension());
            }
        } catch (IOException e) {
            throw new ImageEditorException("Failed to write image: " + path, e);
        }
    }

    // --- Stream-based read/write ---

    public static BufferedImage read(InputStream input, ImageFormat format) {
        try {
            if (format.isStandard()) {
                BufferedImage image = ImageIO.read(input);
                if (image == null) {
                    throw new ImageEditorException("Could not read image from stream with format hint: " + format.getExtension());
                }
                return image;
            } else if (format == ImageFormat.WEBP || format == ImageFormat.AVIF) {
                Path tmpInput = Files.createTempFile("imageeditor-in-", "." + format.getExtension());
                try {
                    try (OutputStream out = Files.newOutputStream(tmpInput)) {
                        input.transferTo(out);
                    }
                    return read(tmpInput);
                } finally {
                    Files.deleteIfExists(tmpInput);
                }
            } else {
                throw new ImageEditorException("Unsupported image format: " + format.getExtension());
            }
        } catch (IOException e) {
            throw new ImageEditorException("Failed to read image from stream", e);
        }
    }

    public static void write(BufferedImage image, OutputStream output, ImageFormat format, OutputOptions options) {
        try {
            if (format.isStandard()) {
                writeStandardToStream(image, format, output, options);
            } else if (format == ImageFormat.WEBP || format == ImageFormat.AVIF) {
                Path tmpOutput = Files.createTempFile("imageeditor-out-", "." + format.getExtension());
                try {
                    write(image, tmpOutput, format, options);
                    try (InputStream in = Files.newInputStream(tmpOutput)) {
                        in.transferTo(output);
                    }
                } finally {
                    Files.deleteIfExists(tmpOutput);
                }
            } else {
                throw new ImageEditorException("Unsupported image format: " + format.getExtension());
            }
        } catch (IOException e) {
            throw new ImageEditorException("Failed to write image to stream", e);
        }
    }

    // --- Format detection ---

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

    private static ImageFormat detectFromMagicBytes(InputStream is) throws IOException {
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

    // --- Format support check ---

    public static boolean isFormatSupported(ImageFormat format) {
        if (format.isStandard()) {
            return true;
        }
        List<String> tools = CLI_TOOLS.get(format);
        if (tools == null) {
            return false;
        }
        return tools.stream().allMatch(ImageIOHandler::isToolAvailable);
    }

    // --- Private helpers ---

    private static boolean isToolAvailable(String tool) {
        try {
            Path dir = toolDirectory;
            if (dir != null) {
                return Files.isExecutable(dir.resolve(tool));
            }
            String lookupCommand = System.getProperty("os.name", "")
                    .toLowerCase().contains("win") ? "where" : "which";
            Process p = new ProcessBuilder(lookupCommand, tool)
                    .redirectErrorStream(true).start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static BufferedImage readStandard(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new ImageEditorException("Could not read image: " + path);
        }
        return image;
    }

    private static void writeStandard(BufferedImage image, ImageFormat format, Path path, OutputOptions options)
            throws IOException {
        String writeFormat = format.getExtension();

        if (options.getQuality() != null && format == ImageFormat.JPEG) {
            writeWithQuality(image, writeFormat, path, options);
        } else if (options.isStripMetadata()) {
            writeWithWriter(image, writeFormat, path, null);
        } else {
            boolean written = ImageIO.write(image, writeFormat, path.toFile());
            if (!written) {
                throw new ImageEditorException("No writer found for format: " + writeFormat);
            }
        }
    }

    private static void writeWithQuality(BufferedImage image, String writeFormat, Path path, OutputOptions options)
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
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(path.toFile())) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }
    }

    private static void writeWithWriter(BufferedImage image, String writeFormat, Path path,
                                        ImageWriteParam param) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(writeFormat);
        if (!writers.hasNext()) {
            throw new ImageEditorException("No writer found for format: " + writeFormat);
        }
        ImageWriter writer = writers.next();
        try {
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(path.toFile())) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }
    }

    private static void writeStandardToStream(BufferedImage image, ImageFormat format, OutputStream output,
                                              OutputOptions options) throws IOException {
        String writeFormat = format.getExtension();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(writeFormat);
        if (!writers.hasNext()) {
            throw new ImageEditorException("No writer found for format: " + writeFormat);
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

    private static BufferedImage readViaCliToPng(Path inputPath, String... commandParts) throws IOException {
        Path tmpPng = Files.createTempFile("imageeditor-", ".png");
        try {
            String[] command = appendArg(commandParts, tmpPng.toAbsolutePath().toString());
            runProcess(command);
            return readStandard(tmpPng);
        } finally {
            Files.deleteIfExists(tmpPng);
        }
    }

    private static void writeViaCliFromPng(BufferedImage image, Path outputPath, OutputOptions options,
                                           String... commandParts) throws IOException {
        Path tmpPng = Files.createTempFile("imageeditor-", ".png");
        try {
            ImageIO.write(image, "png", tmpPng.toFile());
            // Build command, replacing null placeholder with tmp png path
            java.util.ArrayList<String> command = new java.util.ArrayList<>();
            for (String part : commandParts) {
                command.add(part == null ? tmpPng.toAbsolutePath().toString() : part);
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
            Files.deleteIfExists(tmpPng);
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
