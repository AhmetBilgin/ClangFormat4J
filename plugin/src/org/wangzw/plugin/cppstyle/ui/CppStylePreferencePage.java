package org.wangzw.plugin.cppstyle.ui;

import static org.wangzw.plugin.cppstyle.ui.CppStyleConstants.*;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wangzw.plugin.cppstyle.CppStyle;
import org.wangzw.plugin.cppstyle.FilePathUtil;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class CppStylePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private FileFieldEditor clangFormatPath = null;

    private FileFieldEditor clangFormatStylePath = null;

    private static String validClangFormatPath = null;

    private static String validClangFormatStylePath = null;

    public CppStylePreferencePage() {
        super(GRID);
        setPreferenceStore(CppStyle.getDefault().getPreferenceStore());
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common GUI
     * blocks needed to manipulate various types of preferences. Each field editor
     * knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        clangFormatPath = createClangPathEditorField();
        addField(clangFormatPath);
        clangFormatStylePath = createClangFormatStylePathEditorField();
        addField(clangFormatStylePath);
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getProperty().equals(FieldEditor.VALUE)) {
            String newValue = event.getNewValue().toString();
            if (event.getSource() == clangFormatPath) {
                pathChange(LABEL_CLANG_FORMAT_PATH, newValue, FilePathUtil::isFileRunnable);
            }
            else if (event.getSource() == clangFormatStylePath) {
                pathChange(LABEL_CLANG_FORMAT_STYLE_PATH, newValue, FilePathUtil::fileExists);
            }
            checkState();
        }
    }

    public static String getValidClangFormatPath() {
        return validClangFormatPath;
    }

    public static String getValidClangFormatStylePath() {
        return validClangFormatStylePath;
    }

    private void pathChange(String propertyLable, String newPath, Predicate<? super String> predicate) {
        List<String> newPathCandidates = FilePathUtil.resolvePaths(newPath);
        Optional<String> validPath = newPathCandidates.stream().filter(predicate).findFirst();
        boolean validCandidateExists = validPath.isPresent();
        if (validCandidateExists) {
            this.setValid(true);
            this.setErrorMessage(null);
            if (LABEL_CLANG_FORMAT_PATH.equals(propertyLable)) {
                validClangFormatPath = validPath.get();
            }
            else if (LABEL_CLANG_FORMAT_STYLE_PATH.equals(propertyLable)) {
                validClangFormatStylePath = validPath.get();
            }
        }
        else {
            this.setValid(false);
            this.setErrorMessage(propertyLable + " None of the candidates exist \"" + newPath + "\"");
        }
    }

    private FileFieldEditor createClangPathEditorField() {
        return createFileFieldEditorWithEnvironmentVariableSupport(
                CLANG_FORMAT_PATH, LABEL_CLANG_FORMAT_PATH, getFieldEditorParent());
    }

    private FileFieldEditor createClangFormatStylePathEditorField() {
        return createFileFieldEditorWithEnvironmentVariableSupport(
                CLANG_FORMAT_STYLE_PATH, LABEL_CLANG_FORMAT_STYLE_PATH, getFieldEditorParent());
    }

    private FileFieldEditor createFileFieldEditorWithEnvironmentVariableSupport(
            String preferenceName, String label, Composite parentComposite) {
        return new FileFieldEditor(preferenceName, label, parentComposite) {
            @Override
            protected boolean checkState() {
                String pathCandidates = getTextControl().getText();
                String msg = checkPathCandidates(pathCandidates);

                if (msg != null) { // error
                    showErrorMessage(msg);
                    return false;
                }

                if (doCheckState()) { // OK!
                    clearErrorMessage();
                    return true;
                }
                msg = getErrorMessage(); // subclass might have changed it in the #doCheckState()
                if (msg != null) {
                    showErrorMessage(msg);
                }
                return false;
            }

            private String checkPathCandidates(String pathCandidates) {
                String msg = null;
                List<String> resolvedPathCandidates = FilePathUtil.resolvePaths(pathCandidates);
                for (String candidate : resolvedPathCandidates) {
                    if (candidate.isEmpty()) {
                        if (!isEmptyStringAllowed()) {
                            msg = getErrorMessage();
                        }
                    }
                    else {
                        File file = new File(candidate);
                        if (file.isFile()) {
                            if (!file.isAbsolute()) {
                                msg = JFaceResources.getString("FileFieldEditor.errorMessage2"); //$NON-NLS-1$
                            }
                            // is valid
                            else {
                                msg = null;
                                break;
                            }
                        }
                        else {
                            msg = getErrorMessage();
                        }
                    }
                }
                return msg;
            }
        };
    }
}