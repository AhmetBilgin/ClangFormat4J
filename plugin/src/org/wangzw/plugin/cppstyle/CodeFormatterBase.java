package org.wangzw.plugin.cppstyle;

import static org.wangzw.plugin.cppstyle.replacement.Logger.*;
import static org.wangzw.plugin.cppstyle.ui.CppStyleConstants.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jdt.internal.formatter.TextEditsBuilder;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.wangzw.plugin.cppstyle.ui.CppStyleMessageConsole;

@SuppressWarnings("restriction")
public abstract class CodeFormatterBase extends CodeFormatter {

    private static final String ASSUME_FILENAME = "-assume-filename=";

    private static final String ASSUME_FILENAME_JAVA = "A.java";

    private static final String STYLE_VIA_FILE = "-style=file";

    private static final String FALLBACK_STYLE_CHROMIUM = "-fallback-style=Chromium";

    private static final String OUTPUT_REPLACEMENTS_XML = "-output-replacements-xml";

    private static final int INDENTATION_WIDTH = 4;

    private static final int TABSIZE = 8;

    Map<String, String> options;

    protected MessageConsoleStream err = null;

    private String clangFormatPath;

    private String clangFormatStylePath;

    private String assumeFilenamePath;

    private boolean isClangFormatStylePathValid;

    private boolean isAssumeFilenamePathValid;

    private ClangPathHelper clangPathHelper;

    public CodeFormatterBase() {
        super();
        CppStyleMessageConsole console = CppStyle.buildConsole();
        err = console.getErrorStream();

        clangPathHelper = new ClangPathHelper();
        initClangFormatPath();
        initClangFormatStylePath();
        initAssumeFilenamePath();
    }

    private void initClangFormatPath() {
        if (clangPathHelper.getCachedClangFormatPath() == null) {
            List<String> candidates = getClangFormatPathsFromPreferences();
            boolean validPathPresent = clangPathHelper.getFirstValidClangFormatPath(candidates).isPresent();
            if (!validPathPresent) {
                logError("No valid clang-format executable path found");
            }
        }
        clangFormatPath = clangPathHelper.getCachedClangFormatPath();
    }

    private void initClangFormatStylePath() {
        if (clangPathHelper.getCachedClangFormatStylePath() == null) {
            List<String> candidates = getClangFormatStylePathsFromPreferences();
            boolean validPathPresent = clangPathHelper.getFirstValidClangFormatStylePath(candidates).isPresent();
            if (!validPathPresent) {
                logInfo("No valid .clang-format style path found");
            }
        }
        clangFormatStylePath = clangPathHelper.getCachedClangFormatStylePath();
        isClangFormatStylePathValid = clangFormatStylePath != null;
    }

    private void initAssumeFilenamePath() {
        if (isClangFormatStylePathValid) {
            assumeFilenamePath = stylePathToAssumeFilenamePath(clangFormatStylePath);
            isAssumeFilenamePathValid = true;
        }
    }

    @Override
    public TextEdit format(
            int kind, String source, int offset, int length, int indentationLevel, String lineSeparator) {
        TextEdit retval =
                format(kind, source, new IRegion[] { new Region(offset, length) }, indentationLevel, lineSeparator);
        return retval;
    }

    @Override
    public TextEdit format(int kind, String source, IRegion[] regions, int indentationLevel, String lineSeparator) {
        TextEdit retval = format(source, getAssumeFilenamePath(), regions);
        return retval != null ? retval : new MultiTextEdit();
    }

    protected TextEdit format(String source, String path, IRegion[] regions) {
        logInfo(String.format("Using clang-format: %s with style-file: %s", clangFormatPath, clangFormatStylePath));
        return handleProcess(source, clangFormatPath, assumeFilenamePath, regions);
    }

    protected TextEdit handleProcess(
            String source, String clangFormatPath, String assumeFilenamePath, IRegion[] regions) {
        MultiTextEdit edit = null;
        try {
            ProcessHandler processHandler = createProcessHandler(source);
            addpParameters(processHandler, clangFormatPath, assumeFilenamePath, regions);
            processHandler.start();
            processHandler.handleInputStream();
            processHandler.handleErrorStream();

            if (!processHandler.success() || processHandler.hasErrors()) {
                err.println("clang-format return error (" + processHandler.getCode() + ").");
                err.println(processHandler.getError());
            }
            else {
                edit = processHandler.getTextEdit();
            }
        }
        catch (IOException e) {
            CppStyle.log("Failed to format code", e);
        }
        catch (InterruptedException e) {
            CppStyle.log("Failed to format code", e);
        }

        return edit;
    }

