package org.clangformat4j.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wangzw.plugin.cppstyle.CppStyle;
import org.wangzw.plugin.cppstyle.ReplacementFormatter;
import org.wangzw.plugin.cppstyle.replacement.Formatter;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;

public class FormatUnchangedTest {

    private final static Logger LOG = Logger.getLogger(FormatUnchangedTest.class.getName());

    private static final String CLANG_FORMAT_EXECUTABLE_PATH = "../../../clang-8.0.1/bin/clang-format.exe";

    private static final File CLANG_FORMAT_EXECUTABLE_FILE = new File(CLANG_FORMAT_EXECUTABLE_PATH).getAbsoluteFile();

    private static final String SRC_TEST_RESOURCES_DIR = "src/test/resources";

    private static final String COMMANDLINE_FORMATTED_DIR = "commandline-formatted";

    private static final String JAVA_EXTENSION = ".java";

    private ReplacementFormatter formatter;

    private List<File> wronglyChangedFiles;

    private Formatter referenceFormatter;

    @BeforeClass
    public static void beforeClass() {
        assertClangFormatExecutableExists();
        setClangFormatExecutablePath();
    }

    private static void setClangFormatExecutablePath() {
        CppStyle.getDefault().getPreferenceStore().setValue(CppStyleConstants.CLANG_FORMAT_PATH,
                CLANG_FORMAT_EXECUTABLE_FILE.getAbsolutePath());
    }

    private static void assertClangFormatExecutableExists() {
        String msgClangFormatNotFound =
                String.format("Please configure your clang-format path within %s " + " currently set to %s",
                        FormatUnchangedTest.class.getName(), CLANG_FORMAT_EXECUTABLE_FILE.getAbsolutePath());
        assertTrue(msgClangFormatNotFound, CLANG_FORMAT_EXECUTABLE_FILE.exists());
    }

    @Before
    public void setUp() {
        referenceFormatter = new Formatter();
        formatter = new ReplacementFormatter();
        wronglyChangedFiles = new ArrayList<>();
    }

    @Test
    public void testFormatterImplementationsProduceEqualOutput() {
        File commandLineFormattedDir = getCommandLineFormattedDir();
        getAllJavaFiles(commandLineFormattedDir).forEach(this::checkFormatUnchanged);
        checkAllFilesAreEqual();
    }

    private File getCommandLineFormattedDir() {
        File testResourceDir = getTestResourceDir();
        File cmdFormattedDir = new File(testResourceDir, COMMANDLINE_FORMATTED_DIR);
        return cmdFormattedDir;
    }

    private void checkFormatUnchanged(File javaFile) {
        String fileContent = getContent(javaFile);

        IDocument formattedDocument = formatWithTestedFormatter(fileContent, javaFile);
        IDocument referenceDocument = formatWithReferenceFormatter(fileContent, javaFile);

        String referenceFormattedContent = referenceDocument.get();
        String testedFormattedContent = formattedDocument.get();
        checkDocumentsAreEqual(javaFile, referenceFormattedContent, testedFormattedContent);
    }

    private void checkDocumentsAreEqual(File javaFile, String referenceFormattedContent,
            String testedFormattedContent) {
        if (!referenceFormattedContent.equals(testedFormattedContent)) {
            File fileThatDiffersWhenFormatted = new File(javaFile.getPath());
            wronglyChangedFiles.add(fileThatDiffersWhenFormatted);
            logError("The following file is formatted differently" + fileThatDiffersWhenFormatted);
            logError("reference:\n" + referenceFormattedContent + "\n\n");
            logError("formatted:\n" + testedFormattedContent + "\n\n");
        }
    }

    private IDocument formatWithReferenceFormatter(String fileContent, File file) {
        IDocument formattedDocument = new Document(fileContent);
        DocumentUndoManagerRegistry.connect(formattedDocument);
        referenceFormatter.formatAndApply(formattedDocument, file.getPath());
        DocumentUndoManagerRegistry.disconnect(formattedDocument);
        return formattedDocument;
    }

    private IDocument formatWithTestedFormatter(String fileContent, File file) {
        IDocument formattedDocument = new Document(fileContent);
        DocumentUndoManagerRegistry.connect(formattedDocument);
        formatter.formatAndApply(formattedDocument, file.getPath());
        DocumentUndoManagerRegistry.disconnect(formattedDocument);
        return formattedDocument;
    }

    private void checkAllFilesAreEqual() {
        for (File file : wronglyChangedFiles) {
            String msg = "File shouldn't have changed: " + file.getPath();
            logError(msg);
        }

        int expectedFilesThatDiffer = 0;
        assertEquals("There should be no formatted files that differ", expectedFilesThatDiffer,
                wronglyChangedFiles.size());
    }

    private String getContent(File javaFile) {
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

    private File getTestResourceDir() {
        String testResources = SRC_TEST_RESOURCES_DIR;
        File testResourceDir = new File(testResources).getAbsoluteFile();
        return testResourceDir;
    }

    private Stream<File> getAllJavaFiles(File testResourceDir) {
        File[] javaFiles = testResourceDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(JAVA_EXTENSION);
            }
        });
        return Arrays.stream(javaFiles);
    }

    private void logError(String msg) {
        logError(msg, null);
    }

    private void logError(String msg, Throwable t) {
        LOG.log(Level.SEVERE, msg, t);
    }
}
