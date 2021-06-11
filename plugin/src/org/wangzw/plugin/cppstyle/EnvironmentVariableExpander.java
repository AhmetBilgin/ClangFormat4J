package org.wangzw.plugin.cppstyle;

import java.io.File;

public class EnvironmentVariableExpander {

    private static final String START_TAG_ENVIRONMENT_VAR = "${env_var:";

    private static final String END_TAG_ENVIRONMENT_VAR = "}";

    private static final int START_TAG_ENVIRONMENT_VAR_LENGTH = START_TAG_ENVIRONMENT_VAR.length();

    public static String environmentVariableOf(String environmentVariableName) {
        return environmentVariableOf(environmentVariableName, "");
    }
    public static String environmentVariableOf(String environmentVariableName, String postFix) {
        return String.format(
                "%s%s%s%s", START_TAG_ENVIRONMENT_VAR, environmentVariableName, END_TAG_ENVIRONMENT_VAR, postFix);
    }

    public static String expandEnvVar(String toExpand) {
        String resolvedEnvVar = "";

        if (toExpand == null || toExpand.length() == 0) {
            return resolvedEnvVar;
        }

        if (containsEnvironmentVariable(toExpand)) {
            final String envVar =
                    toExpand.substring(START_TAG_ENVIRONMENT_VAR_LENGTH, toExpand.lastIndexOf(END_TAG_ENVIRONMENT_VAR));
            resolvedEnvVar = System.getenv(envVar);
            if (resolvedEnvVar == null) {
                resolvedEnvVar = System.getProperty(envVar, "");
            }
            resolvedEnvVar += toExpand.substring(toExpand.lastIndexOf(END_TAG_ENVIRONMENT_VAR) + 1, toExpand.length());
            File file = new File(resolvedEnvVar);
            resolvedEnvVar = file.getAbsolutePath();
        }
        else {
            resolvedEnvVar = toExpand;
        }

        return resolvedEnvVar;
    }

    public static boolean containsEnvironmentVariable(String toExpand) {
        boolean containsEnvironmentVariable = false;
        if (toExpand.startsWith(START_TAG_ENVIRONMENT_VAR)) {
            if (toExpand.contains(END_TAG_ENVIRONMENT_VAR)) {
                containsEnvironmentVariable = true;
            }
            else {
                CppStyle.log("Environment variable definition does not end with a }");
            }
        }
        return containsEnvironmentVariable;
    }
}
