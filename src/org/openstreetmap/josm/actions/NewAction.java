// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;

public class NewAction extends JosmAction {

    public NewAction() {
        super(tr("New"), "new", tr("Create a new map."),
                Shortcut.registerShortcut("system:new", tr("File: {0}", tr("New")), KeyEvent.VK_N, Shortcut.GROUP_MENU), true);
    }

    public void actionPerformed(ActionEvent e) {
        Main.main.addLayer(new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null));
    }
}
