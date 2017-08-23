// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action that deletes selected objects.
 * @since 770
 */
public final class DeleteAction extends JosmAction {

    /**
     * Constructs a new {@code DeleteAction}.
     */
    public DeleteAction() {
        super(tr("Delete"), "dialogs/delete", tr("Delete selected objects."),
                Shortcut.registerShortcut("system:delete", tr("Edit: {0}", tr("Delete")), KeyEvent.VK_DELETE, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/Delete"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MapFrame map = MainApplication.getMap();
        if (!isEnabled() || !map.mapView.isActiveLayerVisible())
            return;
        map.mapModeDelete.doActionPerformed(e);
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
