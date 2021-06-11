package org.wangzw.plugin.cppstyle;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

public class FilePathUtil {

    public static boolean fileExists(String filePath) {
        return filePath != null && !filePath.isEmpty() && new File(filePath).exists();
    }

    public static boolean isFileRunnable(String path) {
        File file = new File(path);
        return file.exists() && !file.isDirectory();
    }

    public static List<String> resolvePaths(String semicolonSeperatedPaths) {
        String[] splittedPaths = semicolonSeperatedPaths.split(";");
        List<String> resolvedPaths = new ArrayList<>();
        for (String value : splittedPaths) {
            String resolvedPath = EnvironmentVariableExpander.expandEnvVar(value.trim());
            if (!resolvedPath.isEmpty()) {
                resolvedPaths.add(resolvedPath);
            }
        }
        return resolvedPaths;
    }

    public static String toNormalizedAbsolutePath(String path) {
        return FileSystems.getDefault().getPath(path).normalize().toAbsolutePath().toString();
    }}
