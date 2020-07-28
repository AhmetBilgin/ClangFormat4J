package org.wangzw.plugin.cppstyle;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.wangzw.plugin.cppstyle.ui.CppStyleMessageConsole;

/**
 * Parameters to pass when invoking an external tool.
 *
 * @since 2.1
 */
public final class InvocationParameters {
    private final IResource originalFile;
    private final IResource actualFile;
    private final String actualFilePath;
    private final IPath workingDirectory;
    private final CppStyleMessageConsole console;

    /**
     * Constructor.
     *
     * @param originalFile
     *            the original file to process.
     * @param actualFile
     *            the actual file to process.
     * @param actualFilePath
     *            the path of {@code actual}, in a format that the external tool
     *            can understand.
     * @param workingDirectory
     *            the directory where the external tool should be executed.
     * @see #getOriginalFile()
     * @see #getActualFile()
     */
    public InvocationParameters(IResource originalFile, IResource actualFile, String actualFilePath,
            IPath workingDirectory, CppStyleMessageConsole console) {
        this.originalFile = originalFile;
        this.actualFile = actualFile;
        this.actualFilePath = actualFilePath;
        this.workingDirectory = workingDirectory;
        this.console = console;
    }

    /**
     * Returns the original file to process. This is the file that triggered
     * execution of a command-line tool when saved.
     *
     * @return the original file to process.
     */
    public IResource getOriginalFile() {
        return originalFile;
    }

    /**
     * Returns the actual file to process. It may not be the same as
     * <code>{@link #getOriginalFile()}</code>, depending on how the external
     * tool works.
     * <p>
     * A good example is an external tool that can only process C++ source files
     * but not header files. If the <em>original</em> file is a header file, the
     * checker could potentially find a C++ file that includes such header and
     * use it as the <em>actual</em> file to process.
     * </p>
     * <p>
     * We still need to keep a reference to the <em>actual</em> file, in order
     * to add markers to the editor in case of problems found.
     * </p>
     *
     * @return the actual file to process.
     */
    public IResource getActualFile() {
        return actualFile;
    }

    /**
     * Returns the path of <code>{@link #getActualFile()}</code>, in a format
     * the external tool can understand.
     *
     * @return the path of the <em>actual</em> file to process.
     */
    public String getActualFilePath() {
        return actualFilePath;
    }

    /**
     * Returns the directory where the external tool should be executed.
     *
     * @return the directory where the external tool should be executed.
     */
    public IPath getWorkingDirectory() {
        return workingDirectory;
    }

    public CppStyleMessageConsole getConsole() {
        return console;
    }
}
