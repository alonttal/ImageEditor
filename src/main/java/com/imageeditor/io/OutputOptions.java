package com.imageeditor.io;

public class OutputOptions {

    private final Float quality;
    private final boolean stripMetadata;
    private final String outputFormat;

    private OutputOptions(Float quality, boolean stripMetadata, String outputFormat) {
        this.quality = quality;
        this.stripMetadata = stripMetadata;
        this.outputFormat = outputFormat;
    }

    public static OutputOptions defaults() {
        return new OutputOptions(null, false, null);
    }

    public Float getQuality() {
        return quality;
    }

    public boolean isStripMetadata() {
        return stripMetadata;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Float quality;
        private boolean stripMetadata;
        private String outputFormat;

        public Builder quality(float quality) {
            if (Float.isNaN(quality) || quality < 0.0f || quality > 1.0f) {
                throw new IllegalArgumentException("Quality must be between 0.0 and 1.0, got: " + quality);
            }
            this.quality = quality;
            return this;
        }

        public Builder stripMetadata(boolean strip) {
            this.stripMetadata = strip;
            return this;
        }

        public Builder outputFormat(String format) {
            this.outputFormat = format != null ? format.toLowerCase() : null;
            return this;
        }

        public OutputOptions build() {
            return new OutputOptions(quality, stripMetadata, outputFormat);
        }
    }
}
