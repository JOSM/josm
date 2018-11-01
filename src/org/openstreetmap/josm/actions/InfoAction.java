// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.dialogs.InspectPrimitiveDialog;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Display advanced object information about OSM nodes, ways, or relations.
 * @since 1697
 */
public class InfoAction extends JosmAction {

    /**
     * Constructs a new {@code InfoAction}.
     */
    public InfoAction() {
        super(tr("Advanced info"), "info",
            tr("Display advanced object information about OSM nodes, ways, or relations."),
            Shortcut.registerShortcut("core:info",
                tr("Advanced info"), KeyEvent.VK_I, Shortcut.CTRL),
            true, "action/info", true);
        setHelpId(ht("/Action/InfoAboutElements"));
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        OsmData<?, ?, ?, ?> set = getLayerManager().getActiveData();
        if (set != null) {
            new InspectPrimitiveDialog(set.getAllSelected(), set).showDialog();
        }
    }

    @Override
    public void updateEnabledState() {
        updateEnabledStateOnCurrentSelection(true);
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(!selection.isEmpty());
    }
}
