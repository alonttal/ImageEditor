package com.imageeditor.io;

import com.imageeditor.exception.ImageEditorException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CliToolRunner {

    private static final Map<ImageFormat, List<String>> CLI_TOOLS = Map.of(
            ImageFormat.WEBP, List.of("cwebp", "dwebp"),
            ImageFormat.AVIF, List.of("heif-enc", "heif-dec")
    );
    private static final long TIMEOUT_SECONDS = 30;

    private static volatile Path toolDirectory;

    private CliToolRunner() {
    }

    public static void setToolDirectory(Path directory) {
        toolDirectory = directory;
    }

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
