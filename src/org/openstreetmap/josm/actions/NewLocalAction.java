// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Creates a blank new local OSM data layer.
 * @since 169
 */
public class NewLocalAction extends JosmAction {

    /**
     * Constructs a {@code NewLocalAction}.
     */
    public NewLocalAction() {
        super(tr("New Local Layer"), "new_local", tr("Create a new local map layer."),
                Shortcut.registerShortcut("system:new_local", tr("File: {0}", tr("New Local Layer")), KeyEvent.VK_N, Shortcut.CTRL_SHIFT), true, false);
        setHelpId(ht("/Action/NewLocalLayer"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final OsmDataLayer layer = new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null);
        layer.setUploadDiscouraged(true);
        getLayerManager().addLayer(layer);
    }
}
