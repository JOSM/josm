// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests for class {@link SelectAllAction}.
 */
@BasicPreferences
final class SelectAllActionTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules().projection().main();

    /**
     * Unit test of {@link SelectAllAction#actionPerformed} method.
     */
    @Test
    void testActionPerformed() {
        SelectByInternalPointActionTest.initDataSet();
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();

        assertEquals(0, ds.getSelected().size());
        new SelectAllAction().actionPerformed(null);
        assertEquals(6, ds.getSelected().size());
    }
}

