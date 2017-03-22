// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.junit.Assert.assertEquals;

import java.beans.PropertyChangeEvent;

import javax.swing.JSplitPane;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AutoAdjustingSplitPane} class.
 */
public class AutoAdjustingSplitPaneTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link AutoAdjustingSplitPane} class.
     */
    @Test
    public void testAutoAdjustingSplitPane() {
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
