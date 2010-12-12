// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.layer.ImageryLayer;

public class AddImageryLayerAction extends JosmAction {

    private final ImageryInfo info;

    public AddImageryLayerAction(ImageryInfo info) {
        super(info.getMenuName(), "imagery_menu", tr("Add imagery layer {0}",info.getName()), null, false);
        putValue("toolbar", "imagery_" + info.getToolbarName());
        this.info = info;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImageryLayer wmsLayer = ImageryLayer.create(info);
        Main.main.addLayer(wmsLayer);
    }
};
