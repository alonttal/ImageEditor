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
import java.util.concurrent.TimeUnit;

public class ImageEditor {

    private final List<Operation> operations;
    private final OutputOptions outputOptions;

    private ImageEditor(List<Operation> operations, OutputOptions outputOptions) {
        this.operations = Collections.unmodifiableList(operations);
        this.outputOptions = outputOptions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void process(Path inputPath, Path outputPath) {
        BufferedImage image = ImageIOHandler.read(inputPath);

        for (Operation op : operations) {
            image = op.apply(image);
        }

        ImageFormat format = outputOptions.getOutputFormat();
        if (format == null) {
            format = ImageIOHandler.getFormat(inputPath.getFileName().toString());
        }
        ImageIOHandler.write(image, outputPath, format, outputOptions);
    }

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

        ImageFormat format = outputOptions.getOutputFormat();
        if (format == null) {
            format = detectedFormat;
        }
        ImageIOHandler.write(image, output, format, outputOptions);
    }

    public void processDirectory(Path inputDir, Path outputDir) {
        processDirectory(inputDir, outputDir, 1);
    }

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
            ExecutorService pool = Executors.newFixedThreadPool(parallelism);
            List<ImageEditorException> errors = Collections.synchronizedList(new ArrayList<>());
            for (Path f : imageFiles) {
                pool.submit(() -> {
                    try {
                        processOne(f, outputDir);
                    } catch (ImageEditorException e) {
                        errors.add(e);
                    }
                });
            }
            pool.shutdown();
            try {
                pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ImageEditorException("Batch processing interrupted", e);
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
        ImageFormat format = outputOptions.getOutputFormat();
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

    public static class Builder {
        private final List<Operation> operations = new ArrayList<>();
        private Float quality;
        private boolean stripMetadata;
        private ImageFormat outputFormat;

        public Builder resize(int width, int height) {
            operations.add(new ResizeOperation(width, height));
            return this;
        }

        public Builder crop(int x, int y, int width, int height) {
            operations.add(new CropOperation(x, y, width, height));
            return this;
        }

        public Builder cover(int width, int height) {
            operations.add(new CoverOperation(width, height));
            return this;
        }

        public Builder fit(int maxWidth, int maxHeight) {
            operations.add(new FitOperation(maxWidth, maxHeight));
            return this;
        }

        public Builder quality(float q) {
            if (Float.isNaN(q) || q < 0.0f || q > 1.0f) {
                throw new IllegalArgumentException("Quality must be between 0.0 and 1.0, got: " + q);
            }
            this.quality = q;
            return this;
        }

        public Builder stripMetadata() {
            this.stripMetadata = true;
            return this;
        }

        public Builder outputFormat(ImageFormat format) {
            this.outputFormat = format;
            return this;
        }

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
