// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import javax.swing.JPanel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link GenericRelationEditor} class.
 */
public class GenericRelationEditorTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().platform().main();

    /**
     * Returns a new relation editor for unit tests.
     * @param orig relation
     * @param layer data layer
     * @return new relation editor for unit tests
     */
    public static IRelationEditor newRelationEditor(final Relation orig, final OsmDataLayer layer) {
        return new IRelationEditor() {
            private Relation r = orig;

            @Override
            public void setRelation(Relation relation) {
                r = relation;
            }

            @Override
            public boolean isDirtyRelation() {
                return false;
            }

            @Override
            public Relation getRelationSnapshot() {
                return r;
            }

            @Override
            public Relation getRelation() {
                return r;
            }

            @Override
            public void reloadDataFromRelation() {
                // Do nothing
            }

            @Override
            public OsmDataLayer getLayer() {
                return layer;
            }
        };
    }

    /**
     * Unit test of {@link GenericRelationEditor#addPrimitivesToRelation}.
     */
    @Test
    public void testAddPrimitivesToRelation() {
        Relation r = TestUtils.addFakeDataSet(new Relation(1));
        assertNull(GenericRelationEditor.addPrimitivesToRelation(r, Collections.<OsmPrimitive>emptyList()));
        assertNull(GenericRelationEditor.addPrimitivesToRelation(r, Collections.singleton(new Relation(1))));

        assertNotNull(GenericRelationEditor.addPrimitivesToRelation(r, Collections.singleton(new Node(1))));
        assertNotNull(GenericRelationEditor.addPrimitivesToRelation(r, Collections.singleton(new Way(1))));
        assertNotNull(GenericRelationEditor.addPrimitivesToRelation(r, Collections.singleton(new Relation(2))));
    }

    /**
     * Unit test of {@code GenericRelationEditor#build*} methods.
     */
    @Test
    public void testBuild() {
        DataSet ds = new DataSet();
        Relation relation = new Relation(1);
        ds.addPrimitive(relation);
        OsmDataLayer layer = new OsmDataLayer(ds, "test", null);
        IRelationEditor re = newRelationEditor(relation, layer);

        AutoCompletingTextField tfRole = GenericRelationEditor.buildRoleTextField(re);
        assertNotNull(tfRole);

        TagEditorPanel tagEditorPanel = new TagEditorPanel(relation, null);

        JPanel top = GenericRelationEditor.buildTagEditorPanel(tagEditorPanel);
        assertNotNull(top);
        assertNotNull(tagEditorPanel.getModel());
    }
}
