package com.imageeditor.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutputOptionsTest {

    @Test
    void defaultsHaveNoQualityAndNoStrip() {
        OutputOptions opts = OutputOptions.defaults();
        assertNull(opts.getQuality());
        assertFalse(opts.isStripMetadata());
    }

    @Test
    void qualityBoundary0() {
        OutputOptions opts = OutputOptions.builder().quality(0.0f).build();
        assertEquals(0.0f, opts.getQuality());
    }

    @Test
    void qualityBoundary1() {
        OutputOptions opts = OutputOptions.builder().quality(1.0f).build();
        assertEquals(1.0f, opts.getQuality());
    }

    @Test
    void qualityOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> OutputOptions.builder().quality(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> OutputOptions.builder().quality(1.1f));
    }

    @Test
    void outputFormatRoundTrip() {
        OutputOptions opts = OutputOptions.builder().outputFormat(ImageFormat.WEBP).build();
        assertEquals(ImageFormat.WEBP, opts.getOutputFormat());
    }

    @Test
    void defaultOutputFormatIsNull() {
        OutputOptions opts = OutputOptions.defaults();
        assertNull(opts.getOutputFormat());

        OutputOptions built = OutputOptions.builder().build();
        assertNull(built.getOutputFormat());
    }

    @Test
    void stripMetadataTrueRoundTrip() {
        OutputOptions opts = OutputOptions.builder().stripMetadata(true).build();
        assertTrue(opts.isStripMetadata());
    }

    @Test
    void outputFormatNullExplicit() {
        OutputOptions opts = OutputOptions.builder().outputFormat(null).build();
        assertNull(opts.getOutputFormat());
    }

    @Test
    void allSevenFormatsRoundTrip() {
        for (ImageFormat format : ImageFormat.values()) {
            OutputOptions opts = OutputOptions.builder().outputFormat(format).build();
            assertEquals(format, opts.getOutputFormat());
        }
    }

    @Test
    void fullCombination() {
        OutputOptions opts = OutputOptions.builder()
                .quality(0.8f)
                .stripMetadata(true)
                .outputFormat(ImageFormat.JPEG)
                .build();

        assertEquals(0.8f, opts.getQuality());
        assertTrue(opts.isStripMetadata());
        assertEquals(ImageFormat.JPEG, opts.getOutputFormat());
    }

    @Test
    void builderIsReusable() {
        OutputOptions.Builder builder = OutputOptions.builder().quality(0.5f).stripMetadata(true);
        OutputOptions a = builder.build();
        OutputOptions b = builder.build();

        assertNotSame(a, b);
        assertEquals(a.getQuality(), b.getQuality());
        assertEquals(a.isStripMetadata(), b.isStripMetadata());
    }
}
