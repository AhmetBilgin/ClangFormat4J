package org.wangzw.plugin.cppstyle;

import java.io.IOException;

public class ThreadedReplacementFormatter extends ReplacementFormatter {

    @Override
    protected ProcessHandler createProcessHandler(String source) {

        return new ThreadedProcessHandler(source) {
            @Override
            protected void handleInputStreamInternal() throws IOException {
                textEdit = createEdit(process);
            }
        };
    }
}
