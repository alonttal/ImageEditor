# ImageEditor

A lightweight Java library for image manipulation. Supports **resize**, **crop**, and **cover** operations with a fluent builder API.

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

2. **AVIF** — Download `libheif` from https://github.com/nicktheriot/libheif-windows/releases or build from source. Add the folder containing `heif-enc.exe` and `heif-dec.exe` to your `PATH`.

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
import com.imageeditor.io.ImageIOHandler;

// Standard formats — always true
ImageIOHandler.isFormatSupported("png");   // true
ImageIOHandler.isFormatSupported("jpg");   // true

// CLI-dependent formats — true only if tools are installed
ImageIOHandler.isFormatSupported("webp");  // true if cwebp & dwebp are on PATH
ImageIOHandler.isFormatSupported("avif");  // true if heif-enc & heif-dec are on PATH

// Unknown formats
ImageIOHandler.isFormatSupported("xyz");   // false
```

You can use this to fail fast at application startup or to show users which formats are available:

```java
String[] formatsNeeded = {"png", "webp", "avif"};
for (String fmt : formatsNeeded) {
    if (!ImageIOHandler.isFormatSupported(fmt)) {
        System.err.println("WARNING: " + fmt + " is not supported. Install the required CLI tools.");
    }
}
```

## Usage

```java
import com.imageeditor.ImageEditor;
import java.io.File;

// Resize an image
ImageEditor.builder()
    .resize(800, 600)
    .build()
    .process(new File("input.webp"), new File("output.webp"));

// Crop a region
ImageEditor.builder()
    .crop(10, 10, 200, 200)
    .build()
    .process(new File("photo.avif"), new File("cropped.avif"));

// Cover (scale + crop to fill exact dimensions)
ImageEditor.builder()
    .cover(400, 400)
    .build()
    .process(new File("photo.png"), new File("thumbnail.png"));

// Chain multiple operations
ImageEditor.builder()
    .resize(1000, 750)
    .crop(100, 50, 800, 600)
    .build()
    .process(new File("input.jpg"), new File("output.jpg"));

// Convert between formats (input and output extensions determine the format)
ImageEditor.builder()
    .resize(500, 500)
    .build()
    .process(new File("photo.webp"), new File("photo.png"));
```

The `ImageEditor` instance is reusable — you can call `process()` on multiple files with the same operation pipeline.

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
