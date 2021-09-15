// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.beans.PropertyChangeEvent;

import javax.swing.JSplitPane;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link AutoAdjustingSplitPane} class.
 */
class AutoAdjustingSplitPaneTest {
    /**
     * Unit test of {@link AutoAdjustingSplitPane} class.
     */
    @Test
    void testAutoAdjustingSplitPane() {
        AutoAdjustingSplitPane pane = new AutoAdjustingSplitPane(JSplitPane.VERTICAL_SPLIT);
        assertEquals(-1, pane.getDividerLocation());
        assertEquals(0, pane.getHeight());
        pane.propertyChange(new PropertyChangeEvent(this, null, null, null));
        pane.propertyChange(new PropertyChangeEvent(this, JSplitPane.DIVIDER_LOCATION_PROPERTY, null, 50));
        assertEquals(-1, pane.getDividerLocation());
        pane.setSize(10, 10);
        assertEquals(10, pane.getHeight());
        pane.propertyChange(new PropertyChangeEvent(this, JSplitPane.DIVIDER_LOCATION_PROPERTY, null, 50));
        pane.ancestorResized(null);
        pane.ancestorMoved(null);
        assertEquals(50, pane.getDividerLocation());
    }
}
