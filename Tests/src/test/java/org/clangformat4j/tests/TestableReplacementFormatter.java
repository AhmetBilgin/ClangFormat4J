package org.clangformat4j.tests;

import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.TextEdit;
import org.wangzw.plugin.cppstyle.ReplacementFormatter;

public class TestableReplacementFormatter extends ReplacementFormatter {
    long time;

    public TextEdit format(String source, String path) {
        return super.format(source, path, new IRegion[0]);
    }

    @Override
    protected TextEdit handleProcess(
            String source, String assumeFilenamePath, IRegion[] regions, String clangFormatPath) {
        long start = System.nanoTime();
        TextEdit textEdit = super.handleProcess(source, assumeFilenamePath, regions, clangFormatPath);
        time = System.nanoTime() - start;
        return textEdit;
    }
}