// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ChildRelationBrowser} class.
 */
public class ChildRelationBrowserTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link ChildRelationBrowser#ChildRelationBrowser}.
     */
    @Test
    public void testChildRelationBrowser() {
        DataSet ds = new DataSet();
        Relation r = new Relation();
        ds.addPrimitive(r);
        assertNotNull(new ChildRelationBrowser(new OsmDataLayer(ds, "", null), r));
    }
}
