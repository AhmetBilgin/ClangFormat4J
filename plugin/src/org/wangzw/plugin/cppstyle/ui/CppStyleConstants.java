package org.wangzw.plugin.cppstyle.ui;

/**
 * Constant definitions for plug-in
 */
public class CppStyleConstants {
    public static final String PREFERENCE_PAGE_ID = "org.wangzw.plugin.cppstyle.ui.CppStylePreferencePage";

    public static final String CLANG_FORMAT_PATH = "cppstyle.clangformat.path";

    public static final String PROJECTS_PECIFIC_PROPERTY = "cppstyle.ENABLE_PROJECTS_PECIFIC";

    public static final String CPPLINT_PROJECT_ROOT = "cppstyle.PROJECT_ROOT";

    public static final String CONSOLE_NAME = "CppStyle Output";

    public static final String CPPLINT_OUTPUT_PATTERN = "(.+)\\:(\\d+)\\:(.+)\\[(.+)/(.+)\\](.*)\\[(\\d)\\]";

    public static final int CPPLINT_OUTPUT_PATTERN_LINE_NO_GROUP = 2;

}
