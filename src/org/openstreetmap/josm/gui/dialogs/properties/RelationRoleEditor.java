// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem;
import org.openstreetmap.josm.tools.Utils;

/**
 * Quickly change the relation roles of the selected members.
 */
final class RelationRoleEditor {

    private RelationRoleEditor() {
    }

    /**
     * Shows an {{@linkplain JOptionPane#showInputDialog input dialog} in order to quickly change
     * the roles of the selected members.
     * @param relation the relation to edit
     * @param memberInfo the corresponding member info
     */
    static void editRole(Relation relation, PropertiesDialog.MemberInfo memberInfo) {
        if (MainApplication.getLayerManager().getActiveDataLayer().isLocked()) {
            return;
        }
        final Collection<RelationMember> members = Utils.filteredCollection(memberInfo.getRole(), RelationMember.class);
        final String oldRole = memberInfo.getRoleString();
        final DefaultNameFormatter formatter = DefaultNameFormatter.getInstance();
        final String newRole = JOptionPane.showInputDialog("<html>" + tr("Change role for {0} in relation {1}",
                formatter.formatAsHtmlUnorderedList(Utils.transform(members, RelationMember::getMember), 5),
                formatter.formatAsHtmlUnorderedList(relation)),
                oldRole);
        if (newRole == null || oldRole.equals(newRole) || KeyedItem.DIFFERENT_I18N.equals(newRole)) {
            return;
        }
        final List<RelationMember> newMembers = relation.getMembers();
        newMembers.replaceAll(m -> members.contains(m) ? new RelationMember(Utils.strip(newRole), m.getMember()) : m);
        UndoRedoHandler.getInstance().add(new ChangeMembersCommand(relation, newMembers));
    }
}
