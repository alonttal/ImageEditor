# ImageEditor

A lightweight Java library for image manipulation. Supports **resize**, **fit**, **cover**, **crop**, and **scaleDown** operations with a fluent builder API, plus quality control, metadata stripping, format conversion, stream I/O, and batch processing.

## Supported Formats

| Format | Requirements |
|---|---|
| PNG, JPG, GIF, BMP, TIFF | None — built-in via Java `ImageIO` |
| WebP | `cwebp` and `dwebp` CLI tools must be installed |
| AVIF | `heif-enc` and `heif-dec` CLI tools must be installed |

Standard formats work out of the box. WebP and AVIF support is optional — install the relevant CLI tools only for the formats you need.

## Installing CLI Tools

### Linux (Fedora / RHEL / CentOS)

```bash
# WebP support
sudo dnf install libwebp-tools

# AVIF support
sudo dnf install libheif-tools
```

### Linux (Ubuntu / Debian)

```bash
# WebP support
sudo apt install webp

# AVIF support
sudo apt install libheif-examples
```

### macOS (Homebrew)

```bash
# WebP support
brew install webp

# AVIF support
brew install libheif
```

### Windows

1. **WebP** — Download the prebuilt `libwebp` binaries from https://developers.google.com/speed/webp/download. Extract the archive and add the `bin` folder (containing `cwebp.exe` and `dwebp.exe`) to your `PATH`.

2. **AVIF** — Download `libheif` from https://github.com/strukturag/libheif/releases or build from source. Add the folder containing `heif-enc.exe` and `heif-dec.exe` to your `PATH`.

After installation, verify the tools are accessible from your terminal:

```bash
cwebp -version
dwebp -version
heif-enc --version
heif-dec --version
```

## Validating Your Setup

Use `ImageIOHandler.isFormatSupported()` to check at runtime whether a format is ready to use. This checks that the required CLI tools are installed and available on the system `PATH`.

```java
import com.imageeditor.io.ImageFormat;
import com.imageeditor.io.ImageIOHandler;

// Standard formats — always true
ImageIOHandler.isFormatSupported(ImageFormat.PNG);   // true
ImageIOHandler.isFormatSupported(ImageFormat.JPEG);  // true

// CLI-dependent formats — true only if tools are installed
ImageIOHandler.isFormatSupported(ImageFormat.WEBP);  // true if cwebp & dwebp are on PATH
ImageIOHandler.isFormatSupported(ImageFormat.AVIF);  // true if heif-enc & heif-dec are on PATH
```

You can use this to fail fast at application startup or to show users which formats are available:

```java
for (ImageFormat fmt : ImageFormat.values()) {
    if (!ImageIOHandler.isFormatSupported(fmt)) {
        System.err.println("WARNING: " + fmt + " is not supported. Install the required CLI tools.");
    }
}
```

## Usage

```java
import com.imageeditor.ImageEditor;
import com.imageeditor.io.ImageFormat;
import java.nio.file.Path;

// Resize an image to exact dimensions
ImageEditor.builder()
    .resize(800, 600)
    .build()
    .process(Path.of("input.webp"), Path.of("output.webp"));

// Fit within a bounding box (preserves aspect ratio)
ImageEditor.builder()
    .fit(800, 600)
    .build()
    .process(Path.of("photo.png"), Path.of("fitted.png"));

// Cover (scale + center-crop to fill exact dimensions)
ImageEditor.builder()
    .cover(400, 400)
    .build()
    .process(Path.of("photo.png"), Path.of("thumbnail.png"));

// Crop a region
ImageEditor.builder()
    .crop(10, 10, 200, 200)
    .build()
    .process(Path.of("photo.avif"), Path.of("cropped.avif"));

// Scale down to fit within bounds (no-op if already smaller)
ImageEditor.builder()
    .scaleDown(800, 600)
    .build()
    .process(Path.of("photo.png"), Path.of("scaled.png"));

// Chain multiple operations
ImageEditor.builder()
    .resize(1000, 750)
    .crop(100, 50, 800, 600)
    .build()
    .process(Path.of("input.jpg"), Path.of("output.jpg"));
```

### Quality, Metadata, and Format Conversion

```java
// Set JPEG/WebP compression quality (0.0–1.0) and strip metadata
ImageEditor.builder()
    .resize(1200, 800)
    .quality(0.85f)
    .stripMetadata()
    .build()
    .process(Path.of("photo.jpg"), Path.of("optimized.jpg"));

// Force output format regardless of file extension
ImageEditor.builder()
    .outputFormat(ImageFormat.WEBP)
    .build()
    .process(Path.of("photo.png"), Path.of("photo.webp"));
```

### Stream I/O

```java
import java.io.InputStream;
import java.io.OutputStream;

// Process from streams (format auto-detected from magic bytes)
try (InputStream in = ...; OutputStream out = ...) {
    ImageEditor.builder()
        .fit(800, 600)
        .build()
        .process(in, out);
}
```

### Batch Processing

```java
// Process all images in a directory (single-threaded)
ImageEditor.builder()
    .resize(800, 600)
    .build()
    .processDirectory(Path.of("input-dir"), Path.of("output-dir"));

// Process with 4 threads
ImageEditor.builder()
    .resize(800, 600)
    .build()
    .processDirectory(Path.of("input-dir"), Path.of("output-dir"), 4);
```

The `ImageEditor` instance is reusable — you can call `process()` on multiple files with the same operation pipeline.

### Custom Tool Directory

By default, CLI tools are resolved from the system `PATH`. To use tools from a specific directory:

```java
import com.imageeditor.io.ImageIOHandler;

ImageIOHandler.setToolDirectory(Path.of("/opt/image-tools/bin"));
```

## Building

Requires Java 25+ and Maven.

```bash
mvn clean package
```

## Running Tests

```bash
mvn clean test
```

Tests for WebP and AVIF are automatically skipped if the CLI tools are not installed. All other tests run regardless.
