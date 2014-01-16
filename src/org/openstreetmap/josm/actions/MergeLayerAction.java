// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action that merges two or more OSM data layers. 
 * @since 1890
 */
public class MergeLayerAction extends AbstractMergeAction {

    /**
     * Constructs a new {@code MergeLayerAction}.
     */
    public MergeLayerAction() {
        super(tr("Merge layer"), "dialogs/mergedown",
            tr("Merge the current layer into another layer"),
            Shortcut.registerShortcut("system:merge", tr("Edit: {0}",
            tr("Merge")), KeyEvent.VK_M, Shortcut.CTRL),
            true, "action/mergelayer", true);
        putValue("help", ht("/Action/MergeLayer"));
    }

    protected void doMerge(List<Layer> targetLayers, final Collection<Layer> sourceLayers) {
        final Layer targetLayer = askTargetLayer(targetLayers);
        if (targetLayer == null)
            return;
        Main.worker.submit(new Runnable() {
            @Override
            public void run() {
                boolean layerMerged = false;
                for (Layer sourceLayer: sourceLayers) {
                    if (sourceLayer != null && sourceLayer != targetLayer) {
                        if (sourceLayer instanceof OsmDataLayer && targetLayer instanceof OsmDataLayer
                                && ((OsmDataLayer)sourceLayer).isUploadDiscouraged() != ((OsmDataLayer)targetLayer).isUploadDiscouraged()) {
                            if (warnMergingUploadDiscouragedLayers(sourceLayer, targetLayer)) {
                                break;
                            }
                        }
                        targetLayer.mergeFrom(sourceLayer);
                        Main.main.removeLayer(sourceLayer);
                        layerMerged = true;
                    }
                }
                if (layerMerged) {
                    Main.map.mapView.setActiveLayer(targetLayer);
                }
            }
        });
    }

    /**
     * Merges a list of layers together.
     * @param sourceLayers The layers to merge
     */
    public void merge(List<Layer> sourceLayers) {
        doMerge(sourceLayers, sourceLayers);
    }

    /**
     * Merges the given source layer with another one, determined at runtime.
     * @param sourceLayer The source layer to merge
     */
    public void merge(Layer sourceLayer) {
        if (sourceLayer == null)
            return;
        List<Layer> targetLayers = LayerListDialog.getInstance().getModel().getPossibleMergeTargets(sourceLayer);
        if (targetLayers.isEmpty()) {
            warnNoTargetLayersForSourceLayer(sourceLayer);
            return;
        }
        doMerge(targetLayers, Collections.singleton(sourceLayer));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Layer sourceLayer = Main.main.getEditLayer();
        if (sourceLayer == null)
            return;
        merge(sourceLayer);
    }

    @Override
    protected void updateEnabledState() {
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                if (getEditLayer() == null) {
                    setEnabled(false);
                    return;
                }
                setEnabled(!LayerListDialog.getInstance().getModel().getPossibleMergeTargets(getEditLayer()).isEmpty());
            }
        });
    }

    /**
     * Warns about a discouraged merge operation, ask for confirmation.
     * @param sourceLayer The source layer
     * @param targetLayer The target layer
     * @return {@code true} if the user wants to cancel, {@code false} if they want to continue
     */
    public static final boolean warnMergingUploadDiscouragedLayers(Layer sourceLayer, Layer targetLayer) {
        return GuiHelper.warnUser(tr("Merging layers with different upload policies"),
                "<html>" +
                tr("You are about to merge data between layers ''{0}'' and ''{1}''.<br /><br />"+
                        "These layers have different upload policies and should not been merged as it.<br />"+
                        "Merging them will result to enforce the stricter policy (upload discouraged) to ''{1}''.<br /><br />"+
                        "<b>This is not the recommended way of merging such data</b>.<br />"+
                        "You should instead check and merge each object, one by one, by using ''<i>Merge selection</i>''.<br /><br />"+
                        "Are you sure you want to continue?", sourceLayer.getName(), targetLayer.getName(), targetLayer.getName())+
                "</html>",
                ImageProvider.get("dialogs", "mergedown"), tr("Ignore this hint and merge anyway"));
    }
}
