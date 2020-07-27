package org.wangzw.plugin.cppstyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.text.edits.MultiTextEdit;

public abstract class ProcessHandler {

    protected Process process;

    List<String> commands = new ArrayList<>();

    private String source;

    private int code = -1;

    protected MultiTextEdit textEdit;

    private StringBuilder errout;

    public ProcessHandler(String source) {
        this.source = source;
    }

    public void start() throws IOException {
        ProcessBuilder builder = createProcessBuilder();
        process = builder.start();
        pipeSourceCodeToProcess(process, source);
    }

    private ProcessBuilder createProcessBuilder() {
        ProcessBuilder builder = new ProcessBuilder(commands);
        String root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
        builder.directory(new File(root));
        return builder;
    }

    private void pipeSourceCodeToProcess(Process process, String sourceCode) throws IOException {
        OutputStreamWriter output = new OutputStreamWriter(process.getOutputStream());
        output.write(sourceCode);
        output.flush();
        output.close();
    }

    public ProcessHandler addParameter(String parameter) {
        commands.add(parameter);
        return this;
    }

    protected abstract void handleInputStream() throws IOException;

    protected void handleErrorStream() throws IOException {
        InputStreamReader error = new InputStreamReader(process.getErrorStream());
        final char[] buffer = new char[1024];
        errout = new StringBuilder();

        for (;;) {
            int rsz = error.read(buffer, 0, buffer.length);

            if (rsz < 0) {
                break;
            }

            errout.append(buffer, 0, rsz);
        }
    }

    public String getError() {
        return errout.toString();
    }

    public int getCode() {
        return code;
    }

    public boolean success() throws InterruptedException {
        code = process.waitFor();
        return code == 0;
    }

    public boolean hasErrors() {
        return errout.length() > 0;

    }

    MultiTextEdit getTextEdit() {
        return this.textEdit;
    }
}