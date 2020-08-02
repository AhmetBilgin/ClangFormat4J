package org.clangformat4j.tests;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.TextEdit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wangzw.plugin.cppstyle.ReplacementFormatter;
import org.wangzw.plugin.cppstyle.ThreadedProcessHandler;
import org.wangzw.plugin.cppstyle.ThreadedReplacementFormatter;

public class FormatterExecutionTimeBenchmark extends AbstractFormatterTestBase {

    private static final String HEADER_SEQUENTIAL = "Time in ns Sequential";

    private static final String HEADER_THREADPOOL =
            String.format("Time in ns ThreadPool(%s)", ThreadedProcessHandler.POOL_SIZE);

    private static final String HEADER_LINES_OF_CODE = "LOC";

    private static final String MEASUREMENT_ENTRY_FORMAT = "%10s|%24s|%24s";

    private static final char LINEFEED = '\n';

    private static final Map<String, File> FILES = new LinkedHashMap<>();

    private Map<String, Long> durationsSequential = new LinkedHashMap<>();

    private Map<String, Long> durationsThreadPool = new LinkedHashMap<>();

    @BeforeClass
    public static void beforeClass() {
        AbstractFormatterTestBase.beforeClass();
        registerBenchmarkFile("100", "unformatted/B20128760.java");
        registerBenchmarkFile("200", "unformatted/B20701054.java");
        registerBenchmarkFile("350", "unformatted/B24909927.java");
        registerBenchmarkFile("400", "unformatted/M.java");
        //        registerBenchmarkFile("100");
        //        registerBenchmarkFile("200");
        //        registerBenchmarkFile("500");
        //        registerBenchmarkFile("800");
        //        registerBenchmarkFile("1000");
        //        registerBenchmarkFile("2000");
        //        registerBenchmarkFile("3000");
        //        registerBenchmarkFile("4000");
    }

    private static void registerBenchmarkFile(String linesOfCode) {
        File testResourceDir = getTestResourceDir();
        FILES.put(linesOfCode, new File(testResourceDir, "benchmark/LinesOfCode" + linesOfCode + JAVA_EXTENSION));
    }
    private static void registerBenchmarkFile(String key, String filePath) {
        File testResourceDir = getTestResourceDir();
        FILES.put(key, new File(testResourceDir, filePath));
    }

    @Test
    public void doBenchmark() {
        measure();
        printMeasurements();
    }

    private void measure() {
        for (Entry<String, File> fileEntry : FILES.entrySet()) {
            String SourceCode = getFileContent(fileEntry);
            measureSequentialFormatter(fileEntry, SourceCode);
            measureThreadPoolFormatter(fileEntry, SourceCode);
        }
    }

    private void measureSequentialFormatter(Entry<String, File> fileEntry, String SourceCode) {
        TestableReplacementFormatter sequentialFormatter = new TestableReplacementFormatter();
        sequentialFormatter.format(SourceCode, assumeFilePath);
        durationsSequential.put(fileEntry.getKey(), sequentialFormatter.time);
    }

    private void measureThreadPoolFormatter(Entry<String, File> fileEntry, String sourceCode) {
        TestableThreadedReplacementFormatter fixedThreadPoolFormatter = new TestableThreadedReplacementFormatter();
        fixedThreadPoolFormatter.format(sourceCode, assumeFilePath);
        durationsThreadPool.put(fileEntry.getKey(), fixedThreadPoolFormatter.time);
    }

    private String getFileContent(Entry<String, File> fileEntry) {
        File javaFile = fileEntry.getValue();
        return getContent(javaFile);
    }

    private void printMeasurements() {
        String header =
                String.format(MEASUREMENT_ENTRY_FORMAT, HEADER_LINES_OF_CODE, HEADER_SEQUENTIAL, HEADER_THREADPOOL);
        StringBuilder strBuilder = new StringBuilder().append(LINEFEED).append(header).append(LINEFEED);
        for (String key : FILES.keySet()) {
            Long durationSequential = durationsSequential.get(key);
            Long durationThreadPool = durationsThreadPool.get(key);
            strBuilder.append(String.format(MEASUREMENT_ENTRY_FORMAT, key, durationSequential, durationThreadPool));
            strBuilder.append(LINEFEED);
        }
        String table = LINEFEED + LINEFEED + strBuilder.toString();
        LOG.info(table);
    }

    protected class TestableThreadedReplacementFormatter extends ThreadedReplacementFormatter {
        private long time;

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
    protected class TestableReplacementFormatter extends ReplacementFormatter {
        private long time;

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
}
