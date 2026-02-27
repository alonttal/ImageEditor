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

public class ImageIOHandler {

    public static void setToolDirectory(Path directory) {
        CliToolRunner.setToolDirectory(directory);
    }

    public static Path getToolDirectory() {
        return CliToolRunner.getToolDirectory();
    }

    // --- Path-based read/write ---

    public static BufferedImage read(Path path) {
        ImageFormat format = getFormat(path.getFileName().toString());

        try {
            if (format.isStandard()) {
                return readStandard(path);
            } else if (format == ImageFormat.WEBP) {
                return readViaCliToPng(path, CliToolRunner.resolveToolPath("dwebp"), path.toAbsolutePath().toString(), "-o");
            } else if (format == ImageFormat.AVIF) {
                return readViaCliToPng(path, CliToolRunner.resolveToolPath("heif-dec"), path.toAbsolutePath().toString());
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
                writeViaCliFromPng(image, path, options, CliToolRunner.resolveToolPath("cwebp"), null, "-o", path.toAbsolutePath().toString());
            } else if (format == ImageFormat.AVIF) {
                writeViaCliFromPng(image, path, options, CliToolRunner.resolveToolPath("heif-enc"), null, "-o", path.toAbsolutePath().toString());
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

    // --- Format detection (delegates to FormatDetector) ---

    public static ImageFormat detectFormat(Path path) {
        return FormatDetector.detectFormat(path);
    }

    public static ImageFormat detectFormat(InputStream input) {
        return FormatDetector.detectFormat(input);
    }

    // --- Format support check (delegates to CliToolRunner) ---

    public static boolean isFormatSupported(ImageFormat format) {
        return format.isStandard() || CliToolRunner.isCliFormatSupported(format);
    }

    // --- Private helpers ---

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

        if (options.quality() != null && format == ImageFormat.JPEG) {
            writeViaImageWriter(image, writeFormat, path.toFile(), options);
        } else if (options.stripMetadata()) {
            writeViaImageWriter(image, writeFormat, path.toFile(), OutputOptions.defaults());
        } else {
            boolean written = ImageIO.write(image, writeFormat, path.toFile());
            if (!written) {
                throw new ImageEditorException("No writer found for format: " + writeFormat);
            }
        }
    }

    private static void writeStandardToStream(BufferedImage image, ImageFormat format, OutputStream output,
                                              OutputOptions options) throws IOException {
        writeViaImageWriter(image, format.getExtension(), output, options);
    }

    private static void writeViaImageWriter(BufferedImage image, String formatName,
                                            Object target, OutputOptions options) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
        if (!writers.hasNext()) {
            throw new ImageEditorException("No writer found for format: " + formatName);
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (options.quality() != null && param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] types = param.getCompressionTypes();
                if (types != null && types.length > 0) {
                    param.setCompressionType(types[0]);
                }
                param.setCompressionQuality(options.quality());
            }
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(target)) {
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
            String[] command = CliToolRunner.appendArg(commandParts, tmpPng.toAbsolutePath().toString());
            CliToolRunner.runProcess(command);
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
            if (options.quality() != null) {
                int qValue = Math.round(options.quality() * 100);
                // Insert -q <value> right after the tool name
                command.add(1, "-q");
                command.add(2, String.valueOf(qValue));
            }
            CliToolRunner.runProcess(command.toArray(new String[0]));
        } finally {
            Files.deleteIfExists(tmpPng);
        }
    }

    public static ImageFormat getFormat(String fileName) {
        return FormatDetector.getFormat(fileName);
    }

    public static String getExtension(String fileName) {
        return FormatDetector.getExtension(fileName);
    }
}
