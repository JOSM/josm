// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link SaveLayerTask} class.
 */
public class SaveLayerTaskTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test of {@link SaveLayerTask} class - null case.
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_NONVIRTUAL")
    public void testSaveLayerTaskNull() {
        new SaveLayerTask(null, null);
    }

    /**
     * Test of {@link SaveLayerTask} class - nominal case.
     */
    @Test
    public void testSaveLayerTaskNominal() {
        assertNotNull(new SaveLayerTask(new SaveLayerInfo(new OsmDataLayer(new DataSet(), "", null)), null));
    }
}
