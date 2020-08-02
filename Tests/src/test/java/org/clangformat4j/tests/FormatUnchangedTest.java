package org.clangformat4j.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.junit.Before;
import org.junit.Test;
import org.wangzw.plugin.cppstyle.CodeFormatterBase;
import org.wangzw.plugin.cppstyle.ReplacementFormatter;
import org.wangzw.plugin.cppstyle.ThreadedReplacementFormatter;

public class FormatUnchangedTest extends AbstractFormatterTestBase {

    private static final String COMMANDLINE_FORMATTED_DIR = "commandline-formatted";

    private CodeFormatterBase referenceFormatter;

    private CodeFormatterBase testedFormatter;

    private List<File> wronglyChangedFiles;

    @Before
    public void setUp() {
        referenceFormatter = new ReplacementFormatter();
        testedFormatter = new ThreadedReplacementFormatter();
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

    protected void checkDocumentsAreEqual(
            File javaFile, String referenceFormattedContent, String testedFormattedContent) {
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
        testedFormatter.formatAndApply(formattedDocument, file.getPath());
        DocumentUndoManagerRegistry.disconnect(formattedDocument);
        return formattedDocument;
    }

    private void checkAllFilesAreEqual() {
        for (File file : wronglyChangedFiles) {
            String msg = "File shouldn't have changed: " + file.getPath();
            logError(msg);
        }

        int expectedFilesThatDiffer = 0;
        assertEquals(
                "There should be no formatted files that differ", expectedFilesThatDiffer, wronglyChangedFiles.size());
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
