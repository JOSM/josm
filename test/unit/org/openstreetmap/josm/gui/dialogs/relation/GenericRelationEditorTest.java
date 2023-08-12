// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Container;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.ReflectionSupport;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.RelationListDialog;
import org.openstreetmap.josm.gui.dialogs.relation.actions.AddSelectedAtStartAction;
import org.openstreetmap.josm.gui.dialogs.relation.actions.IRelationEditorActionAccess;
import org.openstreetmap.josm.gui.dialogs.relation.actions.PasteMembersAction;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetHandler;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;

/**
 * Unit tests of {@link GenericRelationEditor} class.
 */
@BasicPreferences
@Main
@Projection
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

    @BeforeEach
    void setup() {
        new PasteMembersActionMock();
        new WindowMocker();
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

    /**
     * Ensure that users can create new relations and modify them.
     */
    @Test
    void testNonRegression23116() {
        // Setup the mocks
        final AtomicReference<RelationEditor> editorReference = new AtomicReference<>();
        new MockUp<RelationEditor>() {
            @Mock public RelationEditor getEditor(Invocation invocation, OsmDataLayer layer, Relation r,
                    Collection<RelationMember> selectedMembers) {
                editorReference.set(invocation.proceed(layer, r, selectedMembers));
                return editorReference.get();
            }
        };
        // We want to go through the `setVisible` code, just in case. So we have to mock the window location
        new MockUp<GenericRelationEditor>() {
            @Mock public void setVisible(boolean visible) {
                // Do nothing. Ideally, we would just mock the awt methods called, but that would take a lot of mocking.
            }
        };
        // Set up the data
        final DataSet dataSet = new DataSet();
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(dataSet, "GenericRelationEditorTest.testNonRegression23116", null));
        dataSet.addPrimitive(TestUtils.newNode(""));
        dataSet.setSelected(dataSet.allPrimitives());
        final RelationListDialog relationListDialog = new RelationListDialog();
        try {
            final Action newAction = ((SideButton) getComponent(relationListDialog, 2, 0, 0)).getAction();
            assertEquals("class org.openstreetmap.josm.gui.dialogs.RelationListDialog$NewAction",
                    newAction.getClass().toString());
            // Now get the buttons we want to push
            newAction.actionPerformed(null);
            final GenericRelationEditor editor = assertInstanceOf(GenericRelationEditor.class, editorReference.get());
            final JButton okAction = getComponent(editor, 0, 1, 0, 2, 0);
            assertEquals(tr("Delete"), okAction.getText(), "OK is Delete until the relation actually has data");
            assertNotNull(editor);
            final TagEditorPanel tagEditorPanel = getComponent(editor, 0, 1, 0, 1, 0, 0, 1, 1);
            // We need at least one tag for the action to not be "Delete".
            tagEditorPanel.getModel().add("type", "someUnknownTypeHere");
            final Action addAtStartAction = assertInstanceOf(AddSelectedAtStartAction.class,
                    ((JButton) getComponent(editor, 0, 1, 0, 1, 0, 0, 2, 0, 2, 1, 2, 0, 0)).getAction());
            // Perform the actual test.
            assertDoesNotThrow(() -> addAtStartAction.actionPerformed(null));
            assertDoesNotThrow(() -> okAction.getAction().actionPerformed(null));
            assertFalse(dataSet.getRelations().isEmpty());
            assertSame(dataSet.getNodes().iterator().next(),
                    dataSet.getRelations().iterator().next().getMember(0).getNode());
        } finally {
            // This avoids an issue with the cleanup code and the mocks for this test
            if (editorReference.get() != null) {
                RelationDialogManager.getRelationDialogManager().windowClosed(new WindowEvent(editorReference.get(), 0));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Container> T getComponent(Container parent, int... tree) {
        Container current = parent;
        for (int i : tree) {
            current = (Container) current.getComponent(i);
        }
        return (T) current;
    }

    private static class PasteMembersActionMock extends MockUp<PasteMembersAction> {
        @Mock
        protected void updateEnabledState() {
            // Do nothing
        }
    }
}
