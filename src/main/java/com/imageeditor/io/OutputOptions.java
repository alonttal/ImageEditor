package com.imageeditor.io;

/**
 * Options that control how a processed image is written.
 *
 * @param quality       JPEG/WebP compression quality between 0.0 and 1.0,
 *                      or {@code null} to use the encoder default
 * @param stripMetadata {@code true} to omit image metadata from the output
 * @param outputFormat  explicit output format, or {@code null} to infer
 *                      from the file extension / input format
 */
public record OutputOptions(Float quality, boolean stripMetadata, ImageFormat outputFormat) {

    /**
     * Returns options with all defaults (no quality override, metadata kept,
     * format inferred).
     *
     * @return default output options
     */
    public static OutputOptions defaults() {
        return new OutputOptions(null, false, null);
    }

    /**
     * Returns a new {@link Builder} for constructing {@code OutputOptions}.
     *
     * @return a fresh builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Mutable builder for {@link OutputOptions}.
     */
    public static class Builder {
        private Float quality;
        private boolean stripMetadata;
        private ImageFormat outputFormat;

        /**
         * Sets the compression quality.
         *
         * @param quality value between 0.0 (lowest) and 1.0 (highest)
         * @return this builder
         * @throws IllegalArgumentException if {@code quality} is outside
         *         [0.0, 1.0] or NaN
         */
        public Builder quality(float quality) {
            if (Float.isNaN(quality) || quality < 0.0f || quality > 1.0f) {
                throw new IllegalArgumentException("Quality must be between 0.0 and 1.0, got: " + quality);
            }
            this.quality = quality;
            return this;
        }

        /**
         * Sets whether image metadata should be stripped from the output.
         *
         * @param strip {@code true} to strip metadata
         * @return this builder
         */
        public Builder stripMetadata(boolean strip) {
            this.stripMetadata = strip;
            return this;
        }

        /**
         * Sets the explicit output format.
         *
         * @param format the desired format, or {@code null} to infer from
         *               the file extension / input format
         * @return this builder
         */
        public Builder outputFormat(ImageFormat format) {
            this.outputFormat = format;
            return this;
        }

        /**
         * Builds an immutable {@link OutputOptions} from the current settings.
         *
         * @return a new {@code OutputOptions} instance
         */
        public OutputOptions build() {
            return new OutputOptions(quality, stripMetadata, outputFormat);
        }
    }
}
