package com.imageeditor.io;

public class OutputOptions {

    private final Float quality;
    private final boolean stripMetadata;

    private OutputOptions(Float quality, boolean stripMetadata) {
        this.quality = quality;
        this.stripMetadata = stripMetadata;
    }

    public static OutputOptions defaults() {
        return new OutputOptions(null, false);
    }

    public Float getQuality() {
        return quality;
    }

    public boolean isStripMetadata() {
        return stripMetadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Float quality;
        private boolean stripMetadata;

        public Builder quality(float quality) {
            if (quality < 0.0f || quality > 1.0f) {
                throw new IllegalArgumentException("Quality must be between 0.0 and 1.0, got: " + quality);
            }
            this.quality = quality;
            return this;
        }

        public Builder stripMetadata(boolean strip) {
            this.stripMetadata = strip;
            return this;
        }

        public OutputOptions build() {
            return new OutputOptions(quality, stripMetadata);
        }
    }
}
