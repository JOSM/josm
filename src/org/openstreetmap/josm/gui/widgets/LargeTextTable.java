// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.FontMetrics;
import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Table with a scrolling behavior that is better suited for cells with large amounts of text.
 * <p>
 * The scrolling in the {@link javax.swing.JTable} is well suited for tables with rows of small and constant height.
 * If the height of the rows varies greatly or if some cells contain a lot of text, the scrolling becomes messy.
 * <p>
 * This class {@code LargeTextTable} has the same scrolling behavior as {@link javax.swing.JTextArea}:
 * scrolling increments depend on the font size.
 */
public class LargeTextTable extends JTable {

    private int fontHeight;
    private int charWidth;

    public LargeTextTable(TableModel tableModel, TableColumnModel tableColumnModel) {
        super(tableModel, tableColumnModel);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        switch (orientation) {
            case SwingConstants.VERTICAL:
                return getFontHeight();
            case SwingConstants.HORIZONTAL:
                return getCharWidth();
            default:
                throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        switch (orientation) {
            case SwingConstants.VERTICAL:
                return visibleRect.height;
            case SwingConstants.HORIZONTAL:
                return visibleRect.width;
            default:
                throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
    }

    private int getFontHeight() {
        if (fontHeight == 0) {
            FontMetrics fontMetrics = getFontMetrics(getFont());
            fontHeight = fontMetrics.getHeight();
        }
        return fontHeight;
    }

    // see javax.swing.JTextArea#getColumnWidth()
    private int getCharWidth() {
        if (charWidth == 0) {
            FontMetrics fontMetrics = getFontMetrics(getFont());
            charWidth = fontMetrics.charWidth('m');
        }
        return charWidth;
    }
}
