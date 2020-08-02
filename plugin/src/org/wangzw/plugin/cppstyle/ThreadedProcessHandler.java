package org.wangzw.plugin.cppstyle;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public abstract class ThreadedProcessHandler extends ProcessHandler {

    public static final int POOL_SIZE = 0;

    static final ExecutorService executorService =
            Executors.newCachedThreadPool(); // Executors.newFixedThreadPool(POOL_SIZE);

    private static final String SUCCCESS = "succcess";

    private static final String HANDLE_ERROR_STREAM = "handleErrorStream";

    private static final String HANDLE_INPUT_STREAM = "handleInputStream";

    private Future<Boolean> inputStreamFuture;

    private Future<Boolean> errorStreamFuture;

    public ThreadedProcessHandler(String source) {
        super(source);
    }

    @Override
    protected void handleInputStream() throws IOException {
        inputStreamFuture = executorService.submit(() -> {
            try {
                handleInputStreamInternal();
            }
            catch (IOException e) {
                logException(HANDLE_INPUT_STREAM, e);
            }
        }, Boolean.TRUE);
    }

    protected abstract void handleInputStreamInternal() throws IOException;

    @Override
    protected void handleErrorStream() throws IOException {
        errorStreamFuture = executorService.submit(() -> {
            try {
                super.handleErrorStream();
            }
            catch (IOException e) {
                logException(HANDLE_ERROR_STREAM, e);
            }
        }, Boolean.TRUE);
    }

    @Override
    public boolean success() throws InterruptedException {
        boolean success = false;
        try {
            Boolean inputFutureReturnValue = inputStreamFuture.get(TIMEOUT, SECONDS);
            Boolean errorFutureReturnValue = errorStreamFuture.get(TIMEOUT, SECONDS);
            success = inputFutureReturnValue && errorFutureReturnValue;
        }
        catch (ExecutionException | TimeoutException e) {
            logException(SUCCCESS, e);
        }
        return success && super.success();
    }

    private void logException(String methodName, Throwable e) {
        CppStyle.log(e.getMessage(), e);
        errout.append(String.format("Exception occured during %s\n%s", methodName, e.getMessage()));
    }
}
