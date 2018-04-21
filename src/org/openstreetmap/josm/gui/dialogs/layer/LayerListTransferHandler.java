// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.datatransfer.LayerTransferable;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Logging;

/**
 * This class allows the user to transfer layers using drag+drop.
 * <p>
 * It supports copy (duplication) of layers, simple moves and linking layers to a new layer manager.
 *
 * @author Michael Zangl
 * @since 10605
 */
public class LayerListTransferHandler extends TransferHandler {
    @Override
    public int getSourceActions(JComponent c) {
        if (c instanceof JTable) {
            LayerListModel tableModel = (LayerListModel) ((JTable) c).getModel();
            if (!tableModel.getSelectedLayers().isEmpty()) {
                int actions = MOVE;
                if (onlyDataLayersSelected(tableModel)) {
                    actions |= COPY;
                }
                return actions /* soon: | LINK*/;
            }
        }
        return NONE;
    }

    private static boolean onlyDataLayersSelected(LayerListModel tableModel) {
        for (Layer l : tableModel.getSelectedLayers()) {
            if (!(l instanceof OsmDataLayer)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JTable) {
            LayerListModel tableModel = (LayerListModel) ((JTable) c).getModel();
            return new LayerTransferable(tableModel.getLayerManager(), tableModel.getSelectedLayers());
        }
        return null;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (support.isDrop()) {
            support.setShowDropLocation(true);
        }

        if (!support.isDataFlavorSupported(LayerTransferable.LAYER_DATA)) {
            return false;
        }

        // cannot link yet.
        return support.getDropAction() != LINK;
    }

    @Override
    public boolean importData(TransferSupport support) {
        try {
            LayerListModel tableModel = (LayerListModel) ((JTable) support.getComponent()).getModel();

            LayerTransferable.Data layers = (LayerTransferable.Data) support.getTransferable()
                    .getTransferData(LayerTransferable.LAYER_DATA);

            int dropLocation;
            if (support.isDrop()) {
                DropLocation dl = support.getDropLocation();
                if (dl instanceof JTable.DropLocation) {
                    dropLocation = ((JTable.DropLocation) dl).getRow();
                } else {
                    dropLocation = 0;
                }
            } else {
                dropLocation = layers.getLayers().get(0).getDefaultLayerPosition().getPosition(layers.getManager());
            }

            boolean isSameLayerManager = tableModel.getLayerManager() == layers.getManager();

            if (isSameLayerManager && support.getDropAction() == MOVE) {
                for (Layer layer : layers.getLayers()) {
                    boolean wasBeforeInsert = layers.getManager().getLayers().indexOf(layer) <= dropLocation;
                    if (wasBeforeInsert) {
                        // need to move insertion point one down to preserve order
                        dropLocation--;
                    }
                    layers.getManager().moveLayer(layer, dropLocation);
                    dropLocation++;
                }
            } else {
                List<Layer> layersToUse = layers.getLayers();
                if (support.getDropAction() == COPY) {
                    layersToUse = createCopy(layersToUse, layers.getManager().getLayers());
                }
                for (Layer layer : layersToUse) {
                    layers.getManager().addLayer(layer);
                    layers.getManager().moveLayer(layer, dropLocation);
                    dropLocation++;
                }
            }

            return true;
        } catch (UnsupportedFlavorException e) {
            Logging.warn("Flavor not supported", e);
            return false;
        } catch (IOException e) {
            Logging.warn("Error while pasting layer", e);
            return false;
        }
    }

    private static List<Layer> createCopy(List<Layer> layersToUse, List<Layer> namesToAvoid) {
        Collection<String> layerNames = getNames(namesToAvoid);
        ArrayList<Layer> layers = new ArrayList<>();
        for (Layer layer : layersToUse) {
            if (layer instanceof OsmDataLayer) {
                String newName = suggestNewLayerName(layer.getName(), layerNames);
                OsmDataLayer newLayer = new OsmDataLayer(new DataSet(((OsmDataLayer) layer).getDataSet()), newName, null);
                layers.add(newLayer);
                layerNames.add(newName);
            }
        }
        return layers;
    }

    /**
     * Suggests a new name in the form "copy of name"
     * @param name The base name
     * @param namesToAvoid The list of layers to use to avoid duplicate names.
     * @return The new name
     */
    public static String suggestNewLayerName(String name, List<Layer> namesToAvoid) {
        Collection<String> layerNames = getNames(namesToAvoid);

        return suggestNewLayerName(name, layerNames);
    }

    private static List<String> getNames(List<Layer> namesToAvoid) {
        List<String> layerNames = new ArrayList<>();
        for (Layer l: namesToAvoid) {
            layerNames.add(l.getName());
        }
        return layerNames;
    }

    private static String suggestNewLayerName(String name, Collection<String> layerNames) {
        // Translators: "Copy of {layer name}"
        String newName = tr("Copy of {0}", name);
        int i = 2;
        while (layerNames.contains(newName)) {
            // Translators: "Copy {number} of {layer name}"
            newName = tr("Copy {1} of {0}", name, i);
            i++;
        }
        return newName;
    }
}
