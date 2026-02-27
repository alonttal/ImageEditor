package com.imageeditor;

import com.imageeditor.io.ImageIOHandler;
import com.imageeditor.operation.CoverOperation;
import com.imageeditor.operation.CropOperation;
import com.imageeditor.operation.FitOperation;
import com.imageeditor.operation.Operation;
import com.imageeditor.operation.ResizeOperation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageEditor {

    private final List<Operation> operations;

    private ImageEditor(List<Operation> operations) {
        this.operations = Collections.unmodifiableList(operations);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void process(File inputFile, File outputFile) {
        BufferedImage image = ImageIOHandler.read(inputFile);

        for (Operation op : operations) {
            image = op.apply(image);
        }

        ImageIOHandler.write(image, outputFile);
    }

    public static class Builder {
        private final List<Operation> operations = new ArrayList<>();

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

        public ImageEditor build() {
            return new ImageEditor(new ArrayList<>(operations));
        }
    }
}
