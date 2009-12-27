// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action synchronizes a set of primitives with their state on the server.
 *
 */
public class UpdateModifiedAction extends UpdateSelectionAction {

    /**
     * constructor
     */
    public UpdateModifiedAction() {
        super(tr("Update modified"),
                "updatemodified",
                tr("Updates the currently modified objects from the server (re-downloads data)"),
                Shortcut.registerShortcut("file:updatemodified",
                        tr("Update modified"),
                        KeyEvent.VK_M,
                        Shortcut.GROUP_HOTKEY + Shortcut.GROUPS_ALT2),
                        true);
        putValue("help", ht("UpdateModified"));
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        super.updateEnabledState(getData());
    }

    @Override
    protected Collection<OsmPrimitive> getData() {
        return getCurrentDataSet().allModifiedPrimitives();
    }
}
