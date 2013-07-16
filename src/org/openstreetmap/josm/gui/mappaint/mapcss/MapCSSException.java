// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

public class MapCSSException extends RuntimeException {

    protected String specialmessage;
    protected Integer line;
    protected Integer column;

    public MapCSSException(String specialmessage) {
        this.specialmessage = specialmessage;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setLine(int line) {
        this.line = line;
    }

    @Override
    public String getMessage() {
        if (line == null || column == null)
            return specialmessage;
        return String.format("Error at line %s, column %s: %s", line, column, specialmessage);
    }
}
