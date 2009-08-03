// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.MergeLayerAction;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.ImageProvider.OverlayPosition;

/**
 * This is a toggle dialog which displays the list of layers. Actions allow to
 * change the ordering the layer, to hide/show layers, to activate layers
 * and to delete layers.
 * 
 */
public class LayerListDialog extends ToggleDialog {
    static private final Logger logger = Logger.getLogger(LayerListDialog.class.getName());

    /** the unique instance of the dialog */
    static private LayerListDialog instance;

    /**
     * Creates the instance of the dialog. It's connected to the map frame <code>mapFrame</code>
     * 
     * @param mapFrame the map frame
     */
    static public void createInstance(MapFrame mapFrame) {
        instance = new LayerListDialog(mapFrame);
    }

    /**
     * Replies the instance of the dialog
     * 
     * @return the instance of the dialog
     * @throws IllegalStateException thrown, if the dialog is not created yet
     * @see #createInstance(MapFrame)
     */
    static public LayerListDialog getInstance() throws IllegalStateException {
        if (instance == null)
            throw new IllegalStateException(tr("Dialog not created yet. Invoke createInstance() first"));
        return instance;
    }

    /** the model for the layer list */
    private LayerListModel model;

    /** the selection model */
    private DefaultListSelectionModel selectionModel;

    /** the list of layers */
    private LayerList layerList;

