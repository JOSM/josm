// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * An action enabling/disabling the {@linkplain OsmDataLayer#setUploadDiscouraged(boolean) discouraged upload flag}
 * of the layer specified in the constructor.
 */
public class ToggleUploadDiscouragedLayerAction extends AbstractAction {

    private final transient OsmDataLayer layer;

    /**
     * Constructs a new {@code ToggleUploadDiscouragedLayerAction}.
     * @param layer the layer for which to toggle the {@linkplain OsmDataLayer#setUploadDiscouraged(boolean) discouraged upload flag}
     */
    public ToggleUploadDiscouragedLayerAction(OsmDataLayer layer) {
        super(tr("Encourage/discourage upload"), ImageProvider.get("no_upload"));
        this.layer = layer;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        layer.setUploadDiscouraged(!layer.isUploadDiscouraged());
        LayerListDialog.getInstance().repaint();
    }
}
