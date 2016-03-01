// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.Assert.assertEquals;

import javax.swing.JTree;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Relation;

/**
 * Unit tests of {@link RelationTreeCellRenderer} class.
 */
public class RelationTreeCellRendererTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link RelationTreeCellRenderer#RelationTreeCellRenderer}.
     */
    @Test
    public void testRelationTreeCellRenderer() {
        RelationTreeCellRenderer r = new RelationTreeCellRenderer();
        assertEquals(r, r.getTreeCellRendererComponent(new JTree(), new Relation(), false, false, false, 0, false));
    }
}
