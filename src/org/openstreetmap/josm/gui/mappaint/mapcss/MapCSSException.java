// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

/**
 * MapCSS parsing error, with line/columnn information in error message.
 */
public class MapCSSException extends RuntimeException {

    /** line number at which the parse error occurred */
    protected Integer line;
    /** column number at which the parse error occurred */
    protected Integer column;

    /**
     * Constructs a new {@code MapCSSException} with an explicit error message.
     * @param specialmessage error message
     */
    public MapCSSException(String specialmessage) {
        super(specialmessage);
    }

    /**
     * Constructs a new {@code MapCSSException} with a cause.
     * @param cause the root cause
     * @since 11562
     */
    public MapCSSException(Throwable cause) {
        super(cause);
    }

    /**
     * Sets the column number at which the parse error occurred.
     * @param column the column number at which the parse error occurred
     */
    public void setColumn(int column) {
        this.column = column;
    }

    /**
     * Sets the line number at which the parse error occurred.
     * @param line the line number at which the parse error occurred
     */
    public void setLine(int line) {
        this.line = line;
    }

    @Override
    public String getMessage() {
        if (line == null || column == null)
            return super.getMessage();
        return String.format("Error at line %s, column %s: %s", line, column, super.getMessage());
    }
}
