package org.clangformat4j.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.wangzw.plugin.cppstyle.CppStyle;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;

public class AbstractFormatterTestBase {

    final static Logger LOG = Logger.getLogger(AbstractFormatterTestBase.class.getName());

    private static final String ENVIRONMANT_VARIABLE_CLANGFORMAT = "CLANG_FORMAT_EXECUTABLE";

    private static File CLANG_FORMAT_EXECUTABLE_FILE;

    private static final File CLANG_FORMAT_STYLE_FILE = new File("./../.clang-format");

    private static final String SRC_TEST_RESOURCES_DIR = "src/test/resources";

    protected static final String JAVA_EXTENSION = ".java";

    protected static String assumeFilePath;

    @BeforeClass
    public static void beforeClass() {
        configureClangFormatExecutable();
        configureClangFormatStyleFile();
    }

    private static void configureClangFormatExecutable() {
        assignClangFormatExecutablePathFromEnvironment();
        assertClangFormatExecutableExists();
        setClangFormatExecutablePathInPreferenceStore();
    }

    private static void assignClangFormatExecutablePathFromEnvironment() {
        String path = System.getenv(ENVIRONMANT_VARIABLE_CLANGFORMAT);
        if (path != null) {
            CLANG_FORMAT_EXECUTABLE_FILE = new File(path).getAbsoluteFile();
        }
    }

    private static void assertClangFormatExecutableExists() {
        String msgClangFormatNotFound =
                String.format("Please configure your clang-format path with the einvironment variable %s",
                        ENVIRONMANT_VARIABLE_CLANGFORMAT);
        assertNotNull(msgClangFormatNotFound, CLANG_FORMAT_EXECUTABLE_FILE);
        assertTrue(msgClangFormatNotFound, CLANG_FORMAT_EXECUTABLE_FILE.exists());
    }

    private static void setClangFormatExecutablePathInPreferenceStore() {
        CppStyle.getDefault().getPreferenceStore().setValue(
                CppStyleConstants.CLANG_FORMAT_PATH, CLANG_FORMAT_EXECUTABLE_FILE.getAbsolutePath());
    }
    static void configureClangFormatStyleFile() {
        assertClangFormatStyleFileExists();
        setClangFormatStylePathInPreferenceStore();
    }

    private static void assertClangFormatStyleFileExists() {
        String msgStyleFileNotFound = String.format(
                ".clang-format file does not exists at location %s", CLANG_FORMAT_STYLE_FILE.getAbsolutePath());
        assertTrue(msgStyleFileNotFound, CLANG_FORMAT_STYLE_FILE.exists());
        assumeFilePath = new File(CLANG_FORMAT_STYLE_FILE.getParentFile(), "A.java").getAbsolutePath();
    }

    private static void setClangFormatStylePathInPreferenceStore() {
        CppStyle.getDefault().getPreferenceStore().setValue(
                CppStyleConstants.CLANG_FORMAT_STYLE_PATH, CLANG_FORMAT_STYLE_FILE.getAbsolutePath());
    }

    protected static File getTestResourceDir() {
        String testResources = SRC_TEST_RESOURCES_DIR;
        File testResourceDir = new File(testResources).getAbsoluteFile();
        return testResourceDir;
    }

    protected String getContent(File javaFile) {
        String fileContent = null;
        try {
            byte[] allBytes = Files.readAllBytes(javaFile.toPath());
            fileContent = new String(allBytes);
        }
        catch (IOException e) {
            logError(e.getMessage(), e);
        }
        return fileContent;
    }

    protected void logError(String msg) {
        logError(msg, null);
    }

    protected void logError(String msg, Throwable t) {
        LOG.log(Level.SEVERE, msg, t);
    }
}