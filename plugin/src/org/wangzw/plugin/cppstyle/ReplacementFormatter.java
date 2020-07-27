package org.wangzw.plugin.cppstyle;

import static org.wangzw.plugin.cppstyle.replacement.Logger.*;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.wangzw.plugin.cppstyle.replacement.XMLReplacementHandler;
import org.xml.sax.SAXException;

public class ReplacementFormatter extends CodeFormatterBase {

    public ReplacementFormatter() {
        super();
    }

    @Override
    protected ProcessHandler createProcessHandler(String source) {

        return new ProcessHandler(source) {

            @Override
            protected void handleInputStream() {
                textEdit = createEdit(process);
            }
        };
    }

    protected MultiTextEdit createEdit(Process subProc) {
        XMLReplacementHandler replacementHandler = parseProcessOutput(subProc.getInputStream());
        MultiTextEdit textEdit = new MultiTextEdit();
        TextEdit edits[] = new TextEdit[0];
        edits = replacementHandler.getEdits().toArray(edits);

        if (edits.length != 0) {
            textEdit.addChildren(edits);
        }
        return textEdit;
    }

    private XMLReplacementHandler parseProcessOutput(InputStream inputStream) {
        XMLReplacementHandler replacementHandler = new XMLReplacementHandler();
        try {
            // read the edits
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.newSAXParser().parse(inputStream, replacementHandler);
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
        return replacementHandler;
    }

    private void logAndDialogError(String title, Exception e) {
        logError(title, e);
        ErrorDialog.openError(null, title, null, new Status(Status.ERROR, CppStyle.PLUGIN_ID, e.getMessage(), e));
    }

}
