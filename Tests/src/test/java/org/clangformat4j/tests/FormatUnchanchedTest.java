package org.clangformat4j.tests;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wangzw.plugin.cppstyle.CppStyle;
import org.wangzw.plugin.cppstyle.ReplacementFormatter;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;

public class FormatUnchanchedTest {

//    @Rule
//    public TemporaryFolder testFolder = new TemporaryFolder();

    private static final String SRC_TEST_RESOURCES = "src/test/resources";

    private static final String OUTPUT_EXTENSION = ".output";

    private static final String JAVA_EXTENSION = ".java";

    private ReplacementFormatter formatter;

    private List<File> wronglyChangedFiles;

    @BeforeClass
    public static void beforeClass() {
        CppStyle.getDefault().getPreferenceStore().setValue(CppStyleConstants.CLANG_FORMAT_PATH,
                "D:/Projects/productRelated/codeStyle/clang-8.0.1/bin/clang-format.exe");
    }

    @Before
    public void setUp() {
        formatter = new ReplacementFormatter();
        wronglyChangedFiles = new ArrayList<>();
    }

    @Test
    public void testFormattedFilesEqualReferenceFiles() {
        File testResourceDir = getTestResourceDir();
        getAllJavaFiles(testResourceDir).forEach(this::checkFormatUnchanged);
        printFilesThatChanged();
    }

    private void checkFormatUnchanged(File javaFile) {
        String fileContent = getContent(javaFile);
        IDocument referenceDocument = new Document(fileContent);
        IDocument formattedDocument = new Document(fileContent);
        DocumentUndoManagerRegistry.connect(formattedDocument);
        File formattedJavaFile = new File(javaFile.getPath() /* + OUTPUT_EXTENSION */);
        formatter.formatAndApply(formattedDocument, formattedJavaFile.getPath());

        if (!referenceDocument.equals(formattedDocument)) {
            wronglyChangedFiles.add(formattedJavaFile);
        }
        DocumentUndoManagerRegistry.disconnect(formattedDocument);
    }

    private void printFilesThatChanged() {
        for (File file : wronglyChangedFiles) {
            System.out.println("File shouldn't have changed: " + file.getPath());
        }
    }

    private String getContent(File javaFile) {
        String fileContent = null;
        try {
            byte[] allBytes = Files.readAllBytes(javaFile.toPath());
            fileContent = new String(allBytes);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return fileContent;
    }

    private File getTestResourceDir() {
        String testResources = SRC_TEST_RESOURCES;
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
}
