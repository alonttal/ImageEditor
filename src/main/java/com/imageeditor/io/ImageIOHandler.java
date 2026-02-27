package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ImageIOHandler {

    private static final Set<String> STANDARD_FORMATS = Set.of("png", "jpg", "jpeg", "gif", "bmp", "tiff");
    private static final Map<String, List<String>> CLI_TOOLS = Map.of(
            "webp", List.of("cwebp", "dwebp"),
            "avif", List.of("heif-enc", "heif-dec")
    );
    private static final long TIMEOUT_SECONDS = 30;

    public static BufferedImage read(File file) {
        String ext = getExtension(file.getName());

        try {
            if (STANDARD_FORMATS.contains(ext)) {
                return readStandard(file);
            } else if ("webp".equals(ext)) {
                return readViaCliToPng(file, "dwebp", file.getAbsolutePath(), "-o");
            } else if ("avif".equals(ext)) {
                return readViaCliToPng(file, "heif-dec", file.getAbsolutePath());
            } else {
                throw new ImageEditorException("Unsupported image format: " + ext);
            }
        } catch (IOException e) {
            throw new ImageEditorException("Failed to read image: " + file, e);
        }
    }

    public static void write(BufferedImage image, File file) {
        String ext = getExtension(file.getName());

        try {
            if (STANDARD_FORMATS.contains(ext)) {
                writeStandard(image, ext, file);
            } else if ("webp".equals(ext)) {
                writeViaCliFromPng(image, file, "cwebp", null, "-o", file.getAbsolutePath());
            } else if ("avif".equals(ext)) {
                writeViaCliFromPng(image, file, "heif-enc", null, "-o", file.getAbsolutePath());
            } else {
                throw new ImageEditorException("Unsupported image format: " + ext);
            }
        } catch (IOException e) {
            throw new ImageEditorException("Failed to write image: " + file, e);
        }
    }

    public static boolean isFormatSupported(String format) {
        String ext = format.toLowerCase();
        if (STANDARD_FORMATS.contains(ext)) {
            return true;
        }
        List<String> tools = CLI_TOOLS.get(ext);
        if (tools == null) {
            return false;
        }
        return tools.stream().allMatch(ImageIOHandler::isToolAvailable);
    }

    private static boolean isToolAvailable(String tool) {
        try {
            String lookupCommand = System.getProperty("os.name", "")
                    .toLowerCase().contains("win") ? "where" : "which";
            Process p = new ProcessBuilder(lookupCommand, tool)
                    .redirectErrorStream(true).start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static BufferedImage readStandard(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new ImageEditorException("Could not read image: " + file);
        }
        return image;
    }

    private static void writeStandard(BufferedImage image, String format, File file) throws IOException {
        String writeFormat = "jpg".equals(format) ? "jpeg" : format;
        boolean written = ImageIO.write(image, writeFormat, file);
        if (!written) {
            throw new ImageEditorException("No writer found for format: " + format);
        }
    }

    private static BufferedImage readViaCliToPng(File inputFile, String... commandParts) throws IOException {
        File tmpPng = File.createTempFile("imageeditor-", ".png");
        try {
            String[] command = appendArg(commandParts, tmpPng.getAbsolutePath());
            runProcess(command);
            return readStandard(tmpPng);
        } finally {
            tmpPng.delete();
        }
    }

    private static void writeViaCliFromPng(BufferedImage image, File outputFile, String... commandParts)
            throws IOException {
        File tmpPng = File.createTempFile("imageeditor-", ".png");
        try {
            ImageIO.write(image, "png", tmpPng);
            // Replace null placeholder with the tmp png path
            String[] command = new String[commandParts.length];
            for (int i = 0; i < commandParts.length; i++) {
                command[i] = commandParts[i] == null ? tmpPng.getAbsolutePath() : commandParts[i];
            }
            runProcess(command);
        } finally {
            tmpPng.delete();
        }
    }

    private static void runProcess(String[] command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            byte[] output = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new ImageEditorException(
                        "CLI tool timed out after " + TIMEOUT_SECONDS + "s: " + command[0]);
            }

            if (process.exitValue() != 0) {
                String errorOutput = new String(output).trim();
                throw new ImageEditorException(
                        "CLI tool failed (exit " + process.exitValue() + "): " + command[0]
                                + (errorOutput.isEmpty() ? "" : "\n" + errorOutput));
            }
        } catch (IOException e) {
            throw new ImageEditorException("CLI tool not found or failed to execute: " + command[0], e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImageEditorException("Interrupted while running CLI tool: " + command[0], e);
        }
    }

    private static String[] appendArg(String[] parts, String arg) {
        String[] result = new String[parts.length + 1];
        System.arraycopy(parts, 0, result, 0, parts.length);
        result[parts.length] = arg;
        return result;
    }

    static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            throw new ImageEditorException("Cannot determine format from file name: " + fileName);
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}
