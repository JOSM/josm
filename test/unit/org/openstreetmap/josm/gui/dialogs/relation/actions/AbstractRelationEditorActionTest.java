// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditorTest;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTable;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetHandler;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * This class provides the basic test environment for relation editor actions.
 * @author Michael Zangl
 */
@Disabled
@BasicPreferences
@Main
public abstract class AbstractRelationEditorActionTest {
    protected OsmDataLayer layer;

    private SelectionTableModel selectionTableModel;
    private SelectionTable selectionTable;
    private IRelationEditor editor;
    private MemberTable memberTable;
    private MemberTableModel memberTableModel;
    private AutoCompletingTextField tfRole;
    private TagEditorModel tagModel;

    protected final IRelationEditorActionAccess relationEditorAccess = new IRelationEditorActionAccess() {

        @Override
        public AutoCompletingTextField getTextFieldRole() {
            return tfRole;
        }

        @Override
        public TagEditorModel getTagModel() {
            return tagModel;
        }

        @Override
        public SelectionTableModel getSelectionTableModel() {
            return selectionTableModel;
        }

        @Override
        public SelectionTable getSelectionTable() {
            return selectionTable;
        }

        @Override
        public MemberTableModel getMemberTableModel() {
            return memberTableModel;
        }

        @Override
        public MemberTable getMemberTable() {
            return memberTable;
        }

        @Override
        public IRelationEditor getEditor() {
            return editor;
        }
    };

    /**
     * Set up the test data required for common tests using one relation.
     */
    @BeforeEach
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
        selectionTable = new SelectionTable(selectionTableModel, memberTableModel);
        editor = GenericRelationEditorTest.newRelationEditor(orig, layer);
        tfRole = new AutoCompletingTextField();
        tagModel = new TagEditorModel();
        memberTable = new MemberTable(layer, editor.getRelation(), memberTableModel);
    }
}
