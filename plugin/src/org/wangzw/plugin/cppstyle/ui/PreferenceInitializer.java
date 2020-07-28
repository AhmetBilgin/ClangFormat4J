package org.wangzw.plugin.cppstyle.ui;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.wangzw.plugin.cppstyle.CppStyle;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = CppStyle.getDefault().getPreferenceStore();
        store.setDefault(CppStyleConstants.CLANG_FORMAT_PATH, "");
        store.setDefault(CppStyleConstants.CLANG_FORMAT_STYLE_PATH, "");
    }
}
