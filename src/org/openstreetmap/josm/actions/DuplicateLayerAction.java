// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;

public class DuplicateLayerAction extends JosmAction {

    public DuplicateLayerAction() {
        super(tr("Duplicate Layer"), "dialogs/duplicatelayer", tr("Make a duplicate of the currently selected layer."),
                Shortcut.registerShortcut("layer:duplicate", tr("Layer: {0}", tr("Duplicate")), KeyEvent.VK_N, Shortcut.GROUP_NONE), true);
        putValue("help", ht("/Action/DuplicateLayer"));
    }

    public void actionPerformed(ActionEvent e) {
        Layer sourceLayer = Main.main.getEditLayer();
        if (sourceLayer == null)
            return;
        duplicate(sourceLayer);
    }

    public void duplicate(Layer layer) {
        if ((Main.map == null) || (Main.map.mapView == null))
            return;
        List<String> layerNames = new ArrayList<String>();
        for (Layer l: Main.map.mapView.getAllLayers()) {
            layerNames.add(l.getName());
        }
        if (layer instanceof OsmDataLayer) {
            OsmDataLayer oldLayer = (OsmDataLayer)layer;
            // Translators: "Copy of {layer name}"
            String newName = tr("Copy of {0}", oldLayer.getName());
            int i = 2;
            while (layerNames.contains(newName)) {
                // Translators: "Copy {number} of {layer name}"
                newName = tr("Copy {1} of {0}", oldLayer.getName(), i);
                i++;
            }
            Main.main.addLayer(new OsmDataLayer(oldLayer.data.clone(), newName, null));
        }
    }

    public static boolean canDuplicate(Layer layer) {
        if (layer instanceof OsmDataLayer)
            return true;
        return false;
    }
}
