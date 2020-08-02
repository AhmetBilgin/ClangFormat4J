package org.clangformat4j.tests;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;
import org.wangzw.plugin.cppstyle.ThreadedProcessHandler;

public class FormatterExecutionTimeBenchmark extends AbstractFormatterTestBase {

    private static final String MEASUREMENT_HEADER_FORMAT = "%10s|%24s|%24s|%24s";

    private static final String MEASUREMENT_ENTRY_FORMAT = "%10s|%24.2f|%24.2f|%24.2f";

    private static final String HEADER_LINES_OF_CODE = "LOC";

    private static final String HEADER_SEQUENTIAL = "Time in ns Sequential";

    private static final String HEADER_THREADPOOL =
            String.format("Time in \u00B5s ThreadPool(%s)", ThreadedProcessHandler.POOL_SIZE);

    private static final String HEADER_DIFF_FORMATTER = "HEADER_DIFF_FORMATTER";

    private static final String THREADPOOL_SIZES_HEADER_FORMAT = "%24s|%24s|%24s";

    private static final String THREADPOOL_SIZES_ENTRY_FORMAT = "%24d|%24d|%24d";

    private static final char LINEFEED = '\n';

    private static final int RUNS_PER_FILE = 3;

    private static final Map<String, File> FILES = new LinkedHashMap<>();

    private Map<String, Long> durationsSequential = new LinkedHashMap<>();

    private Map<String, Long> durationsThreadPool = new LinkedHashMap<>();

    private Map<String, Long> durationsDiffFormatter = new LinkedHashMap<>();

    @BeforeClass
    public static void beforeClass() {
        AbstractFormatterTestBase.beforeClass();
        registerBenchmarkFile("1", "benchmark/DummyClass.java");
        registerBenchmarkFile("100", "unformatted/B20128760.java");
        registerBenchmarkFile("200", "unformatted/B20701054.java");
        registerBenchmarkFile("350", "unformatted/B24909927.java");
        registerBenchmarkFile("400", "unformatted/M.java");
        //        registerBenchmarkFile("100");
        //        registerBenchmarkFile("200");
        //        registerBenchmarkFile("500");
        registerBenchmarkFile("800");
        registerBenchmarkFile("1000");
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
        printThreadPoolSizes();
    }

    private void measure() {
        for (Entry<String, File> fileEntry : FILES.entrySet()) {
            for (int i = 0; i < RUNS_PER_FILE; i++) {
                String SourceCode = getFileContent(fileEntry);
                measureSequentialFormatter(fileEntry, SourceCode);
                measureThreadPoolFormatter(fileEntry, SourceCode);
                measureDiffFormatter(fileEntry, SourceCode);
            }
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

    private void measureDiffFormatter(Entry<String, File> fileEntry, String SourceCode) {
        TestableClangFormatFormatter clangFormatFormatter = new TestableClangFormatFormatter();
        clangFormatFormatter.format(SourceCode, assumeFilePath);
        durationsDiffFormatter.put(fileEntry.getKey(), clangFormatFormatter.time);
    }

    private String getFileContent(Entry<String, File> fileEntry) {
        File javaFile = fileEntry.getValue();
        return getContent(javaFile);
    }

    private void printMeasurements() {
        String header = String.format(MEASUREMENT_HEADER_FORMAT, HEADER_LINES_OF_CODE, HEADER_SEQUENTIAL,
                HEADER_THREADPOOL, HEADER_DIFF_FORMATTER);
        StringBuilder strBuilder = new StringBuilder().append(LINEFEED).append(header).append(LINEFEED);
        for (String key : FILES.keySet()) {
            double durationSequential = convertToMeanValueInMicroSeconds(durationsSequential.get(key));
            double durationThreadPool = convertToMeanValueInMicroSeconds(durationsThreadPool.get(key));
            double durationDiffFormatter = convertToMeanValueInMicroSeconds(durationsDiffFormatter.get(key));
            strBuilder.append(String.format(
                    MEASUREMENT_ENTRY_FORMAT, key, durationSequential, durationThreadPool, durationDiffFormatter));
            strBuilder.append(LINEFEED);
        }
        String table = LINEFEED + LINEFEED + strBuilder.toString();
        LOG.info(table);
    }

    private void printThreadPoolSizes() {
        ThreadPoolExecutor poolExecutor = TestableThreadedReplacementFormatter.getThreadPoolExecuter();
        String header =
                String.format(THREADPOOL_SIZES_HEADER_FORMAT, "CorePoolSize", "LargestPoolSize", "MaximumPoolSize");
        StringBuilder strBuilder = new StringBuilder().append(LINEFEED).append(header).append(LINEFEED);
        String poolSizes = String.format(THREADPOOL_SIZES_ENTRY_FORMAT, poolExecutor.getCorePoolSize(),
                poolExecutor.getLargestPoolSize(), poolExecutor.getMaximumPoolSize());
        strBuilder.append(poolSizes).append(LINEFEED);
        LOG.info(strBuilder.toString());
    }

    private double convertToMeanValueInMicroSeconds(Long cumulatedValue) {
        return (TimeUnit.NANOSECONDS.toMicros(cumulatedValue) / 1.d) / RUNS_PER_FILE;
    }
}
