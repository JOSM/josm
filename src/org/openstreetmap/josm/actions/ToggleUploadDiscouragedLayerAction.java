// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * An action enabling/disabling the {@linkplain OsmDataLayer#setUploadDiscouraged(boolean) discouraged upload flag}
 * of the layer specified in the constructor.
 */
public class ToggleUploadDiscouragedLayerAction extends AbstractAction implements LayerAction {

    private final transient OsmDataLayer layer;

    /**
     * Constructs a new {@code ToggleUploadDiscouragedLayerAction}.
     * @param layer the layer for which to toggle the {@linkplain OsmDataLayer#setUploadDiscouraged(boolean) discouraged upload flag}
     */
    public ToggleUploadDiscouragedLayerAction(OsmDataLayer layer) {
        super(tr("Discourage upload"));
        new ImageProvider("no_upload").getResource().attachImageIcon(this, true);
        this.layer = layer;
        setEnabled(layer.isUploadable());
        putValue("help", ht("/Action/EncourageDiscourageUpload"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        layer.setUploadDiscouraged(!layer.isUploadDiscouraged());
        LayerListDialog.getInstance().repaint();
    }

    @Override
    public Component createMenuComponent() {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(this);
        item.setSelected(layer.isUploadDiscouraged() || !layer.isUploadable());
        return item;
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        return layers.size() == 1 && layers.get(0) instanceof OsmDataLayer;
    }
}
