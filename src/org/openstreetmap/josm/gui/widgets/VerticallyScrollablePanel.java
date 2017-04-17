// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import org.openstreetmap.josm.gui.util.GuiHelper;

public class VerticallyScrollablePanel extends JPanel implements Scrollable {

    /**
     * Constructs a new {@code VerticallyScrollablePanel}.
     */
    public VerticallyScrollablePanel() {
        super();
    }

    /**
     * Constructs a new {@code VerticallyScrollablePanel}.
     * @param isDoubleBuffered  a boolean, true for double-buffering, which
     *        uses additional memory space to achieve fast, flicker-free updates
     */
    public VerticallyScrollablePanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    /**
     * Constructs a new {@code VerticallyScrollablePanel}.
     * @param layout  the LayoutManager to use
     * @param isDoubleBuffered  a boolean, true for double-buffering, which
     *        uses additional memory space to achieve fast, flicker-free updates
     */
    public VerticallyScrollablePanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    /**
     * Constructs a new {@code VerticallyScrollablePanel}.
     * @param layout  the LayoutManager to use
     */
    public VerticallyScrollablePanel(LayoutManager layout) {
        super(layout);
    }

    /**
     * Returns a vertical scrollable {@code JScrollPane} containing this panel.
     * @return the vertical scrollable {@code JScrollPane}
     * @since 6666
     */
    public final JScrollPane getVerticalScrollPane() {
        return GuiHelper.embedInVerticalScrollPane(this);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10;
    }
}
