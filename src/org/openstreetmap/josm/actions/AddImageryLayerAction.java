// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.layer.ImageryLayer;

public class AddImageryLayerAction extends JosmAction implements AdaptableAction {

    private final ImageryInfo info;

    public AddImageryLayerAction(ImageryInfo info) {
        super(info.getMenuName(), /* ICON */"imagery_menu", tr("Add imagery layer {0}",info.getName()), null, false, false);
        putValue("toolbar", "imagery_" + info.getToolbarName());
        this.info = info;
        installAdapters();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            Main.main.addLayer(ImageryLayer.create(info));
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() == null || ex.getMessage().isEmpty()) {
                throw ex;
            } else {
                JOptionPane.showMessageDialog(Main.parent,
                        ex.getMessage(), tr("Error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    protected void updateEnabledState() {
        // never enable blacklisted entries.
        if (info.isBlacklisted()) {
            setEnabled(false);
        } else if (info.getImageryType() == ImageryType.TMS || info.getImageryType() == ImageryType.BING || info.getImageryType() == ImageryType.SCANEX) {
            setEnabled(true);
        } else if (Main.map != null && Main.map.mapView != null && !Main.map.mapView.getAllLayers().isEmpty()) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }
}
