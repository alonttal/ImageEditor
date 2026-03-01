package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Central facade for reading and writing images in all supported formats.
 *
 * <p>Standard formats (PNG, JPEG, GIF, BMP, TIFF) are handled via Java
 * ImageIO. Non-standard formats (WebP, AVIF) are delegated to external CLI
 * tools managed by {@link CliToolRunner}.
 */
public class ImageIOHandler {

    /**
     * Sets the directory where external CLI tools (e.g. {@code cwebp},
     * {@code heif-enc}) are located.
     *
     * @param directory path to the tool directory, or {@code null} to
     *                  resolve tools from the system {@code PATH}
     */
    public static void setToolDirectory(Path directory) {
        CliToolRunner.setToolDirectory(directory);
    }

    /**
     * Returns the currently configured CLI tool directory.
     *
     * @return the tool directory, or {@code null} if tools are resolved from
     *         the system {@code PATH}
     */
    public static Path getToolDirectory() {
        return CliToolRunner.getToolDirectory();
    }

    // --- Path-based read/write ---

    /**
     * Reads an image from the given file path.
     *
     * @param path path to the image file
     * @return the decoded image
     * @throws ImageEditorException if the format is unsupported or reading fails
     */
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

    /**
     * Writes an image to the given path using default output options and the
     * format inferred from the file extension.
     *
     * @param image the image to write
     * @param path  destination file path
     * @throws ImageEditorException if the format is unsupported or writing fails
     */
    public static void write(BufferedImage image, Path path) {
        write(image, path, OutputOptions.defaults());
    }

    /**
     * Writes an image to the given path with the specified output options and
     * the format inferred from the file extension.
     *
     * @param image   the image to write
     * @param path    destination file path
     * @param options output options (quality, metadata stripping, etc.)
     * @throws ImageEditorException if the format is unsupported or writing fails
     */
    public static void write(BufferedImage image, Path path, OutputOptions options) {
        ImageFormat format = getFormat(path.getFileName().toString());
        write(image, path, format, options);
    }

    /**
     * Writes an image to the given path in the specified format.
     *
     * @param image   the image to write
     * @param path    destination file path
     * @param format  the image format to encode as
     * @param options output options (quality, metadata stripping, etc.)
     * @throws ImageEditorException if the format is unsupported or writing fails
     */
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

    /**
     * Reads an image from a stream with an explicit format hint.
     *
     * <p>For non-standard formats the stream is spooled to a temp file and
     * processed via CLI tools.
     *
     * @param input  stream containing the image data
     * @param format the image format of the stream content
     * @return the decoded image
     * @throws ImageEditorException if reading fails or the format is unsupported
     */
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

    /**
     * Writes an image to an output stream in the specified format.
     *
     * <p>For non-standard formats the image is written to a temp file via CLI
     * tools and then streamed to the output.
     *
     * @param image   the image to write
     * @param output  destination stream
     * @param format  the image format to encode as
     * @param options output options (quality, metadata stripping, etc.)
     * @throws ImageEditorException if writing fails or the format is unsupported
     */
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

    /**
     * Detects the image format of the file at the given path.
     *
     * @param path path to the image file
     * @return the detected format, or {@code null} if detection fails
     * @see FormatDetector#detectFormat(Path)
     */
    public static ImageFormat detectFormat(Path path) {
        return FormatDetector.detectFormat(path);
    }

    /**
     * Detects the image format by inspecting the stream's leading bytes.
     *
     * <p>The stream is marked and reset so that it can still be read
     * afterwards. The stream must support {@link InputStream#mark(int)};
     * if it does not, wrap it in a {@link BufferedInputStream} first.
     *
     * @param input an input stream that supports {@link InputStream#mark(int)}
     * @return the detected format, or {@code null} if detection fails
     * @throws IllegalArgumentException if the stream does not support mark/reset
     * @see FormatDetector#detectFormat(InputStream)
     */
    public static ImageFormat detectFormat(InputStream input) {
        return FormatDetector.detectFormat(input);
    }

    // --- Format support check (delegates to CliToolRunner) ---

    /**
     * Returns whether the given format can be read and written on the current
     * system. Standard formats are always supported; non-standard formats
     * require their CLI tools to be available.
     *
     * @param format the format to check
     * @return {@code true} if the format is supported
     */
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
        if (!supportsAlpha(format)) {
            image = removeAlpha(image);
        }
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
        if (!supportsAlpha(format)) {
            image = removeAlpha(image);
        }
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
                if (ios == null) {
                    throw new ImageEditorException(
                            "Could not create image output stream for target: " + target.getClass().getSimpleName());
                }
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }
    }

    private static boolean supportsAlpha(ImageFormat format) {
        return format == ImageFormat.PNG || format == ImageFormat.TIFF;
    }

    private static BufferedImage removeAlpha(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return image;
        }
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgb;
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
            ArrayList<String> command = new ArrayList<>();
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

    /**
     * Returns the {@link ImageFormat} for the given file name based on its
     * extension.
     *
     * @param fileName a file name (e.g. {@code "photo.png"})
     * @return the corresponding format
     * @throws ImageEditorException if the extension is missing or unsupported
     */
    public static ImageFormat getFormat(String fileName) {
        return FormatDetector.getFormat(fileName);
    }

    /**
     * Extracts the lower-case file extension from a file name.
     *
     * @param fileName a file name (e.g. {@code "photo.PNG"})
     * @return the extension in lower case (e.g. {@code "png"})
     * @throws ImageEditorException if the file name has no extension
     */
    public static String getExtension(String fileName) {
        return FormatDetector.getExtension(fileName);
    }
}