    protected JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(1, 5));

        // -- move up action
        MoveUpAction moveUpAction = new MoveUpAction();
        adaptTo(moveUpAction, model);
        adaptTo(moveUpAction,selectionModel);
        buttonPanel.add(new SideButton(moveUpAction));

        // -- move down action
        MoveDownAction moveDownAction = new MoveDownAction();
        adaptTo(moveDownAction, model);
        adaptTo(moveDownAction,selectionModel);
        buttonPanel.add(new SideButton(moveDownAction));

        // -- activate action
        ActivateLayerAction activateLayerAction = new ActivateLayerAction();
        adaptTo(activateLayerAction, selectionModel);
        adaptToLayerChanges(activateLayerAction);
        buttonPanel.add(new SideButton(activateLayerAction, "activate"));

        // -- show hide action
        ShowHideLayerAction showHideLayerAction = new ShowHideLayerAction();
        adaptTo(showHideLayerAction, selectionModel);
        buttonPanel.add(new SideButton(showHideLayerAction, "showhide"));

        // -- merge layer action
        MergeAction mergeLayerAction = new MergeAction();
        adaptTo(mergeLayerAction, model);
        adaptTo(mergeLayerAction,selectionModel);
        buttonPanel.add(new SideButton(mergeLayerAction));

        //-- delete layer action
        DeleteLayerAction deleteLayerAction = new DeleteLayerAction();
        adaptTo(deleteLayerAction, selectionModel);
        buttonPanel.add(new SideButton(deleteLayerAction, "delete"));

        return buttonPanel;
    }

    /**
     * Create an layerlist and attach it to the given mapView.
     */
    protected LayerListDialog(MapFrame mapFrame) {
        super(tr("Layers"), "layerlist", tr("Open a list of all loaded layers."),
                Shortcut.registerShortcut("subwindow:layers", tr("Toggle: {0}", tr("Layers")), KeyEvent.VK_L, Shortcut.GROUP_LAYER), 100);

        // create the models
        //
        selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        model = new LayerListModel(selectionModel);
        Layer.listeners.add(model);

        // create the list control
        //
        layerList = new LayerList(model);
        layerList.setSelectionModel(selectionModel);
        layerList.addMouseListener(new LayerListMouseAdapter());
        layerList.setBackground(UIManager.getColor("Button.background"));
        layerList.setCellRenderer(new LayerListCellRenderer());
        add(new JScrollPane(layerList), BorderLayout.CENTER);

        // init the model
        //
        final MapView mapView = mapFrame.mapView;
        model.populate();
        model.setSelectedLayer(mapView.getActiveLayer());
        model.addLayerListModelListener(
                new LayerListModelListener() {
                    public void makeVisible(int index, Layer layer) {
                        layerList.ensureIndexIsVisible(index);
                    }

                    public void refresh() {
                        layerList.repaint();
                    }
                }
        );

        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    public LayerListModel getModel() {
        return model;
    }

    private interface IEnabledStateUpdating {
        void updateEnabledState();
    }

    protected void adaptTo(final IEnabledStateUpdating listener, ListSelectionModel listSelectionModel) {
        listSelectionModel.addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        listener.updateEnabledState();
                    }
                }
        );
    }

    protected void adaptTo(final IEnabledStateUpdating listener, ListModel listModel) {
        listModel.addListDataListener(
                new ListDataListener() {
                    public void contentsChanged(ListDataEvent e) {
                        listener.updateEnabledState();
                    }

                    public void intervalAdded(ListDataEvent e) {
                        listener.updateEnabledState();
                    }

                    public void intervalRemoved(ListDataEvent e) {
                        listener.updateEnabledState();
                    }
                }
        );
    }

    protected void adaptToLayerChanges(final IEnabledStateUpdating listener) {
        Layer.listeners.add(
                new LayerChangeListener() {
                    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
                        listener.updateEnabledState();
                    }
                    public void layerAdded(Layer newLayer) {
                        listener.updateEnabledState();
                    }
                    public void layerRemoved(Layer oldLayer) {
                        listener.updateEnabledState();
                    }
                }
        );
    }


    private enum DeleteDecision {
        deleteCurrent,
        dontDeleteCurrent,
        deleteAll,
        cancel
    }

    /**
     * The action to delete the currently selected layer
     */
    public final  class DeleteLayerAction extends AbstractAction implements IEnabledStateUpdating {



        private  Layer layer;

        /**
         * Creates a {@see DeleteLayerAction} for a specific layer.
         * 
         * @param layer the layer. Must not be null.
         * @exception IllegalArgumentException thrown, if layer is null
         */
        public DeleteLayerAction(Layer layer) {
            this();
            if (layer == null)
                throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "layer"));
            this.layer = layer;
            putValue(NAME, tr("Delete"));
            updateEnabledState();
        }

        /**
         * Creates a {@see DeleteLayerAction} which will delete the currently
         * selected layers in the layer dialog.
         * 
         */
        public DeleteLayerAction() {
            putValue(SMALL_ICON,ImageProvider.get("dialogs", "delete"));
            putValue(SHORT_DESCRIPTION, tr("Delete the selected layer."));
            putValue("help", "Action/LayerDelete");
            updateEnabledState();
        }

        protected boolean confirmSkipSaving(OsmDataLayer layer) {
            int result = new ExtendedDialog(Main.parent,
                    tr("Unsaved Changes"),
                    tr("There are unsaved changes in the layer''{0}''. Delete the layer anwyay?",layer.getName()),
                    new String[] {tr("Delete Layer"), tr("Cancel")},
                    new String[] {"dialogs/delete.png", "cancel.png"}).getValue();

            return result != 1;
        }

        protected boolean confirmDeleteLayer(Layer layer) {
            return ConditionalOptionPaneUtil.showConfirmationDialog(
                    "delete_layer",
                    Main.parent,
                    tr("Do you really want to delete layer ''{0}''?", layer.getName()),
                    tr("Confirmation"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.YES_OPTION);
        }

        protected DeleteDecision confirmDeleteMultipleLayer(Layer layer, int idx, int numLayers) {
            String options[] = new String[] {
                    tr("Yes"),
                    tr("No"),
                    tr("Delete all"),
                    tr("Cancel")
            };
            int ret = ConditionalOptionPaneUtil.showOptionDialog(
                    "delete_layer",
                    Main.parent,
                    tr("Do you really want to delete layer ''{0}''?", layer.getName()),
                    tr("Deleting layer {0} of {1}", idx+1, numLayers),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    options,
                    options[0]
            );
            switch(ret) {
            case ConditionalOptionPaneUtil.DIALOG_DISABLED_OPTION: return DeleteDecision.deleteAll;
            case JOptionPane.CLOSED_OPTION: return DeleteDecision.cancel;
            case 0: return DeleteDecision.deleteCurrent;
            case 1: return DeleteDecision.dontDeleteCurrent;
            case 2: return DeleteDecision.deleteAll;
            case 3: return DeleteDecision.cancel;
            default:
                // shouldn't happen. This is the safest option.
                return DeleteDecision.cancel;
            }
        }


        public void deleteSingleLayer(Layer layer) {
            if (layer == null)
                return;
            if (layer instanceof OsmDataLayer) {
                OsmDataLayer dataLayer = (OsmDataLayer)layer;
                if (dataLayer.isModified() && ! confirmSkipSaving(dataLayer))
                    return;
                else if (!confirmDeleteLayer(dataLayer))
                    return;

            } else {
                if (!confirmDeleteLayer(layer))
                    return;
            }
            // model and view are going to be updated via LayerChangeListener
            //
            Main.main.removeLayer(layer);
        }

        public void deleteMultipleLayers(List<Layer> layers) {
            boolean doAskConfirmation = true;
            for (int i=0; i < layers.size(); i++) {
                Layer layer = layers.get(i);
                if (layer instanceof OsmDataLayer) {
                    OsmDataLayer dataLayer = (OsmDataLayer)layer;
                    if (dataLayer.isModified() && ! confirmSkipSaving(dataLayer)) {
                        continue;
                    }
                }
                if (doAskConfirmation) {
                    DeleteDecision decision = confirmDeleteMultipleLayer(layer, i, layers.size());
                    switch(decision) {
                    case deleteCurrent: /* do nothing */ break;
                    case deleteAll: doAskConfirmation = false; break;
                    case dontDeleteCurrent: continue;
                    case cancel: return;
                    }
                }
                // model and view are going to be updated via LayerChangeListener
                //
                Main.main.removeLayer(layer);
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (this.layer == null) {
                List<Layer> selectedLayers = getModel().getSelectedLayers();
                if (selectedLayers.size() == 1) {
                    deleteSingleLayer(selectedLayers.get(0));
                } else {
                    deleteMultipleLayers(selectedLayers);
                }
            } else {
                deleteSingleLayer(this.layer);
            }
        }

        public void updateEnabledState() {
            if (layer == null) {
                setEnabled(! getModel().getSelectedLayers().isEmpty());
            } else {
                setEnabled(true);
            }
        }
    }

    public final class ShowHideLayerAction extends AbstractAction implements IEnabledStateUpdating {
        private  Layer layer;

        /**
         * Creates a {@see ShowHideLayerAction} which toggle the visibility of
         * a specific layer.
         * 
         * @param layer  the layer. Must not be null.
         * @exception IllegalArgumentException thrown, if layer is null
         */
        public ShowHideLayerAction(Layer layer) throws IllegalArgumentException {
            this();
            if (layer == null)
                throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "layer"));
            this.layer = layer;
            putValue(NAME, tr("Show/Hide"));
            updateEnabledState();
        }

        /**
         * Creates a {@see ShowHideLayerAction} which will toggle the visibility of
         * the currently selected layers
         * 
         */
        public ShowHideLayerAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "showhide"));
            putValue(SHORT_DESCRIPTION, tr("Toggle visible state of the selected layer."));
            putValue("help", "Action/LayerShowHide");
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (layer != null) {
                layer.toggleVisible();
            } else {
                for(Layer layer: model.getSelectedLayers()) {
                    layer.toggleVisible();
                }
            }
        }

        public void updateEnabledState() {
            if (layer == null) {
                setEnabled(! getModel().getSelectedLayers().isEmpty());
            } else {
                setEnabled(true);
            }
        }
    }

    /**
     * The action to activate the currently selected layer
     */

    public final class ActivateLayerAction extends AbstractAction implements IEnabledStateUpdating{
        private  Layer layer;

        public ActivateLayerAction(Layer layer) throws IllegalArgumentException {
            this();
            if (layer == null)
                throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "layer"));
            this.layer = layer;
            putValue(NAME, tr("Activate"));
            updateEnabledState();
        }

        public ActivateLayerAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "activate"));
            putValue(SHORT_DESCRIPTION, tr("Activate the selected layer"));
            putValue("help", "Action/ActivateLayer");
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            Layer toActivate;
            if (layer != null) {
                toActivate = layer;
            } else {
                toActivate = model.getSelectedLayers().get(0);
            }
            getModel().activateLayer(toActivate);
        }

        protected boolean isActiveLayer(Layer layer) {
            if (Main.map == null) return false;
            if (Main.map.mapView == null) return false;
            return Main.map.mapView.getActiveLayer() == layer;
        }

        public void updateEnabledState() {
            if (layer == null) {
                if (getModel().getSelectedLayers().size() != 1) {
                    setEnabled(false);
                    return;
                }
                Layer selectedLayer = getModel().getSelectedLayers().get(0);
                setEnabled(!isActiveLayer(selectedLayer));
            } else {
                setEnabled(!isActiveLayer(layer));
            }
        }
    }

    /**
     * The action to merge the currently selected layer into another layer.
     */
    public final class MergeAction extends AbstractAction implements IEnabledStateUpdating {
        private  Layer layer;

        public MergeAction(Layer layer) throws IllegalArgumentException {
            this();
            if (layer == null)
                throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "layer"));
            this.layer = layer;
            putValue(NAME, tr("Merge"));
            updateEnabledState();
        }

        public MergeAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "mergedown"));
            putValue(SHORT_DESCRIPTION, tr("Merge this layer into another layer"));
            putValue("help", "Action/MergeLayer");
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (layer != null) {
                new MergeLayerAction().merge(layer);
            } else {
                Layer selectedLayer = getModel().getSelectedLayers().get(0);
                new MergeLayerAction().merge(selectedLayer);
            }
        }

        protected boolean isActiveLayer(Layer layer) {
            if (Main.map == null) return false;
            if (Main.map.mapView == null) return false;
            return Main.map.mapView.getActiveLayer() == layer;
        }

        public void updateEnabledState() {
            if (layer == null) {
                if (getModel().getSelectedLayers().size() != 1) {
                    setEnabled(false);
                    return;
                }
                Layer selectedLayer = getModel().getSelectedLayers().get(0);
                List<Layer> targets = getModel().getPossibleMergeTargets(selectedLayer);
                setEnabled(!targets.isEmpty());
            } else {
                List<Layer> targets = getModel().getPossibleMergeTargets(layer);
                setEnabled(!targets.isEmpty());
            }
        }
    }

    /**
     * the list cell renderer used to render layer list entries
     *
     */
    class LayerListCellRenderer extends DefaultListCellRenderer {

        protected boolean isActiveLayer(Layer layer) {
            if (Main.map == null) return false;
            if (Main.map.mapView == null) return false;
            return Main.map.mapView.getActiveLayer() == layer;
        }

        @Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Layer layer = (Layer)value;
            JLabel label = (JLabel)super.getListCellRendererComponent(list,
                    layer.getName(), index, isSelected, cellHasFocus);
            Icon icon = layer.getIcon();
            if (isActiveLayer(layer)) {
                icon = ImageProvider.overlay(icon, "overlay/active", OverlayPosition.SOUTHWEST);
            }
            if (!layer.isVisible()) {
                icon = ImageProvider.overlay(icon, "overlay/invisiblenew", OverlayPosition.SOUTHEAST);
            }
            label.setIcon(icon);
            label.setToolTipText(layer.getToolTipText());
            return label;
        }
    }

    class LayerListMouseAdapter extends MouseAdapter {

        private void openPopup(MouseEvent e) {
            Point p = e.getPoint();
            int index = layerList.locationToIndex(p);
            if (index < 0) return;
            if (!layerList.getCellBounds(index, index).contains(e.getPoint()))
                return;
            Layer layer = model.getLayer(index);
            LayerListPopup menu = new LayerListPopup(layerList, layer);
            menu.show(LayerListDialog.this, p.x, p.y-3);
        }
        @Override public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                openPopup(e);
            }
        }
        @Override public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                openPopup(e);
            }
        }
        @Override public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int index = layerList.locationToIndex(e.getPoint());
                if (!layerList.getCellBounds(index, index).contains(e.getPoint()))
                    return;
                Layer layer = model.getLayer(index);
                String current = Main.pref.get("marker.show "+layer.getName(),"show");
                Main.pref.put("marker.show "+layer.getName(), current.equalsIgnoreCase("show") ? "hide" : "show");
                layer.toggleVisible();
            }
        }
    }

    /**
     * The action to move up the currently selected entries in the list.
     */
    class MoveUpAction extends AbstractAction implements  IEnabledStateUpdating{
        public MoveUpAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "up"));
            putValue(SHORT_DESCRIPTION, tr("Move the selected layer one row up."));
            updateEnabledState();
        }

        public void updateEnabledState() {
            setEnabled(model.canMoveUp());
        }

        public void actionPerformed(ActionEvent e) {
            model.moveUp();
        }
    }

    /**
     * The action to move down the currently selected entries in the list.
     */
    class MoveDownAction extends AbstractAction implements IEnabledStateUpdating {
        public MoveDownAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "down"));
            putValue(SHORT_DESCRIPTION, tr("Move the selected layer one row down."));
            updateEnabledState();
        }

        public void updateEnabledState() {
            setEnabled(model.canMoveDown());
        }

        public void actionPerformed(ActionEvent e) {
            model.moveDown();
        }
    }

    /**
     * Observer interface to be implemented by views using {@see LayerListModel}
     *
     */
    public interface LayerListModelListener {
        public void makeVisible(int index, Layer layer);
        public void refresh();
    }

    /**
     * The layer list model. The model manages a list of layers and provides methods for
     * moving layers up and down, for toggling their visibiliy, for activating a layer.
     * 
     * The model is a {@see ListModel} and it provides a {@see ListSelectionModel}. It expectes
     * to be configured with a {@see DefaultListSelectionModel}. The selection model is used
     * to update the selection state of views depending on messages sent to the model.
     * 
     * The model manages a list of {@see LayerListModelListener} which are mainly notified if
     * the model requires views to make a specific list entry visible.
     */
    public class LayerListModel extends DefaultListModel implements LayerChangeListener, PropertyChangeListener{

        //private ArrayList<Layer> layers;
        private DefaultListSelectionModel selectionModel;
        private CopyOnWriteArrayList<LayerListModelListener> listeners;

        private LayerListModel(DefaultListSelectionModel selectionModel) {
            this.selectionModel = selectionModel;
            listeners = new CopyOnWriteArrayList<LayerListModelListener>();
        }

        public void addLayerListModelListener(LayerListModelListener listener) {
            synchronized(listeners) {
                if (listener != null && !listeners.contains(listener)) {
                    listeners.add(listener);
                }
            }
        }

        public void removeLayerListModelListener(LayerListModelListener listener) {
            synchronized(listeners) {
                if (listener != null && listeners.contains(listener)) {
                    listeners.remove(listener);
                }
            }
        }

        protected void fireMakeVisible(int index, Layer layer) {
            for (LayerListModelListener listener : listeners) {
                listener.makeVisible(index, layer);
            }
        }

        protected void fireRefresh() {
            for (LayerListModelListener listener : listeners) {
                listener.refresh();
            }
        }

        public void populate() {
            if (getLayers() != null) {
                for (Layer layer: getLayers()) {
                    // make sure the model is registered exactly once
                    //
                    layer.removePropertyChangeListener(this);
                    layer.addPropertyChangeListener(this);
                }
            }
            fireContentsChanged(this, 0, getSize());
        }

        public void setSelectedLayer(Layer layer) {
            if (layer == null || getLayers() == null)
                return;
            int idx = getLayers().indexOf(layer);
            if (idx >= 0) {
                selectionModel.setSelectionInterval(idx, idx);
            }
            fireContentsChanged(this, 0, getSize());
            ensureSelectedIsVisible();
        }

        public List<Layer> getSelectedLayers() {
            ArrayList<Layer> selected = new ArrayList<Layer>();
            if (getLayers() == null) return selected;
            for (int i=0; i<getLayers().size(); i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    selected.add(getLayers().get(i));
                }
            }
            return selected;
        }

        public List<Integer> getSelectedRows() {
            ArrayList<Integer> selected = new ArrayList<Integer>();
            if (getLayers() == null) return selected;
            for (int i=0; i<getLayers().size();i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    selected.add(i);
                }
            }
            return selected;
        }

        protected void removeLayer(Layer layer) {
            if (layer == null)
                return;
            fireRefresh();
            ensureSelectedIsVisible();
        }

        protected void addLayer(Layer layer) {
            if (layer == null) return;
            layer.addPropertyChangeListener(this);
            fireContentsChanged(this, 0, getSize());
        }

        public Layer getFirstLayer() {
            if (getSize() == 0) return null;
            return getLayers().get(0);
        }

        public Layer getLayer(int index) {
            if (index < 0 || index >= getSize())
                return null;
            return getLayers().get(index);
        }

        public boolean canMoveUp() {
            List<Integer> sel = getSelectedRows();
            return !sel.isEmpty() && sel.get(0) > 0;
        }

        public void moveUp() {
            if (!canMoveUp()) return;
            List<Integer> sel = getSelectedRows();
            for (int row: sel) {
                Layer l1 = getLayers().get(row);
                Layer l2 = getLayers().get(row-1);
                Main.map.mapView.moveLayer(l2,row);
                Main.map.mapView.moveLayer(l1, row-1);
            }
            fireContentsChanged(this, 0, getSize());
            selectionModel.clearSelection();
            for(int row: sel) {
                selectionModel.addSelectionInterval(row-1, row-1);
            }
            ensureSelectedIsVisible();
        }

        public boolean canMoveDown() {
            List<Integer> sel = getSelectedRows();
            return !sel.isEmpty() && sel.get(sel.size()-1) < getLayers().size()-1;
        }

        public void moveDown() {
            if (!canMoveDown()) return;
            List<Integer> sel = getSelectedRows();
            Collections.reverse(sel);
            for (int row: sel) {
                Layer l1 = getLayers().get(row);
                Layer l2 = getLayers().get(row+1);
                Main.map.mapView.moveLayer(l1, row+1);
                Main.map.mapView.moveLayer(l2, row);
            }
            fireContentsChanged(this, 0, getSize());
            selectionModel.clearSelection();
            for(int row: sel) {
                selectionModel.addSelectionInterval(row+1, row+1);
            }
            ensureSelectedIsVisible();
        }

        protected void ensureSelectedIsVisible() {
            int index = selectionModel.getMinSelectionIndex();
            if (index <0 )return;
            if (getLayers() == null) return;
            if (index >= getLayers().size()) return;
            Layer layer = getLayers().get(index);
            fireMakeVisible(index, layer);
        }

        public List<Layer> getPossibleMergeTargets(Layer layer) {
            ArrayList<Layer> targets = new ArrayList<Layer>();
            if (layer == null)
                return targets;
            for(Layer target: getLayers()) {
                if (layer == target) {
                    continue;
                }
                if (target.isMergable(layer)) {
                    targets.add(target);
                }
            }
            return targets;
        }

        public void activateLayer(Layer layer) {
            Main.map.mapView.moveLayer(layer,0);
            Main.map.mapView.setActiveLayer(layer);
            layer.setVisible(true);
            selectionModel.setSelectionInterval(0,0);
            ensureSelectedIsVisible();
        }

        protected List<Layer> getLayers() {
            if (Main.map == null) return null;
            if (Main.map.mapView == null) return null;
            return Main.map.mapView.getAllLayersAsList();
        }

        /* ------------------------------------------------------------------------------ */
        /* Interface ListModel                                                            */
        /* ------------------------------------------------------------------------------ */
        @Override
        public Object getElementAt(int index) {
            return getLayers().get(index);
        }

        @Override
        public int getSize() {
            List<Layer> layers = getLayers();
            if (layers == null) return 0;
            return layers.size();
        }

        /* ------------------------------------------------------------------------------ */
        /* Interface LayerChangeListener                                                  */
        /* ------------------------------------------------------------------------------ */
        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            if (oldLayer != null) {
                int idx = getLayers().indexOf(oldLayer);
                if (idx >= 0) {
                    fireContentsChanged(this, idx,idx);
                }
            }

            if (newLayer != null) {
                int idx = getLayers().indexOf(newLayer);
                if (idx >= 0) {
                    fireContentsChanged(this, idx,idx);
                }
            }
        }

        public void layerAdded(Layer newLayer) {
            addLayer(newLayer);
        }

        public void layerRemoved(final Layer oldLayer) {
            SwingUtilities.invokeLater(
                    new Runnable() {
                        public void run() {
                            removeLayer(oldLayer);
                        }
                    }
            );
        }


        /* ------------------------------------------------------------------------------ */
        /* Interface PropertyChangeListener                                               */
        /* ------------------------------------------------------------------------------ */
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() instanceof Layer) {
                Layer layer = (Layer)evt.getSource();
                final int idx = getLayers().indexOf(layer);
                if (idx < 0) return;
                fireRefresh();
            }
        }
    }

    class LayerList extends JList {
        public LayerList(ListModel dataModel) {
            super(dataModel);
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            // if the layer list is embedded in a detached dialog, the last row is
            // is selected if a user clicks in the empty space *below* the last row.
            // This mouse event filter prevents this.
            //
            int idx = locationToIndex(e.getPoint());
            if (getCellBounds(idx, idx).contains(e.getPoint())) {
                super.processMouseEvent(e);
            }
        }
    }

    public ShowHideLayerAction createShowHideLayerAction(Layer layer) {
        return new ShowHideLayerAction(layer);
    }

    public DeleteLayerAction createDeleteLayerAction(Layer layer) {
        return new DeleteLayerAction(layer);
    }

    public ActivateLayerAction createActivateLayerAction(Layer layer) {
        return new ActivateLayerAction(layer);
    }

    public MergeAction createMergeLayerAction(Layer layer) {
        return new MergeAction(layer);
    }
}
