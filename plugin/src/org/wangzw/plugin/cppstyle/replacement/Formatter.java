package org.wangzw.plugin.cppstyle.replacement;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.wangzw.plugin.cppstyle.CppStyle;
import org.wangzw.plugin.cppstyle.ui.CppStyleConstants;
import org.xml.sax.SAXException;

public class Formatter extends CodeFormatter {

    private Map<String, String> options;

    public Formatter() {
    }

    private String[] concat(String[] first, String[] second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private void logAndDialogError(String title, Exception e) {
        Logger.logError(title, e);
        ErrorDialog.openError(null, title, null, new Status(Status.ERROR, CppStyle.PLUGIN_ID, e.getMessage(), e));
    }

    @Override
    public TextEdit format(int kind, String source, int offset, int length, int indentationLevel,
            String lineSeparator) {
        return format(source, getSourceFilePath(), new Region(offset, length));
    }

    private TextEdit format(String source, String path, IRegion region) {
        IPreferenceStore prefs = CppStyle.getDefault().getPreferenceStore();
        Runtime RT = Runtime.getRuntime();
        String err = "";
        List<String> argList = Arrays.asList(new String[] {
                prefs.getString(CppStyleConstants.CLANG_FORMAT_PATH), "-output-replacements-xml",
        });

        if (region != null) {
            argList.add("-offset=" + region.getOffset());
            argList.add("-length=" + region.getLength());
        }
        String[] args = argList.toArray(new String[0]);

        String[] options;
        try {
            options = createOptions(path);
        }
        catch (Exception e) {
            logAndDialogError("Could not compile options for clang-format", e);
            return null;
        }
        if (options == null) {
            return null;
        }
        args = concat(args, options);
        Process subProc;
        try {
            subProc = RT.exec(args);
        }
        catch (IOException exception) {
            logAndDialogError("Could not execute command", exception);
            return null;
        }
        OutputStream outStream = subProc.getOutputStream();
        try {
            outStream.write(source.getBytes(Charset.forName("UTF-8")));
            outStream.flush();
            outStream.close();
        }
        catch (IOException exception) {
            logAndDialogError("Could not send file contents", exception);
            return null;
        }
        // read errors
        String line = null;
        InputStreamReader reader = new InputStreamReader(subProc.getErrorStream());
        BufferedReader br = new BufferedReader(reader);
        try {
            while ((line = br.readLine()) != null) {
                err += line + System.lineSeparator();
            }
        }
        catch (IOException exception) {
            logAndDialogError("Could not read from stderr", exception);
        }
        XMLReplacementHandler replacementHandler = new XMLReplacementHandler();
        try {
            // read the edits
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.newSAXParser().parse(subProc.getInputStream(), replacementHandler);
        }
        catch (IOException exception) {
            logAndDialogError("Could not read from stdout", exception);
        }
        catch (SAXException exception) {
            logAndDialogError("Could not parse xml", exception);
        }
        catch (ParserConfigurationException exception) {
            logAndDialogError("Parser problem", exception);
        }

        if (!err.isEmpty()) {
            logAndDialogError("clang-format call returned errors",
                    new Exception(String.format("%s\n\nfrom error stream for call %s", err, Arrays.toString(args))));
        }
        int textOffset = 0;
        int textLength = source.length();
        MultiTextEdit textEdit = new MultiTextEdit(); /* new MultiTextEdit(textOffset, textLength); */
        TextEdit edits[] = new TextEdit[0];
        edits = replacementHandler.getEdits().toArray(edits);

        if (edits.length != 0) {
            textEdit.addChildren(edits);
        }
        return textEdit;
    }

    public String[] createOptions(String path) throws Exception {
        String filePath = null;
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                filePath = file.getAbsolutePath();
            }
        }
        if (filePath == null) {
            filePath = getSourceFilePath();
        }
        if (filePath == null) {
            filePath = this.filePath();
        }
        if (filePath == null) {
            throw new Exception("Could not determine file path.");
        }
        return new String[] {
                "-style=file", "-assume-filename=" + filePath
        };
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
        System.err.println("Not yet implemented: getSourceFilePath from CompilationUnit");
        ICompilationUnit tu = null;
        if (tu != null) {
            return tu.getResource().getRawLocation().toOSString();
        }
        else {
            String root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
            return new File(root, "A.java").getAbsolutePath();
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

    // private String getTranslationUnitPath() {
    // // taken from org.eclipse.cdt.core.CCodeFormatter
    // ICompilationUnit tu =
    // (ICompilationUnit)options.get(DefaultCodeFormatterConstants.FORMATTER_TRANSLATION_UNIT);
    // if (tu == null) {
    // IFile file =
    // (IFile)options.get(DefaultCodeFormatterConstants.FORMATTER_CURRENT_FILE);
    // if (file != null) {
    // tu = (ITranslationUnit)CoreModel.getDefault().create(file);
    // }
    // }
    // return tu == null ? null : tu.getResource().getRawLocation().toOSString();
    // }

    private String filePath() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null) {
            return null;
        }
        IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
        if (workbenchWindow == null) {
            return null;
        }
        IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
        if (workbenchPage == null) {
            return null;
        }
        IWorkbenchPart workbenchPart = workbenchPage.getActivePart();
        if (workbenchPart == null) {
            return null;
        }
        IWorkbenchPartSite workbenchPartSite = workbenchPart.getSite();
        if (workbenchPartSite == null) {
            return null;
        }
        IWorkbenchPage page = workbenchPartSite.getPage();
        if (page == null) {
            return null;
        }
        IEditorPart activeEditor = page.getActiveEditor();
        if (activeEditor == null) {
            return null;
        }
        IEditorInput editorInput = activeEditor.getEditorInput();
        if (editorInput == null) {
            return null;
        }
        IFile file = editorInput.getAdapter(IFile.class);
        if (file == null) {
            return null;
        }
        return file.getRawLocation().toOSString();
    }

    @Override
    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    @Override
    public TextEdit format(int kind, String source, IRegion[] regions, int indentationLevel, String lineSeparator) {
        String message = "not yet implementeed: format with Regions[]";
        System.err.println(message);
        // throw new RuntimeException(message);
        return null;
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
}
