// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class ToggleUploadDiscouragedLayerAction extends JosmAction {

    private OsmDataLayer layer;
    
    public ToggleUploadDiscouragedLayerAction(OsmDataLayer layer) {
        super(tr("Encourage/discourage upload"), null, null, null, false);
        this.layer = layer;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        layer.setUploadDiscouraged(!layer.isUploadDiscouraged());
        LayerListDialog.getInstance().repaint();
    }
}
