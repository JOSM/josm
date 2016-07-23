// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditorTest;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetHandler;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class provides the basic test environment for relation editor actions.
 * @author Michael Zangl
 */
@Ignore
public abstract class AbstractRelationEditorActionTest {
    /**
     * Platform for tooltips.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().platform();

    protected SelectionTableModel selectionTableModel;

    protected IRelationEditor editor;

    protected MemberTable memberTable;

    protected OsmDataLayer layer;

    protected MemberTableModel memberTableModel;

    /**
     * Set up the test data required for common tests using one relation.
     */
    @Before
    public void setupTestData() {
        DataSet ds = new DataSet();
        final Relation orig = new Relation(1);
        ds.addPrimitive(orig);
        layer = new OsmDataLayer(ds, "test", null);
        memberTableModel = new MemberTableModel(orig, layer, new TaggingPresetHandler() {
            @Override
            public void updateTags(List<Tag> tags) {
            }

            @Override
            public Collection<OsmPrimitive> getSelection() {
                return Collections.<OsmPrimitive>singleton(orig);
            }
        });
        selectionTableModel = new SelectionTableModel(layer);

        editor = GenericRelationEditorTest.newRelationEditor(orig, layer);

        memberTable = new MemberTable(layer, editor.getRelation(), memberTableModel);
    }
}
