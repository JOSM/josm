// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.Assert.assertEquals;

import java.beans.PropertyChangeEvent;

import javax.swing.JSplitPane;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog.AutoAdjustingSplitPane;

/**
 * Unit tests of {@link CombinePrimitiveResolverDialog} class.
 */
public class CombinePrimitiveResolverDialogTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void init() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link CombinePrimitiveResolverDialog.AutoAdjustingSplitPane} class.
     */
    @Test
    public void testAutoAdjustingSplitPane() {
        AutoAdjustingSplitPane pane = new CombinePrimitiveResolverDialog.AutoAdjustingSplitPane(JSplitPane.VERTICAL_SPLIT);
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
