// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Shortcut;

public class MergeLayerAction extends AbstractMergeAction {

    public MergeLayerAction() {
        super(tr("Merge layer"), "dialogs/mergedown", tr("Merge the current layer into another layer"), Shortcut
                .registerShortcut("system:merge", tr("Edit: {0}", tr("Merge")), KeyEvent.VK_M, Shortcut.GROUP_MENU),
                true /* register */
        );
        putValue("help", ht("/Action/MergeLayer"));
    }

    public void merge(Layer sourceLayer) {
        if (sourceLayer == null)
            return;
        List<Layer> targetLayers = LayerListDialog.getInstance().getModel().getPossibleMergeTargets(sourceLayer);
        if (targetLayers.isEmpty()) {
            warnNoTargetLayersForSourceLayer(sourceLayer);
            return;
        }
        Layer targetLayer = askTargetLayer(targetLayers);
        if (targetLayer == null)
            return;
        targetLayer.mergeFrom(sourceLayer);
        Main.map.mapView.removeLayer(sourceLayer);
        Main.map.mapView.setActiveLayer(targetLayer);
    }

    public void actionPerformed(ActionEvent e) {
        Layer sourceLayer = Main.main.getEditLayer();
        if (sourceLayer == null)
            return;
        merge(sourceLayer);
    }

    @Override
    protected void updateEnabledState() {
        if (getEditLayer() == null) {
            setEnabled(false);
            return;
        }
        setEnabled(!LayerListDialog.getInstance().getModel().getPossibleMergeTargets(getEditLayer()).isEmpty());
    }
}
