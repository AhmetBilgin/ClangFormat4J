package org.wangzw.plugin.cppstyle;

import static org.wangzw.plugin.cppstyle.replacement.Logger.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
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
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;
import org.wangzw.plugin.cppstyle.ui.CppStyleMessageConsole;

public abstract class CodeFormatterBase extends CodeFormatter {

    private static final String ASSUME_FILENAME = "-assume-filename=";

    private static final String STYLE_VIA_FILE = "-style=file";

    private static final String FALLBACK_STYLE_CHROMIUM = "-fallback-style=Chromium";

    private static final String OUTPUT_REPLACEMENTS_XML = "-output-replacements-xml";

    Map<String, String> options;

    protected MessageConsoleStream err = null;

    public CodeFormatterBase() {
        super();
        CppStyleMessageConsole console = CppStyle.buildConsole();
        err = console.getErrorStream();
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
        TextEdit retval = format(source, getSourceFilePath(), regions);
        return retval != null ? retval : new MultiTextEdit();
    }

    protected TextEdit format(String source, String path, IRegion[] regions) {
        String clangFormatPath = getClangFormatPath();
        if (checkClangFormat(clangFormatPath) == false) {
            return null;
        }

        String confPath = getClangFormatConfigureFile(path);
        if (confPath == null) {
            err.println("Cannot find .clang-format or _clang-format configuration file under any level "
                    + "parent directories of path (" + path + ").");
            err.println("Not applying any formatting.");
            return null;
        }
        else {
            logInfo(String.format("Using style-file: %s", confPath));
        }

        try {
            ProcessHandler processHandler = createProcessHandler(source);
            addpParameters(processHandler, clangFormatPath, path, regions);
            processHandler.start();
            processHandler.handleInputStream();
            processHandler.handleErrorStream();

            if (!processHandler.success() || processHandler.hasErrors()) {
                err.println("clang-format return error (" + processHandler.getCode() + ").");
                err.println(processHandler.getError());
                return null;
            }

            MultiTextEdit edit = processHandler.getTextEdit();
            return edit;
        }
        catch (IOException e) {
            CppStyle.log("Failed to format code", e);
        }
        catch (InterruptedException e) {
            CppStyle.log("Failed to format code", e);
        }

        return null;
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

    private void addpParameters(ProcessHandler processHandler, String clangFormatPath, String path, IRegion[] regions) {
        processHandler.addParameter(clangFormatPath);
        processHandler.addParameter(ASSUME_FILENAME + path);
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

    protected static String getClangFormatPath() {
        return CppStyle.getDefault().getPreferenceStore().getString(CppStyleConstants.CLANG_FORMAT_PATH);
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

    protected String getClangFormatConfigureFile(String path) {
        File file = new File(path);

        while (file != null) {
            File dir = file.getParentFile();
            if (dir != null) {
                File conf = new File(dir, ".clang-format");
                if (conf.exists()) {
                    return conf.getAbsolutePath();
                }

                conf = new File(dir, "_clang-format");
                if (conf.exists()) {
                    return conf.getAbsolutePath();
                }
            }

            file = dir;
        }

        return null;
    }

    @Override
    public String createIndentationString(int indentationLevel) {
        return super.createIndentationString(indentationLevel);
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

    public boolean checkClangFormat(String clangformat) {
        if (clangformat == null) {
            err.println("clang-format is not specified.");
            return false;
        }

        File file = new File(clangformat);

        if (!file.exists()) {
            err.println("clang-format (" + clangformat + ") does not exist.");
            return false;
        }

        if (!file.canExecute()) {
            err.println("clang-format (" + clangformat + ") is not executable.");
            return false;
        }

        return true;
    }

    protected String getSourceFilePath() {
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
                                return filePath.toOSString();
                            }
                        }
                    }
                }
            }
        }

        // ITranslationUnit tu =
        // (ITranslationUnit)options.get(DefaultCodeFormatterConstants.FORMATTER_TRANSLATION_UNIT);
        //
        // if (tu == null) {
        // IFile file =
        // (IFile)options.get(DefaultCodeFormatterConstants.FORMATTER_CURRENT_FILE);
        // if (file != null) {
        // tu = (ITranslationUnit)CoreModel.getDefault().create(file);
        // }
        // }

        // added
        err.println("Not yet implemented: getSourceFilePath from CompilationUnit");
        ICompilationUnit tu = null;
        if (tu != null) {
            return tu.getResource().getRawLocation().toOSString();
        }
        else {
            String root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
            return new File(root, "A.java").getAbsolutePath();
        }
    }
}