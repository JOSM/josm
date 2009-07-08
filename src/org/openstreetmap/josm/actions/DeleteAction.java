// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.SelectionChangedListener;

public final class DeleteAction extends JosmAction implements SelectionChangedListener {

    public DeleteAction() {
        super(tr("Delete"), "dialogs/delete", tr("Delete selected objects."),
        Shortcut.registerShortcut("system:delete", tr("Edit: {0}", tr("Delete")), KeyEvent.VK_DELETE, Shortcut.GROUP_DIRECT), true);
        DataSet.selListeners.add(this);
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        if(!Main.map.mapView.isActiveLayerVisible())
            return;
        new org.openstreetmap.josm.actions.mapmode.DeleteAction(Main.map)
                .doActionPerformed(e);
    }
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        setEnabled(! newSelection.isEmpty());
    }
}
