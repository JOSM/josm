// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.Shortcut;

public final class DeleteAction extends JosmAction {

    public DeleteAction() {
        super(tr("Delete"), "dialogs/delete", tr("Delete selected objects."),
                Shortcut.registerShortcut("system:delete", tr("Edit: {0}", tr("Delete")), KeyEvent.VK_DELETE, Shortcut.GROUP_DIRECT), true);
        putValue("help", ht("/Action/Delete"));
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        if(!Main.map.mapView.isActiveLayerVisible())
            return;
        new org.openstreetmap.josm.actions.mapmode.DeleteAction(Main.map)
        .doActionPerformed(e);
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
