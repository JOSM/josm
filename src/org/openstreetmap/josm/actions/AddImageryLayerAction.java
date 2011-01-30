// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.layer.ImageryLayer;

public class AddImageryLayerAction extends JosmAction {

    private final ImageryInfo info;

    public AddImageryLayerAction(ImageryInfo info) {
        super(info.getMenuName(), "imagery_menu", tr("Add imagery layer {0}",info.getName()), null, false, false);
        putValue("toolbar", "imagery_" + info.getToolbarName());
        this.info = info;
        installAdapters();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        Main.main.addLayer(ImageryLayer.create(info));
    }

    @Override
    protected void updateEnabledState() {
        // never enable blacklisted entries.
        if (info.isBlacklisted()) {
            setEnabled(false);
        } else if (info.getImageryType() == ImageryType.TMS || info.getImageryType() == ImageryType.BING) {
            setEnabled(true);
        } else if (Main.map != null && Main.map.mapView != null && !Main.map.mapView.getAllLayers().isEmpty()) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }
}
