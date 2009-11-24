// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class OsmDataParsingException extends SAXException {
    private int columnNumber;
    private int lineNumber;

    public OsmDataParsingException() {
        super();
    }

    public OsmDataParsingException(Exception e) {
        super(e);
    }

    public OsmDataParsingException(String message, Exception e) {
        super(message, e);
    }

    public OsmDataParsingException(String message) {
        super(message);
    }

    public OsmDataParsingException rememberLocation(Locator locator) {
        if (locator == null) return this;
        this.columnNumber = locator.getColumnNumber();
        this.lineNumber = locator.getLineNumber();
        return this;
    }

    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (lineNumber == 0 && columnNumber == 0)
            return msg;
        if (msg == null) {
            msg = getClass().getName();
        }
        msg = msg + " " + tr("(at line {0}, column {1})", lineNumber, columnNumber);
        return msg;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
