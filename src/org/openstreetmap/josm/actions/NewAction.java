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
 * Creates a blank new OSM data layer.
 * @since 169
 */
public class NewAction extends JosmAction {

    /**
     * Constructs a {@code NewAction}.
     */
    public NewAction() {
        super(tr("New Layer"), "new", tr("Create a new map layer."),
                Shortcut.registerShortcut("system:new", tr("File: {0}", tr("New Layer")), KeyEvent.VK_N, Shortcut.CTRL), true);
        setHelpId(ht("/Action/NewLayer"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        getLayerManager().addLayer(new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null));
    }
}
