// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collection;
import java.util.Collections;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.ReflectionSupport;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.PasteMembersAction;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetHandler;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;

/**
 * Unit tests of {@link GenericRelationEditor} class.
 */
@BasicPreferences
@Main
public class GenericRelationEditorTest {
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
    void testAddPrimitivesToRelation() {
        TestUtils.assumeWorkingJMockit();
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker();

        Relation r = TestUtils.addFakeDataSet(new Relation(1));
        assertNull(GenericRelationEditor.addPrimitivesToRelation(r, Collections.<OsmPrimitive>emptyList()));
        jopsMocker.getMockResultMap().put(
            "<html>You are trying to add a relation to itself.<br><br>This generates a circular dependency of parent/child elements "
            + "and is therefore discouraged.<br>Skipping relation 'incomplete'.</html>",
            JOptionPane.OK_OPTION
        );

        assertNull(GenericRelationEditor.addPrimitivesToRelation(r, Collections.singleton(new Relation(1))));

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.OK_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Warning", invocationLogEntry[2]);

        assertNotNull(GenericRelationEditor.addPrimitivesToRelation(r, Collections.singleton(new Node(1))));
        assertNotNull(GenericRelationEditor.addPrimitivesToRelation(r, Collections.singleton(new Way(1))));
        assertNotNull(GenericRelationEditor.addPrimitivesToRelation(r, Collections.singleton(new Relation(2))));

        assertEquals(1, jopsMocker.getInvocationLog().size());
    }

    /**
     * Unit test of {@code GenericRelationEditor#build*} methods.
     * <p>
     * This test only tests if they do not throw exceptions.
     */
    @Test
    void testBuild() {
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

    @Test
    void testNonRegression23091() throws Exception {
        new MockUp<PasteMembersAction>() {
            @Mock
            protected void updateEnabledState() {
                // Do nothing
            }
        };
        new WindowMocker();

        DataSet ds = new DataSet();
        Relation relation = new Relation(1);
        ds.addPrimitive(relation);
        OsmDataLayer layer = new OsmDataLayer(ds, "test", null);

        final GenericRelationEditor gr = new GenericRelationEditor(layer, relation, Collections.emptyList());
        final IRelationEditorActionAccess iAccess = (IRelationEditorActionAccess)
                ReflectionSupport.tryToReadFieldValue(GenericRelationEditor.class.getDeclaredField("actionAccess"), gr)
                        .get();
        final TaggingPresetHandler handler = (TaggingPresetHandler)
                ReflectionSupport.tryToReadFieldValue(MemberTableModel.class.getDeclaredField("presetHandler"), iAccess.getMemberTableModel())
                        .get();
        final Collection<OsmPrimitive> selection = handler.getSelection();
        assertEquals(1, selection.size());
        assertSame(relation, selection.iterator().next(), "The selection should be the same");
    }
}
