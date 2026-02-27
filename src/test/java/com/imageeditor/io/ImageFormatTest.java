package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageFormatTest {

    @Test
    void fromExtensionCanonical() {
        assertEquals(ImageFormat.PNG, ImageFormat.fromExtension("png"));
        assertEquals(ImageFormat.JPEG, ImageFormat.fromExtension("jpeg"));
        assertEquals(ImageFormat.GIF, ImageFormat.fromExtension("gif"));
        assertEquals(ImageFormat.BMP, ImageFormat.fromExtension("bmp"));
        assertEquals(ImageFormat.TIFF, ImageFormat.fromExtension("tiff"));
        assertEquals(ImageFormat.WEBP, ImageFormat.fromExtension("webp"));
        assertEquals(ImageFormat.AVIF, ImageFormat.fromExtension("avif"));
    }

    @Test
    void fromExtensionAlias() {
        assertEquals(ImageFormat.JPEG, ImageFormat.fromExtension("jpg"));
    }

    @Test
    void fromExtensionCaseInsensitive() {
        assertEquals(ImageFormat.PNG, ImageFormat.fromExtension("PNG"));
        assertEquals(ImageFormat.JPEG, ImageFormat.fromExtension("JPG"));
        assertEquals(ImageFormat.JPEG, ImageFormat.fromExtension("Jpeg"));
        assertEquals(ImageFormat.WEBP, ImageFormat.fromExtension("WEBP"));
    }

    @Test
    void fromExtensionUnknownThrows() {
        assertThrows(ImageEditorException.class, () -> ImageFormat.fromExtension("xyz"));
        assertThrows(ImageEditorException.class, () -> ImageFormat.fromExtension(""));
    }

    @Test
    void fromExtensionNullThrows() {
        assertThrows(ImageEditorException.class, () -> ImageFormat.fromExtension(null));
    }

    @Test
    void getExtensionReturnsCanonical() {
        assertEquals("png", ImageFormat.PNG.getExtension());
        assertEquals("jpeg", ImageFormat.JPEG.getExtension());
        assertEquals("gif", ImageFormat.GIF.getExtension());
        assertEquals("bmp", ImageFormat.BMP.getExtension());
        assertEquals("tiff", ImageFormat.TIFF.getExtension());
        assertEquals("webp", ImageFormat.WEBP.getExtension());
        assertEquals("avif", ImageFormat.AVIF.getExtension());
    }

    @Test
    void isStandard() {
        assertTrue(ImageFormat.PNG.isStandard());
        assertTrue(ImageFormat.JPEG.isStandard());
        assertTrue(ImageFormat.GIF.isStandard());
        assertTrue(ImageFormat.BMP.isStandard());
        assertTrue(ImageFormat.TIFF.isStandard());
        assertFalse(ImageFormat.WEBP.isStandard());
        assertFalse(ImageFormat.AVIF.isStandard());
    }
}
