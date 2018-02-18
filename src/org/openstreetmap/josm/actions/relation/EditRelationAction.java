// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;

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
        Set<RelationMember> members = new HashSet<>();
        if (MainApplication.isDisplayingMapView()) {
            OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
            if (editLayer != null && editLayer.data != null) {
                Collection<OsmPrimitive> selection = editLayer.data.getSelected();
                for (RelationMember member: r.getMembers()) {
                    if (selection.contains(member.getMember())) {
                        members.add(member);
                    }
                }
            }
        }
        return members;
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
        if (!isEnabled() || relations.isEmpty()) return;
        if (relations.size() > Config.getPref().getInt("warn.open.maxrelations", 5) &&
            /* I18N english text for value 1 makes no real sense, never called for values <= maxrel (usually 5) */
            JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(Main.parent,
                    "<html>"+trn("You are about to open <b>{0}</b> different relation editor simultaneously.<br/>Do you want to continue?",
                            "You are about to open <b>{0}</b> different relation editors simultaneously.<br/>Do you want to continue?",
                            relations.size(), relations.size())+"</html>",
                    tr("Confirmation"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)) {
            return;
        }
        for (Relation r : relations) {
            launchEditor(r);
        }
    }

    @Override
    protected void updateEnabledState() {
        boolean enabled = false;
        if (relations.stream().map(r -> r.getDataSet()).noneMatch(DataSet::isReadOnly)) {
            for (Relation r : relations) {
                if (!r.isDeleted()) {
                    enabled = true;
                    break;
                }
            }
        }
        setEnabled(enabled);
    }
}
