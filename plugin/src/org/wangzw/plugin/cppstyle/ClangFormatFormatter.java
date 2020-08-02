package org.wangzw.plugin.cppstyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
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
import org.eclipse.ui.texteditor.ITextEditor;
import org.wangzw.plugin.cppstyle.diff_match_patch.Diff;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;
import org.wangzw.plugin.cppstyle.ui.CppStyleMessageConsole;

public class ClangFormatFormatter extends CodeFormatter {
    private MessageConsoleStream err = null;

    private Map<String, ?> options;

    public ClangFormatFormatter() {
        super();
        CppStyleMessageConsole console = CppStyle.buildConsole();
        err = console.getErrorStream();
    }

    public void formatAndApply(ITextEditor editor) {
        IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());

        String path = ((IFileEditorInput)editor.getEditorInput()).getFile().getLocation().toOSString();
        formatAndApply(doc, path);
    }

    public void formatAndApply(IDocument doc, String path) {
        TextEdit res = format(doc.get(), path, null);

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

    protected TextEdit format(String source, String path, IRegion region) {
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

        // make clang-format do its own search for the configuration, but fall back to
        // Google.
        String stdArg = "-style=file";
        String fallbackArg = "-fallback-style=Google";

        ArrayList<String> commands =
                new ArrayList<>(Arrays.asList(clangFormatPath, "-assume-filename=" + path, stdArg, fallbackArg));

        StringBuffer sb = new StringBuffer();
        sb.append(stdArg + " " + fallbackArg + " ");

        if (region != null) {
            commands.add("-offset=" + region.getOffset());
            commands.add("-length=" + region.getLength());

            sb.append("-offset=");
            sb.append(region.getOffset());
            sb.append(" -length=");
            sb.append(region.getLength());
            sb.append(' ');
        }

        ProcessBuilder builder = new ProcessBuilder(commands);

        String root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
        builder.directory(new File(root));

        return handleProcess(source, builder);
    }

    protected TextEdit handleProcess(String source, ProcessBuilder builder) {
        try {
            Process process = builder.start();
            OutputStreamWriter output = new OutputStreamWriter(process.getOutputStream());

            output.write(source);
            output.flush();
            output.close();

            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            InputStreamReader error = new InputStreamReader(process.getErrorStream());

            final char[] buffer = new char[1024];
            final StringBuilder stdout = new StringBuilder();
            final StringBuilder errout = new StringBuilder();

            for (;;) {
                int rsz = reader.read(buffer, 0, buffer.length);

                if (rsz < 0) {
                    break;
                }

                stdout.append(buffer, 0, rsz);
            }

            for (;;) {
                int rsz = error.read(buffer, 0, buffer.length);

                if (rsz < 0) {
                    break;
                }

                errout.append(buffer, 0, rsz);
            }

            int code = process.waitFor();
            if (code != 0) {
                err.println("clang-format return error (" + code + ").");
                err.println(errout.toString());
                return null;
            }

            if (errout.length() > 0) {
                err.println(errout.toString());
                return null;
            }

            String newSource = stdout.toString();
            if (0 == source.compareTo(newSource)) {
                err.println("0 == source.compareTo(newSource)");
                return null;
            }

            MultiTextEdit edit = new MultiTextEdit();
            diff_match_patch diff = new diff_match_patch();

            LinkedList<Diff> diffs = diff.diff_main(source, newSource);
            diff.diff_cleanupEfficiency(diffs);

            int offset = 0;
            for (Diff d : diffs) {
                switch (d.operation) {
                case INSERT:
                    InsertEdit e = new InsertEdit(offset, d.text);
                    edit.addChild(e);
                    break;
                case DELETE:
                    DeleteEdit e1 = new DeleteEdit(offset, d.text.length());
                    offset += d.text.length();
                    edit.addChild(e1);
                    break;
                case EQUAL:
                    offset += d.text.length();
                    break;
                }
            }
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

    private String getClangFormatConfigureFile(String path) {
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

    public static String getClangFormatPath() {
        return CppStyle.getDefault().getPreferenceStore().getString(CppStyleConstants.CLANG_FORMAT_PATH);
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

    private String getSourceFilePath() {
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
            return new File(root, "a.cc").getAbsolutePath();
        }
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

    @Override
    public TextEdit format(int kind, String source, int offset, int length, int arg4, String lineSeparator) {
        TextEdit retval = format(source, getSourceFilePath(), new Region(offset, length));
        return retval != null ? retval : new MultiTextEdit();
    }

    @Override
    public TextEdit format(int kind, String source, IRegion[] regions, int indentationLevel, String lineSeparator) {
        String message = "not yet implementeed: format with Regions[]";
        err.println(message);
        throw new RuntimeException(message);
    }
}
