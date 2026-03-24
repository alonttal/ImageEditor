package io.github.alonttal.imageeditor.io;

import io.github.alonttal.imageeditor.exception.ImageEditorException;

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
        ImageFormat format = FormatDetector.detectFormat(path);
        if (format == null) {
            throw new ImageEditorException(
                    "Unrecognized image format: " + path.getFileName()
                    + " (not a known image type and no supported file extension)");
        }

        try {
            if (format.isStandard()) {
                return readStandard(path);
            } else if (format == ImageFormat.WEBP) {
                return readViaCliToPng(path, CliToolRunner.resolveToolPath("dwebp"), path.toAbsolutePath().toString(), "-o");
            } else if (format == ImageFormat.AVIF) {
                String decoder = CliToolRunner.resolveHeifDecoder();
                if (decoder == null) {
                    throw new ImageEditorException("No HEIF decoder found (install heif-dec or heif-convert)");
                }
                return readViaCliToPng(path, CliToolRunner.resolveToolPath(decoder), path.toAbsolutePath().toString());
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
                writeAvif(image, path, options);
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
        if (!image.getColorModel().hasAlpha() && image.getType() != BufferedImage.TYPE_CUSTOM) {
            return image;
        }
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
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

    private static void writeAvif(BufferedImage image, Path outputPath, OutputOptions options) throws IOException {
        Path tmpPng = Files.createTempFile("imageeditor-", ".png");
        try {
            // Write as 16-bit PNG so heif-enc can produce 10-bit AVIF output.
            // 10-bit encoding improves quantization precision, reducing banding
            // and often producing smaller files than 8-bit at the same quality.
            write16BitPng(image, tmpPng);

            ArrayList<String> command = new ArrayList<>();
            command.add(CliToolRunner.resolveToolPath("heif-enc"));
            command.add("-A");
            if (options.quality() != null) {
                int qValue = Math.round(options.quality() * 100);
                command.add("-q");
                command.add(String.valueOf(qValue));
            }
            // 10-bit depth for smoother gradients and better compression
            command.add("-b");
            command.add("10");
            // Full chroma (4:4:4) to avoid color smearing on edges
            command.add("-p");
            command.add("chroma=444");
            command.add(tmpPng.toAbsolutePath().toString());
            command.add("-o");
            command.add(outputPath.toAbsolutePath().toString());

            CliToolRunner.runProcess(command.toArray(new String[0]));
        } finally {
            Files.deleteIfExists(tmpPng);
        }
    }

    /**
     * Writes the image as a 16-bit PNG, streaming row by row to avoid
     * allocating a full 16-bit BufferedImage in memory. Only one scanline
     * buffer is held at a time, so memory overhead is O(width) instead of
     * O(width * height).
     */
    private static void write16BitPng(BufferedImage image, Path path) throws IOException {
        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int channels = hasAlpha ? 4 : 3;
        int colorType = hasAlpha ? 6 : 2; // 6 = RGBA, 2 = RGB

        try (OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(path))) {
            // PNG signature
            fileOut.write(new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});

            // IHDR chunk
            byte[] ihdr = new byte[13];
            writeInt(ihdr, 0, w);
            writeInt(ihdr, 4, h);
            ihdr[8] = 16; // bit depth
            ihdr[9] = (byte) colorType;
            ihdr[10] = 0; // compression method
            ihdr[11] = 0; // filter method
            ihdr[12] = 0; // interlace method
            writeChunk(fileOut, "IHDR", ihdr);

            // IDAT chunk(s) — compressed scanlines
            // Filter byte (0 = None) + 2 bytes per channel per pixel
            int scanlineBytes = 1 + w * channels * 2;
            byte[] scanline = new byte[scanlineBytes];
            // Batch row of ARGB pixels to avoid per-pixel getRGB overhead
            int[] rowPixels = new int[w];

            java.util.zip.Deflater deflater = new java.util.zip.Deflater();
            try {
                ByteArrayOutputStream idatBuffer = new ByteArrayOutputStream();
                java.util.zip.DeflaterOutputStream deflaterOut =
                        new java.util.zip.DeflaterOutputStream(idatBuffer, deflater);

                for (int y = 0; y < h; y++) {
                    image.getRGB(0, y, w, 1, rowPixels, 0, w);
                    scanline[0] = 0; // filter: None
                    int pos = 1;
                    for (int x = 0; x < w; x++) {
                        int argb = rowPixels[x];
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;
                        // Scale 8-bit to 16-bit: v * 257 maps 0->0, 255->65535
                        writeShort(scanline, pos, r * 257);
                        pos += 2;
                        writeShort(scanline, pos, g * 257);
                        pos += 2;
                        writeShort(scanline, pos, b * 257);
                        pos += 2;
                        if (hasAlpha) {
                            int a = (argb >> 24) & 0xFF;
                            writeShort(scanline, pos, a * 257);
                            pos += 2;
                        }
                    }
                    deflaterOut.write(scanline);

                    // Flush IDAT chunks periodically to avoid large buffers
                    if (idatBuffer.size() > 65536) {
                        deflaterOut.flush();
                        writeChunk(fileOut, "IDAT", idatBuffer.toByteArray());
                        idatBuffer.reset();
                    }
                }

                deflaterOut.finish();
                if (idatBuffer.size() > 0) {
                    writeChunk(fileOut, "IDAT", idatBuffer.toByteArray());
                }
            } finally {
                deflater.end();
            }

            // IEND chunk
            writeChunk(fileOut, "IEND", new byte[0]);
        }
    }

    private static void writeChunk(OutputStream out, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] lengthBytes = new byte[4];
        writeInt(lengthBytes, 0, data.length);
        out.write(lengthBytes);
        out.write(typeBytes);
        out.write(data);

        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(typeBytes);
        crc.update(data);
        byte[] crcBytes = new byte[4];
        writeInt(crcBytes, 0, (int) crc.getValue());
        out.write(crcBytes);
    }

    private static void writeInt(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value >> 24);
        buf[offset + 1] = (byte) (value >> 16);
        buf[offset + 2] = (byte) (value >> 8);
        buf[offset + 3] = (byte) value;
    }

    private static void writeShort(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value >> 8);
        buf[offset + 1] = (byte) value;
    }

    private static void writeViaCliFromPng(BufferedImage image, Path outputPath, OutputOptions options,
                                           String... commandParts) throws IOException {
        Path tmpPng = Files.createTempFile("imageeditor-", ".png");
        try {
            if (!ImageIO.write(image, "png", tmpPng.toFile())) {
                throw new ImageEditorException("No writer found for PNG format");
            }
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
