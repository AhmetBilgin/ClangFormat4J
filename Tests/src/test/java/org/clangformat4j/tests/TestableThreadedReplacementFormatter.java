package org.clangformat4j.tests;

import java.util.concurrent.ThreadPoolExecutor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.TextEdit;
import org.wangzw.plugin.cppstyle.ThreadedReplacementFormatter;

public class TestableThreadedReplacementFormatter extends ThreadedReplacementFormatter {
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

    public static ThreadPoolExecutor getThreadPoolExecuter() {
        return ThreadedReplacementFormatter.getThreadPoolExecuter();
    }
}