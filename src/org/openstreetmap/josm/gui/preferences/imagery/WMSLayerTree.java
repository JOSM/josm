// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import org.openstreetmap.josm.io.imagery.WMSImagery;

/**
 * The layer tree of a WMS server.
 */
public class WMSLayerTree {
    private final MutableTreeNode treeRootNode = new DefaultMutableTreeNode();
    private final DefaultTreeModel treeData = new DefaultTreeModel(treeRootNode);
    private final JTree layerTree = new JTree(treeData);
    private final List<WMSImagery.LayerDetails> selectedLayers = new LinkedList<WMSImagery.LayerDetails>();
    private boolean previouslyShownUnsupportedCrsError = false;

    /**
     * Returns the root node.
     * @return The root node
     */
    public MutableTreeNode getTreeRootNode() {
        return treeRootNode;
    }

    /**
     * Returns the {@code JTree}.
     * @return The {@code JTree}
     */
    public JTree getLayerTree() {
        return layerTree;
    }

    /**
     * Returns the list of selected layers.
     * @return the list of selected layers
     */
    public List<WMSImagery.LayerDetails> getSelectedLayers() {
        return selectedLayers;
    }

    /**
     * Constructs a new {@code WMSLayerTree}.
     */
    public WMSLayerTree() {
        layerTree.setCellRenderer(new LayerTreeCellRenderer());
        layerTree.addTreeSelectionListener(new WMSTreeSelectionListener());
    }

    void addLayersToTreeData(MutableTreeNode parent, List<WMSImagery.LayerDetails> layers) {
        for (WMSImagery.LayerDetails layerDetails : layers) {
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(layerDetails);
            addLayersToTreeData(treeNode, layerDetails.children);
            treeData.insertNodeInto(treeNode, parent, 0);
        }
    }

    /**
     * Updates the whole tree with the given WMS imagery info.
     * @param wms The imagery info for a given WMS server
     */
    public void updateTree(WMSImagery wms) {
        treeRootNode.setUserObject(wms.getServiceUrl().getHost());
        updateTreeList(wms.getLayers());
    }

    /**
     * Updates the list of WMS layers.
     * @param layers The list of layers to add to the root node
     */
    public void updateTreeList(List<WMSImagery.LayerDetails> layers) {
        addLayersToTreeData(getTreeRootNode(), layers);
        getLayerTree().expandRow(0);
        getLayerTree().expandRow(1);
    }

    private static class LayerTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel, boolean expanded, boolean leaf, int row,
                                                      boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
                    row, hasFocus);
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
            Object userObject = treeNode.getUserObject();
            if (userObject instanceof WMSImagery.LayerDetails) {
                WMSImagery.LayerDetails layer = (WMSImagery.LayerDetails) userObject;
                setEnabled(layer.isSupported());
            }
            return this;
        }
    }

    private class WMSTreeSelectionListener implements TreeSelectionListener {

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath[] selectionRows = layerTree.getSelectionPaths();
            if (selectionRows == null) {
                return;
            }

            selectedLayers.clear();
            for (TreePath i : selectionRows) {
                Object userObject = ((DefaultMutableTreeNode) i.getLastPathComponent()).getUserObject();
                if (userObject instanceof WMSImagery.LayerDetails) {
                    WMSImagery.LayerDetails detail = (WMSImagery.LayerDetails) userObject;
                    if (!detail.isSupported()) {
                        layerTree.removeSelectionPath(i);
                        if (!previouslyShownUnsupportedCrsError) {
                            JOptionPane.showMessageDialog(null, tr("That layer does not support any of JOSM''s projections,\n" +
                                    "so you can not use it. This message will not show again."),
                                    tr("WMS Error"), JOptionPane.ERROR_MESSAGE);
                            previouslyShownUnsupportedCrsError = true;
                        }
                    } else if (detail.ident != null) {
                        selectedLayers.add(detail);
                    }
                }
            }
            layerTree.firePropertyChange("selectedLayers", /*dummy values*/ false , true);
        }
    }
}