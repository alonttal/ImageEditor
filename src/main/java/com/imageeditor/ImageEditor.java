package com.imageeditor;

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

    public void process(File inputFile, File outputFile) {
        BufferedImage image = ImageIOHandler.read(inputFile);

        for (Operation op : operations) {
            image = op.apply(image);
        }

        ImageIOHandler.write(image, outputFile, outputOptions);
    }

    public void process(InputStream input, OutputStream output, String outputFormat) {
        BufferedInputStream buffered = input instanceof BufferedInputStream
                ? (BufferedInputStream) input
                : new BufferedInputStream(input);

        String detectedFormat = ImageIOHandler.detectFormat(buffered);
        if (detectedFormat == null) {
            throw new ImageEditorException("Could not detect input image format from stream");
        }

        BufferedImage image = ImageIOHandler.read(buffered, detectedFormat);

        for (Operation op : operations) {
            image = op.apply(image);
        }

        ImageIOHandler.write(image, output, outputFormat, outputOptions);
    }

    public void processDirectory(File inputDir, File outputDir) {
        processDirectory(inputDir, outputDir, 1);
    }

    public void processDirectory(File inputDir, File outputDir, int parallelism) {
        if (!inputDir.isDirectory()) {
            throw new ImageEditorException("Input path is not a directory: " + inputDir);
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new ImageEditorException("Could not create output directory: " + outputDir);
        }

        File[] files = inputDir.listFiles();
        if (files == null) {
            return;
        }

        List<File> imageFiles = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && isSupportedImage(f)) {
                imageFiles.add(f);
            }
        }

        if (parallelism <= 1) {
            for (File f : imageFiles) {
                processOne(f, outputDir);
            }
        } else {
            ExecutorService pool = Executors.newFixedThreadPool(parallelism);
            List<ImageEditorException> errors = Collections.synchronizedList(new ArrayList<>());
            for (File f : imageFiles) {
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

    private void processOne(File inputFile, File outputDir) {
        File outputFile = new File(outputDir, inputFile.getName());
        process(inputFile, outputFile);
    }

    private boolean isSupportedImage(File file) {
        try {
            String ext = ImageIOHandler.getExtension(file.getName());
            return ImageIOHandler.isFormatSupported(ext);
        } catch (ImageEditorException e) {
            return false;
        }
    }

    public static class Builder {
        private final List<Operation> operations = new ArrayList<>();
        private Float quality;
        private boolean stripMetadata;

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
            if (q < 0.0f || q > 1.0f) {
                throw new IllegalArgumentException("Quality must be between 0.0 and 1.0, got: " + q);
            }
            this.quality = q;
            return this;
        }

        public Builder stripMetadata() {
            this.stripMetadata = true;
            return this;
        }

        public ImageEditor build() {
            OutputOptions.Builder optBuilder = OutputOptions.builder();
            if (quality != null) {
                optBuilder.quality(quality);
            }
            optBuilder.stripMetadata(stripMetadata);
            OutputOptions opts = optBuilder.build();
            return new ImageEditor(new ArrayList<>(operations), opts);
        }
    }
}
