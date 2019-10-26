// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * The action for editing a relation.
 * @since 5793
 */
public class EditRelationAction extends AbstractRelationAction {

    /**
     * Constructs a new <code>EditRelationAction</code>.
     */
    public EditRelationAction() {
        putValue(NAME, tr("Edit"));
        putValue(SHORT_DESCRIPTION, tr("Call relation editor for selected relation"));
        new ImageProvider("dialogs", "edit").getResource().attachImageIcon(this, true);
    }

    /**
     * Returns the set of currently selected relation members for the given relation.
     * @param r The relation to inspect
     * @return The set of currently selected relation members for the given relation.
     */
    public static Set<RelationMember> getMembersForCurrentSelection(Relation r) {
        if (MainApplication.isDisplayingMapView()) {
            OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
            if (editLayer != null && editLayer.data != null) {
                Collection<OsmPrimitive> selection = editLayer.data.getSelected();
                return r.getMembers().stream().filter(m -> selection.contains(m.getMember())).collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }

    /**
     * Launches relation editor for the given relation.
     * @param toEdit The relation to edit
     */
    public static void launchEditor(Relation toEdit) {
        if (toEdit == null || toEdit.isDeleted() || !MainApplication.isDisplayingMapView()) return;
        RelationEditor.getEditor(MainApplication.getLayerManager().getEditLayer(), toEdit,
                getMembersForCurrentSelection(toEdit)).setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SubclassFilteredCollection<IRelation<?>, Relation> filteredRelations = Utils.filteredCollection(relations, Relation.class);
        if (!isEnabled() || filteredRelations.isEmpty()) return;
        if (filteredRelations.size() > Config.getPref().getInt("warn.open.maxrelations", 5) &&
            /* I18N english text for value 1 makes no real sense, never called for values <= maxrel (usually 5) */
            JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(MainApplication.getMainFrame(),
                    "<html>"+trn("You are about to open <b>{0}</b> different relation editor simultaneously.<br/>Do you want to continue?",
                            "You are about to open <b>{0}</b> different relation editors simultaneously.<br/>Do you want to continue?",
                            filteredRelations.size(), filteredRelations.size())+"</html>",
                    tr("Confirmation"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)) {
            return;
        }
        for (Relation r : filteredRelations) {
            launchEditor(r);
        }
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(canModify());
    }
}
