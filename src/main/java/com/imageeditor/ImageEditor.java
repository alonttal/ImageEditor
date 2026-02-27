package com.imageeditor;

import com.imageeditor.io.ImageFormat;
import com.imageeditor.io.ImageIOHandler;
import com.imageeditor.io.OutputOptions;
import com.imageeditor.exception.ImageEditorException;
import com.imageeditor.operation.CoverOperation;
import com.imageeditor.operation.CropOperation;
import com.imageeditor.operation.FitOperation;
import com.imageeditor.operation.Operation;
import com.imageeditor.operation.ResizeOperation;
import com.imageeditor.operation.ScaleDownOperation;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fluent image-processing pipeline that reads an image, applies a sequence of
 * {@link Operation}s, and writes the result.
 *
 * <p>Create instances via the {@link #builder()} method:
 * <pre>{@code
 * ImageEditor editor = ImageEditor.builder()
 *         .resize(800, 600)
 *         .quality(0.85f)
 *         .build();
 * editor.process(inputPath, outputPath);
 * }</pre>
 */
public class ImageEditor {

    private final List<Operation> operations;
    private final OutputOptions outputOptions;

    private ImageEditor(List<Operation> operations, OutputOptions outputOptions) {
        this.operations = Collections.unmodifiableList(operations);
        this.outputOptions = outputOptions;
    }

    /**
     * Returns a new {@link Builder} for configuring an {@code ImageEditor}.
     *
     * @return a fresh builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reads the image at {@code inputPath}, applies all configured operations,
     * and writes the result to {@code outputPath}.
     *
     * <p>The output format is determined by the builder's
     * {@link Builder#outputFormat(ImageFormat)} setting, falling back to the
     * input file's extension.
     *
     * @param inputPath  path to the source image
     * @param outputPath path where the processed image will be written
     * @throws ImageEditorException if reading, processing, or writing fails
     */
    public void process(Path inputPath, Path outputPath) {
        BufferedImage image = ImageIOHandler.read(inputPath);

        for (Operation op : operations) {
            image = op.apply(image);
        }

        ImageFormat format = outputOptions.outputFormat();
        if (format == null) {
            format = ImageIOHandler.getFormat(inputPath.getFileName().toString());
        }
        ImageIOHandler.write(image, outputPath, format, outputOptions);
    }

    /**
     * Reads an image from the given stream, applies all configured operations,
     * and writes the result to the output stream.
     *
     * <p>The input format is auto-detected from magic bytes. The output format
     * defaults to the detected input format unless overridden via
     * {@link Builder#outputFormat(ImageFormat)}.
     *
     * @param input  stream containing the source image data
     * @param output stream where the processed image will be written
     * @throws ImageEditorException if the format cannot be detected or
     *         processing fails
     */
    public void process(InputStream input, OutputStream output) {
        BufferedInputStream buffered = input instanceof BufferedInputStream
                ? (BufferedInputStream) input
                : new BufferedInputStream(input);

        ImageFormat detectedFormat = ImageIOHandler.detectFormat(buffered);
        if (detectedFormat == null) {
            throw new ImageEditorException("Could not detect input image format from stream");
        }

        BufferedImage image = ImageIOHandler.read(buffered, detectedFormat);

        for (Operation op : operations) {
            image = op.apply(image);
        }

        ImageFormat format = outputOptions.outputFormat();
        if (format == null) {
            format = detectedFormat;
        }
        ImageIOHandler.write(image, output, format, outputOptions);
    }

    /**
     * Processes every supported image in {@code inputDir} and writes results to
     * {@code outputDir}, using a single thread.
     *
     * @param inputDir  directory containing source images
     * @param outputDir directory where processed images will be written
     *                  (created if it does not exist)
     * @throws ImageEditorException if the input path is not a directory or
     *         any image fails to process
     * @see #processDirectory(Path, Path, int)
     */
    public void processDirectory(Path inputDir, Path outputDir) {
        processDirectory(inputDir, outputDir, 1);
    }

    /**
     * Processes every supported image in {@code inputDir} and writes results to
     * {@code outputDir}, using up to {@code parallelism} threads.
     *
     * @param inputDir    directory containing source images
     * @param outputDir   directory where processed images will be written
     *                    (created if it does not exist)
     * @param parallelism maximum number of concurrent processing threads;
     *                    values &le; 1 result in single-threaded processing
     * @throws ImageEditorException if the input path is not a directory or
     *         any image fails to process
     */
    public void processDirectory(Path inputDir, Path outputDir, int parallelism) {
        if (!Files.isDirectory(inputDir)) {
            throw new ImageEditorException("Input path is not a directory: " + inputDir);
        }
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new ImageEditorException("Could not create output directory: " + outputDir, e);
        }

        List<Path> imageFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir)) {
            for (Path f : stream) {
                if (Files.isRegularFile(f) && isSupportedImage(f)) {
                    imageFiles.add(f);
                }
            }
        } catch (IOException e) {
            throw new ImageEditorException("Failed to list directory: " + inputDir, e);
        }

        if (parallelism <= 1) {
            for (Path f : imageFiles) {
                processOne(f, outputDir);
            }
        } else {
            List<ImageEditorException> errors = Collections.synchronizedList(new ArrayList<>());
            try (ExecutorService pool = Executors.newFixedThreadPool(parallelism)) {
                for (Path f : imageFiles) {
                    pool.submit(() -> {
                        try {
                            processOne(f, outputDir);
                        } catch (ImageEditorException e) {
                            errors.add(e);
                        }
                    });
                }
            }
            if (!errors.isEmpty()) {
                throw errors.get(0);
            }
        }
    }

    private void processOne(Path inputFile, Path outputDir) {
        Path outputFile = resolveOutputFile(inputFile, outputDir);
        process(inputFile, outputFile);
    }

    private Path resolveOutputFile(Path inputFile, Path outputDir) {
        String name = inputFile.getFileName().toString();
        ImageFormat format = outputOptions.outputFormat();
        if (format != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                name = name.substring(0, dot);
            }
            name = name + "." + format.getExtension();
        }
        return outputDir.resolve(name);
    }

    private boolean isSupportedImage(Path path) {
        try {
            ImageFormat format = ImageIOHandler.getFormat(path.getFileName().toString());
            return ImageIOHandler.isFormatSupported(format);
        } catch (ImageEditorException e) {
            return false;
        }
    }

    /**
     * Mutable builder for configuring and constructing an {@link ImageEditor}.
     */
    public static class Builder {
        private final List<Operation> operations = new ArrayList<>();
        private Float quality;
        private boolean stripMetadata;
        private ImageFormat outputFormat;

        /**
         * Appends an exact-resize operation to the pipeline.
         *
         * @param width  target width in pixels
         * @param height target height in pixels
         * @return this builder
         */
        public Builder resize(int width, int height) {
            operations.add(new ResizeOperation(width, height));
            return this;
        }

        /**
         * Appends a crop operation to the pipeline.
         *
         * @param x      left edge of the crop region
         * @param y      top edge of the crop region
         * @param width  width of the crop region in pixels
         * @param height height of the crop region in pixels
         * @return this builder
         */
        public Builder crop(int x, int y, int width, int height) {
            operations.add(new CropOperation(x, y, width, height));
            return this;
        }

        /**
         * Appends a cover (scale + center-crop) operation to the pipeline.
         *
         * @param width  target width in pixels
         * @param height target height in pixels
         * @return this builder
         */
        public Builder cover(int width, int height) {
            operations.add(new CoverOperation(width, height));
            return this;
        }

        /**
         * Appends a fit (scale-to-fit) operation to the pipeline.
         *
         * @param maxWidth  maximum width in pixels
         * @param maxHeight maximum height in pixels
         * @return this builder
         */
        public Builder fit(int maxWidth, int maxHeight) {
            operations.add(new FitOperation(maxWidth, maxHeight));
            return this;
        }

        /**
         * Appends a scale-down operation to the pipeline. Shrinks the image to
         * fit within the given bounds while preserving aspect ratio, but never
         * enlarges an image that already fits.
         *
         * @param maxWidth  maximum width in pixels
         * @param maxHeight maximum height in pixels
         * @return this builder
         */
        public Builder scaleDown(int maxWidth, int maxHeight) {
            operations.add(new ScaleDownOperation(maxWidth, maxHeight));
            return this;
        }

        /**
         * Sets the compression quality for formats that support it (e.g. JPEG, WebP).
         *
         * @param q quality value between 0.0 (lowest) and 1.0 (highest)
         * @return this builder
         * @throws IllegalArgumentException if {@code q} is outside [0.0, 1.0] or NaN
         */
        public Builder quality(float q) {
            if (Float.isNaN(q) || q < 0.0f || q > 1.0f) {
                throw new IllegalArgumentException("Quality must be between 0.0 and 1.0, got: " + q);
            }
            this.quality = q;
            return this;
        }

        /**
         * Instructs the encoder to strip image metadata from the output.
         *
         * @return this builder
         */
        public Builder stripMetadata() {
            this.stripMetadata = true;
            return this;
        }

        /**
         * Forces the output format, overriding any format inferred from the
         * file extension.
         *
         * @param format the desired output format, or {@code null} to infer
         * @return this builder
         */
        public Builder outputFormat(ImageFormat format) {
            this.outputFormat = format;
            return this;
        }

        /**
         * Builds an immutable {@link ImageEditor} with the configured
         * operations and output options.
         *
         * @return a new {@code ImageEditor} instance
         */
        public ImageEditor build() {
            OutputOptions.Builder optBuilder = OutputOptions.builder();
            if (quality != null) {
                optBuilder.quality(quality);
            }
            optBuilder.stripMetadata(stripMetadata);
            optBuilder.outputFormat(outputFormat);
            OutputOptions opts = optBuilder.build();
            return new ImageEditor(new ArrayList<>(operations), opts);
        }
    }
}
