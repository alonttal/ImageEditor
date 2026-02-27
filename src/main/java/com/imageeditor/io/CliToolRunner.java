package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages discovery and execution of external CLI tools used to read/write
 * non-standard image formats (e.g. WebP via {@code cwebp}/{@code dwebp},
 * AVIF via {@code heif-enc}/{@code heif-dec}).
 */
public class CliToolRunner {

    private static final Map<ImageFormat, List<String>> CLI_TOOLS = Map.of(
            ImageFormat.WEBP, List.of("cwebp", "dwebp"),
            ImageFormat.AVIF, List.of("heif-enc", "heif-dec")
    );
    private static final long TIMEOUT_SECONDS = 30;

    private static volatile Path toolDirectory;

    private CliToolRunner() {
    }

    /**
     * Sets the directory where external CLI tools are located.
     *
     * @param directory path to the tool directory, or {@code null} to resolve
     *                  tools from the system {@code PATH}
     */
    public static void setToolDirectory(Path directory) {
        toolDirectory = directory;
    }

    /**
     * Returns the currently configured CLI tool directory.
     *
     * @return the tool directory, or {@code null} if tools are resolved from
     *         the system {@code PATH}
     */
    public static Path getToolDirectory() {
        return toolDirectory;
    }

    static String resolveToolPath(String toolName) {
        Path dir = toolDirectory;
        if (dir != null) {
            return dir.resolve(toolName).toString();
        }
        return toolName;
    }

    /**
     * Checks whether the CLI tools required for the given non-standard format
     * are available on the current system.
     *
     * @param format the format to check
     * @return {@code true} if all required CLI tools for the format are found
     */
    public static boolean isCliFormatSupported(ImageFormat format) {
        List<String> tools = CLI_TOOLS.get(format);
        if (tools == null) {
            return false;
        }
        return tools.stream().allMatch(CliToolRunner::isToolAvailable);
    }

    static boolean isToolAvailable(String tool) {
        try {
            Path dir = toolDirectory;
            if (dir != null) {
                return Files.isExecutable(dir.resolve(tool));
            }
            String lookupCommand = System.getProperty("os.name", "")
                    .toLowerCase().contains("win") ? "where" : "which";
            Process p = new ProcessBuilder(lookupCommand, tool)
                    .redirectErrorStream(true).start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    static void runProcess(String[] command) {
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

    static String[] appendArg(String[] parts, String arg) {
        String[] result = new String[parts.length + 1];
        System.arraycopy(parts, 0, result, 0, parts.length);
        result[parts.length] = arg;
        return result;
    }
}
