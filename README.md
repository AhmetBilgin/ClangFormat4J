ClangFormat4J
========================
**An Eclipse plugin that integrates the clang-format tool as an alternative Java code formatter.**

## Description
A consistent coding style is important for a project.Many developers use Eclipse as a Java IDE, but it is a little difficult to integrate an external formatter tool to Eclipse. People have to switch to a command line and run the tools to format the code.

The expected behavious is that people just format the code fragment by first selecting it and then pressing `Command + Shift + f` on MacOS or `Ctrl + Shift + f` on Linux and other systems. Further more, the coding style checker is run whenever a file is saved and all the issues are marked on the editor. That is exactly what ClangFormat4J does.

## Requirement
    clang-format   http://clang.llvm.org/docs/ClangFormat.html

### Install clang-format on Linux/MacOS
clanf-format can be built from llvm/clang source. But installing from binary is much easier.

## Installation
### Install from update site
TODO

### Manual

* Build ClangFormat4J with maven first. ```mvn clean verify```
* Install CppStyle with local update site ```file:///<YOUR_CODE_PATH>/update/target/site```

Restart Eclipse.

## Configure CppStyle


To enable ClangFormat4J(clang-format) as default Java code formatter, go to **Preferences -> Java -> Code Style -> Formatter** page and switch **"Code Formatter"** from **[built-in]** to **"ClangFormat4J (clang-format)"**

To enable ClangFormat4J(clang-format) as Java code formatter for a project, go to **Project properties -> Java General -> Formatter** page and switch **"Code Formatter"** from **[built-in]** to **"ClangFormat4J (clang-format)"**

## To configure clang-format

ClangFormat4J does not support appending command line parameters to clang-format. So, use their respective configuration files to do this.

ClangFormat4J will first try the provided clang-format style path (see PreferencePage). If the path is not valid it will then pass the full absolute path of the source file to clang-format in command line. And clang-format will try to find the configuration file named **.clang-format** in the source file's path, and its parent's path recursivly.

So put the configuration file **.clang-format** into the project's root direcotry can make it work for all source files in the project.

Further more, you can also add the configuration file **.clang-format** into Eclipse workspace root directory to make it work for all projects in the workspace.

To generate the clang-format configuration file **.clang-format**:

    clang-format -dump-config -style=Chromium> .clang-format

**If no configure file named .clang-format is found, "-style=Chromium" will be passed to clang-format and Chromium style will be used by default.**
