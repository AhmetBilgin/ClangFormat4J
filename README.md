ClangFormat4J
========================
**An Eclipse plugin that integrates the clang-format tool as an alternative Java code formatter.**

## Description
A consistent coding style is important for a project. As I experienced huge formatter changes with every Eclipse-IDE update and the eclipse formatter has some bugs which won't be fixed, I forked the CppStyle Eclipse Plugin to support clang-format for Java files.

You can format the Java source code by either
* Saving the file
* or Selecting the region to format or all of the java file content and then pressing `Ctrl + Shift + f` on Linux and other systems or `Command + Shift + f` on MacOS.
* or Context-Menu - > Source -> Format

## Requirement
    clang-format   http://clang.llvm.org/docs/ClangFormat.html

### Install clang-format on Linux/MacOS
clanf-format can be built from llvm/clang source. But installing from binary is much easier.

## Installation
### Install from update site
TODO

### Manual

* Build ClangFormat4J with maven first. ```mvn clean verify```
* Install ClangFormat4J with local update site ```file:///<YOUR_CODE_PATH>/update/target/site```

Restart Eclipse.

## Configure ClangFormat4J


To enable ClangFormat4J(clang-format) as default Java code formatter, go to **Preferences -> Java -> Code Style -> Formatter** page and switch **"Code Formatter"** from **[built-in]** to **"ClangFormat4J (clang-format)"**

To enable ClangFormat4J(clang-format) as Java code formatter for a project, go to **Project properties -> Java General -> Formatter** page and switch **"Code Formatter"** from **[built-in]** to **"ClangFormat4J (clang-format)"**

## To configure clang-format

ClangFormat4J does not support appending command line parameters to clang-format. So, use their respective configuration files to do this.

ClangFormat4J will first try the provided clang-format style path (see PreferencePage). If the path is not valid it will then pass the full absolute path of the source file to clang-format in command line. And clang-format will try to find the configuration file named **.clang-format** in the source file's path, and its parent's path recursivly.

So put the configuration file **.clang-format** into the project's root direcotry can make it work for all source files in the project.

Further more, you can also add the configuration file **.clang-format** into Eclipse workspace root directory to make it work for all projects in the workspace.

To generate the clang-format configuration file **.clang-format**:

    clang-format -dump-config -style=Chromium> .clang-format

**If no configuration file named .clang-format is found, "-style=Chromium" will be passed to clang-format and Chromium style will be used by default.**
