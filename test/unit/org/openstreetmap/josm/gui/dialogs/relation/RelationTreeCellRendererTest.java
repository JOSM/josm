// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.Assert.assertEquals;

import javax.swing.JTree;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RelationTreeCellRenderer} class.
 */
public class RelationTreeCellRendererTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link RelationTreeCellRenderer#RelationTreeCellRenderer}.
     */
    @Test
    public void testRelationTreeCellRenderer() {
        RelationTreeCellRenderer r = new RelationTreeCellRenderer();
        assertEquals(r, r.getTreeCellRendererComponent(new JTree(), new Relation(), false, false, false, 0, false));
    }
}