    public void formatAndApply(IDocument doc, String path) {
        TextEdit res = format(doc.get(), path, new IRegion[0]);

        if (res == null) {
            return;
        }

        IDocumentUndoManager manager = DocumentUndoManagerRegistry.getDocumentUndoManager(doc);
        manager.beginCompoundChange();

        try {
            res.apply(doc);
        }
        catch (MalformedTreeException e) {
            CppStyle.log("Failed to apply change", e);
        }
        catch (BadLocationException e) {
            CppStyle.log("Failed to apply change", e);
        }

        manager.endCompoundChange();
    }

    private void addpParameters(
            ProcessHandler processHandler, String clangFormatPath, String assumeFilenamePath, IRegion[] regions) {
        processHandler.addParameter(clangFormatPath);
        processHandler.addParameter(ASSUME_FILENAME + assumeFilenamePath);
        // make clang-format do its own search for the configuration, but fall back to
        // Chromium.
        processHandler.addParameter(STYLE_VIA_FILE);
        processHandler.addParameter(FALLBACK_STYLE_CHROMIUM);
        processHandler.addParameter(OUTPUT_REPLACEMENTS_XML);
        if (regions != null) {
            for (IRegion region : regions) {
                processHandler.addParameter("-offset=" + region.getOffset());
                processHandler.addParameter("-length=" + region.getLength());
            }
        }
    }

    protected List<String> getClangFormatPathsFromPreferences() {
        return getResolvedPreferenceValues(CLANG_FORMAT_PATH);
    }

    protected List<String> getClangFormatStylePathsFromPreferences() {
        return getResolvedPreferenceValues(CLANG_FORMAT_STYLE_PATH);
    }

    private List<String> getResolvedPreferenceValues(String preferenceName) {
        String semicolonSeperatedPaths = CppStyle.getDefault().getPreferenceStore().getString(preferenceName);
        return FilePathUtil.resolvePaths(semicolonSeperatedPaths);
    }

    private static IPath getSourceFilePathFromEditorInput(IEditorInput editorInput) {
        if (editorInput instanceof IURIEditorInput) {
            URI uri = ((IURIEditorInput)editorInput).getURI();
            if (uri != null) {
                IPath path = URIUtil.toPath(uri);
                if (path != null) {
                    return path;
                }
            }
        }

        if (editorInput instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput)editorInput).getFile();
            if (file != null) {
                return file.getLocation();
            }
        }

        if (editorInput instanceof ILocationProvider) {
            return ((ILocationProvider)editorInput).getPath(editorInput);
        }

        return null;
    }

    protected abstract ProcessHandler createProcessHandler(final String source);

    /**
     * Implementation from DefaultCodeFormatter
     */
    @Override
    public String createIndentationString(int indentationLevel) {
        if (indentationLevel < 0) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder();
        int indent = indentationLevel * INDENTATION_WIDTH;
        TextEditsBuilder.appendIndentationString(sb, DefaultCodeFormatterOptions.SPACE, TABSIZE, indent, 0);
        return sb.toString();
    }

    @Override
    public void setOptions(Map<String, String> options) {
        if (options != null) {
            this.options = options;
        }
        else {
            this.options = JavaCore.getOptions();
        }
    }

    protected String getAssumeFilenamePath() {
        if (isAssumeFilenamePathValid) {
            return assumeFilenamePath;
        }
        logInfo("Trying to find .clang-format style");

        assumeFilenamePath = getJavaFilePathFromActiveEditor();
        if (FilePathUtil.fileExists(assumeFilenamePath)) {
            return assumeFilenamePath;
        }

        assumeFilenamePath = useWorkspaceFallback();
        return assumeFilenamePath;
    }

    private String stylePathToAssumeFilenamePath(String clangFormatStylePath) {
        File assumeFile = new File(new File(clangFormatStylePath).getParentFile(), ASSUME_FILENAME_JAVA);
        return assumeFile.getAbsolutePath();
    }

    private String getJavaFilePathFromActiveEditor() {
        String javaFilePath = null;
        IWorkbench wb = PlatformUI.getWorkbench();
        if (wb != null) {
            IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    IEditorPart activeEditor = page.getActiveEditor();
                    if (activeEditor != null) {
                        IEditorInput editorInput = activeEditor.getEditorInput();
                        if (editorInput != null) {
                            IPath filePath = getSourceFilePathFromEditorInput(editorInput);
                            if (filePath != null) {
                                javaFilePath = filePath.toOSString();
                            }
                        }
                    }
                }
            }
        }
        return javaFilePath;
    }

    private String useWorkspaceFallback() {
        String workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
        String assumeFilePath = new File(workspaceRoot, ASSUME_FILENAME_JAVA).getAbsolutePath();
        return assumeFilePath;
    }
}