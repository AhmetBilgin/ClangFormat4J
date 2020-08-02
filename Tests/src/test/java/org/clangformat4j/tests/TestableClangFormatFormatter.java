package org.clangformat4j.tests;

import org.eclipse.text.edits.TextEdit;
import org.wangzw.plugin.cppstyle.ClangFormatFormatter;

public class TestableClangFormatFormatter extends ClangFormatFormatter {
    long time;

    public TextEdit format(String source, String path) {
        return super.format(source, path, null);
    }

    @Override
    protected TextEdit handleProcess(String source, ProcessBuilder builder) {
        long start = System.nanoTime();
        TextEdit textEdit = super.handleProcess(source, builder);
        time = System.nanoTime() - start;
        return textEdit;
    }
}