// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SelectAllAction}.
 */
public final class SelectAllActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules().preferences().projection().main();

    /**
     * Unit test of {@link SelectAllAction#actionPerformed} method.
     */
    @Test
    public void testActionPerformed() {
        SelectByInternalPointActionTest.initDataSet();
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();

        assertEquals(0, ds.getSelected().size());
        new SelectAllAction().actionPerformed(null);
        assertEquals(6, ds.getSelected().size());
    }
}
