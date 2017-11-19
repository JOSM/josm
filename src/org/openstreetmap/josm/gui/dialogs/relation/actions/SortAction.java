// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Sort the relation members
 * @since 9496
 */
public class SortAction extends AbstractRelationEditorAction {

    /**
     * Constructs a new {@code SortAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     */
    public SortAction(MemberTable memberTable, MemberTableModel memberTableModel) {
        super(memberTable, memberTableModel, null);
        new ImageProvider("dialogs", "sort").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Sort"));
        Shortcut sc = Shortcut.registerShortcut("relationeditor:sort", tr("Relation Editor: Sort"), KeyEvent.VK_END, Shortcut.ALT);
        sc.setAccelerator(this);
        putValue(SHORT_DESCRIPTION, Main.platform.makeTooltip(tr("Sort the relation members"), sc));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        memberTableModel.sort();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(memberTableModel.getRowCount() > 0);
    }
}
