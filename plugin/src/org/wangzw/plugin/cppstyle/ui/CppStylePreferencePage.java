package org.wangzw.plugin.cppstyle.ui;

import java.io.File;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wangzw.plugin.cppstyle.CppStyle;

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

class CppStylePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    private FileFieldEditor clangFormatPath = null;

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
        clangFormatPath =
                new FileFieldEditor(CppStyleConstants.CLANG_FORMAT_PATH, "Clang-format path:", getFieldEditorParent());
        addField(clangFormatPath);
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);

        if (event.getProperty().equals(FieldEditor.VALUE)) {
            if (event.getSource() == clangFormatPath) {
                clangFormatPathChange(event.getNewValue().toString());
            }

            checkState();
        }
    }

    private boolean checkPathExist(String path) {
        File file = new File(path);
        return file.exists() && !file.isDirectory();
    }

    private void clangFormatPathChange(String newPath) {
        if (!checkPathExist(newPath)) {
            this.setValid(false);
            this.setErrorMessage("Clang-format path \"" + newPath + "\" does not exist");
        }
        else {
            this.setValid(true);
            this.setErrorMessage(null);
        }
    }
}