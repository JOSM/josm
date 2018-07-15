// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Sort the relation members
 * @since 9496
 */
public class SortAction extends AbstractRelationEditorAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code SortAction}.
     * @param editorAccess An interface to access the relation editor contents.
     */
    public SortAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE);
        new ImageProvider("dialogs", "sort").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Sort"));
        Shortcut sc = Shortcut.registerShortcut("relationeditor:sort", tr("Relation Editor: Sort"), KeyEvent.VK_END, Shortcut.ALT);
        sc.setAccelerator(this);
        putValue(SHORT_DESCRIPTION, Main.platform.makeTooltip(tr("Sort the relation members"), sc));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        editorAccess.getMemberTableModel().sort();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editorAccess.getMemberTableModel().getRowCount() > 0);
    }
}
