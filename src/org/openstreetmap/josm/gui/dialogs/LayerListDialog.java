package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
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

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DuplicateLayerAction;
import org.openstreetmap.josm.actions.MergeLayerAction;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.io.SaveLayersDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.ImageProvider.OverlayPosition;

/**
 * This is a toggle dialog which displays the list of layers. Actions allow to
 * change the ordering of the layers, to hide/show layers, to activate layers,
 * and to delete layers.
 *
 */
public class LayerListDialog extends ToggleDialog {
    //static private final Logger logger = Logger.getLogger(LayerListDialog.class.getName());

    /** the unique instance of the dialog */
    static private LayerListDialog instance;

    /**
     * Creates the instance of the dialog. It's connected to the map frame <code>mapFrame</code>
     *
     * @param mapFrame the map frame
     */
    static public void createInstance(MapFrame mapFrame) {
        if (instance != null)
            throw new IllegalStateException("Dialog was already created");
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
            throw new IllegalStateException("Dialog not created yet. Invoke createInstance() first");
        return instance;
    }

    /** the model for the layer list */
    private LayerListModel model;

    /** the selection model */
    private DefaultListSelectionModel selectionModel;

    /** the list of layers */
    private LayerList layerList;

    ActivateLayerAction activateLayerAction;

    protected JPanel createButtonPanel() {
        JPanel buttonPanel = getButtonPanel(5);

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
        activateLayerAction = new ActivateLayerAction();
        adaptTo(activateLayerAction, selectionModel);
        buttonPanel.add(new SideButton(activateLayerAction));

        // -- show hide action
        ShowHideLayerAction showHideLayerAction = new ShowHideLayerAction();
        adaptTo(showHideLayerAction, selectionModel);
        buttonPanel.add(new SideButton(showHideLayerAction));

        // -- merge layer action
        MergeAction mergeLayerAction = new MergeAction();
        adaptTo(mergeLayerAction, model);
        adaptTo(mergeLayerAction,selectionModel);
        buttonPanel.add(new SideButton(mergeLayerAction));

        // -- duplicate layer action
        DuplicateAction duplicateLayerAction = new DuplicateAction();
        adaptTo(duplicateLayerAction, model);
        adaptTo(duplicateLayerAction, selectionModel);
        buttonPanel.add(new SideButton(duplicateLayerAction));

        //-- delete layer action
        DeleteLayerAction deleteLayerAction = new DeleteLayerAction();
        layerList.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),"deleteLayer"
        );
        layerList.getActionMap().put("deleteLayer", deleteLayerAction);
        adaptTo(deleteLayerAction, selectionModel);
        buttonPanel.add(new SideButton(deleteLayerAction, false));

        return buttonPanel;
    }

    /**
     * Create an layer list and attach it to the given mapView.
     */
    protected LayerListDialog(MapFrame mapFrame) {
        super(tr("Layers"), "layerlist", tr("Open a list of all loaded layers."),
                Shortcut.registerShortcut("subwindow:layers", tr("Toggle: {0}", tr("Layers")), KeyEvent.VK_L, Shortcut.GROUP_LAYER), 100, true);

        // create the models
        //
        selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        model = new LayerListModel(selectionModel);

        // create the list control
        //
        layerList = new LayerList(model);
        layerList.setSelectionModel(selectionModel);
        layerList.addMouseListener(new DblClickAdapter());
        layerList.addMouseListener(new PopupMenuHandler());
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

    @Override
    public void showNotify() {
        MapView.addLayerChangeListener(activateLayerAction);
        MapView.addLayerChangeListener(model);
        model.populate();
    }

    @Override
    public void hideNotify() {
        MapView.removeLayerChangeListener(model);
        MapView.removeLayerChangeListener(activateLayerAction);
    }

    public LayerListModel getModel() {
        return model;
    }

    private interface IEnabledStateUpdating {
        void updateEnabledState();
    }

    /**
     * Wires <code>listener</code> to <code>listSelectionModel</code> in such a way, that
     * <code>listener</code> receives a {@see IEnabledStateUpdating#updateEnabledState()}
     * on every {@see ListSelectionEvent}.
     *
     * @param listener  the listener
     * @param listSelectionModel  the source emitting {@see ListSelectionEvent}s
     */
    protected void adaptTo(final IEnabledStateUpdating listener, ListSelectionModel listSelectionModel) {
        listSelectionModel.addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        listener.updateEnabledState();
                    }
                }
        );
    }

    /**
     * Wires <code>listener</code> to <code>listModel</code> in such a way, that
     * <code>listener</code> receives a {@see IEnabledStateUpdating#updateEnabledState()}
     * on every {@see ListDataEvent}.
     *
     * @param listener  the listener
     * @param listSelectionModel  the source emitting {@see ListDataEvent}s
     */
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

    @Override
    public void destroy() {
        super.destroy();
        instance = null;
    }

    /**
     * The action to delete the currently selected layer
     */
    public final class DeleteLayerAction extends AbstractAction implements IEnabledStateUpdating, LayerAction {
        /**
         * Creates a {@see DeleteLayerAction} which will delete the currently
         * selected layers in the layer dialog.
         *
         */
        public DeleteLayerAction() {
            putValue(SMALL_ICON,ImageProvider.get("dialogs", "delete"));
            putValue(SHORT_DESCRIPTION, tr("Delete the selected layers."));
            putValue(NAME, tr("Delete"));
            putValue("help", HelpUtil.ht("/Dialog/LayerDialog#DeleteLayer"));
            updateEnabledState();
        }

        protected boolean enforceUploadOrSaveModifiedData(List<Layer> selectedLayers) {
            SaveLayersDialog dialog = new SaveLayersDialog(Main.parent);
            List<OsmDataLayer> layersWithUnmodifiedChanges = new ArrayList<OsmDataLayer>();
            for (Layer l: selectedLayers) {
                if (! (l instanceof OsmDataLayer)) {
                    continue;
                }
                OsmDataLayer odl = (OsmDataLayer)l;
                if ((odl.requiresSaveToFile() || odl.requiresUploadToServer()) && odl.data.isModified()) {
                    layersWithUnmodifiedChanges.add(odl);
                }
            }
            dialog.prepareForSavingAndUpdatingLayersBeforeDelete();
            if (!layersWithUnmodifiedChanges.isEmpty()) {
                dialog.getModel().populate(layersWithUnmodifiedChanges);
                dialog.setVisible(true);
                switch(dialog.getUserAction()) {
                case CANCEL: return false;
                case PROCEED: return true;
                default: return false;
                }
            }
            return true;
        }

        public void actionPerformed(ActionEvent e) {
            List<Layer> selectedLayers = getModel().getSelectedLayers();
            if (selectedLayers.isEmpty())
                return;
            if (! enforceUploadOrSaveModifiedData(selectedLayers))
                return;
            for(Layer l: selectedLayers) {
                Main.main.removeLayer(l);
            }
        }

        public void updateEnabledState() {
            setEnabled(! getModel().getSelectedLayers().isEmpty());
        }

        @Override
        public Component createMenuComponent() {
            return new JMenuItem(this);
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DeleteLayerAction;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    public final class ShowHideLayerAction extends AbstractAction implements IEnabledStateUpdating, LayerAction {
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
            CheckParameterUtil.ensureParameterNotNull(layer, "layer");
            this.layer = layer;
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
            putValue("help", HelpUtil.ht("/Dialog/LayerDialog#ShowHideLayer"));
            putValue(NAME, tr("Show/Hide"));
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

        @Override
        public Component createMenuComponent() {
            return new JMenuItem(this);
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ShowHideLayerAction;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    /**
     * The action to activate the currently selected layer
     */

    public final class ActivateLayerAction extends AbstractAction implements IEnabledStateUpdating, MapView.LayerChangeListener{
        private  Layer layer;

        public ActivateLayerAction(Layer layer) {
            this();
            CheckParameterUtil.ensureParameterNotNull(layer, "layer");
            this.layer = layer;
            putValue(NAME, tr("Activate"));
            updateEnabledState();
        }

        public ActivateLayerAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "activate"));
            putValue(SHORT_DESCRIPTION, tr("Activate the selected layer"));
            putValue("help", HelpUtil.ht("/Dialog/LayerDialog#ActivateLayer"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            Layer toActivate;
            if (layer != null) {
                toActivate = layer;
            } else {
                toActivate = model.getSelectedLayers().get(0);
            }
            // model is  going to be updated via LayerChangeListener
            // and PropertyChangeEvents
            Main.map.mapView.setActiveLayer(toActivate);
            toActivate.setVisible(true);
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

        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            updateEnabledState();
        }
        public void layerAdded(Layer newLayer) {
            updateEnabledState();
        }
        public void layerRemoved(Layer oldLayer) {
            updateEnabledState();
        }
    }

    /**
     * The action to merge the currently selected layer into another layer.
     */
    public final class MergeAction extends AbstractAction implements IEnabledStateUpdating {
        private  Layer layer;

        public MergeAction(Layer layer) throws IllegalArgumentException {
            this();
            CheckParameterUtil.ensureParameterNotNull(layer, "layer");
            this.layer = layer;
            putValue(NAME, tr("Merge"));
            updateEnabledState();
        }

        public MergeAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "mergedown"));
            putValue(SHORT_DESCRIPTION, tr("Merge this layer into another layer"));
            putValue("help", HelpUtil.ht("/Dialog/LayerDialog#MergeLayer"));
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
     * The action to merge the currently selected layer into another layer.
     */
    public final class DuplicateAction extends AbstractAction implements IEnabledStateUpdating {
        private  Layer layer;

        public DuplicateAction(Layer layer) throws IllegalArgumentException {
            this();
            CheckParameterUtil.ensureParameterNotNull(layer, "layer");
            this.layer = layer;
            putValue(NAME, tr("Duplicate"));
            updateEnabledState();
        }

        public DuplicateAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "duplicatelayer"));
            putValue(SHORT_DESCRIPTION, tr("Duplicate this layer"));
            putValue("help", HelpUtil.ht("/Dialog/LayerDialog#DuplicateLayer"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (layer != null) {
                new DuplicateLayerAction().duplicate(layer);
            } else {
                Layer selectedLayer = getModel().getSelectedLayers().get(0);
                new DuplicateLayerAction().duplicate(selectedLayer);
            }
        }

        protected boolean isActiveLayer(Layer layer) {
            if (Main.map == null) return false;
            if (Main.map.mapView == null) return false;
            return Main.map.mapView.getActiveLayer() == layer;
        }

        public void updateEnabledState() {
            if (layer == null) {
                if (getModel().getSelectedLayers().size() == 1) {
                    setEnabled(DuplicateLayerAction.canDuplicate(getModel().getSelectedLayers().get(0)));
                } else {
                    setEnabled(false);
                }
            } else {
                setEnabled(DuplicateLayerAction.canDuplicate(layer));
            }
        }
    }

    /**
     * the list cell renderer used to render layer list entries
     *
     */
    static class LayerListCellRenderer extends DefaultListCellRenderer {

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

    class PopupMenuHandler extends PopupMenuLauncher {
        @Override
        public void launch(MouseEvent evt) {
            Point p = evt.getPoint();
            int index = layerList.locationToIndex(p);
            if (index < 0) return;
            if (!layerList.getCellBounds(index, index).contains(evt.getPoint()))
                return;
            if (!layerList.isSelectedIndex(index)) {
                layerList.setSelectedIndex(index);
            }
            Layer layer = model.getLayer(index);
            LayerListPopup menu = new LayerListPopup(getModel().getSelectedLayers(), layer);
            menu.show(LayerListDialog.this, p.x, p.y-3);
        }
    }

    class DblClickAdapter extends MouseAdapter {
        @Override public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int index = layerList.locationToIndex(e.getPoint());
                if (!layerList.getCellBounds(index, index).contains(e.getPoint()))
                    return;
                Layer layer = model.getLayer(index);
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
     * moving layers up and down, for toggling their visibility, and for activating a layer.
     *
     * The model is a {@see ListModel} and it provides a {@see ListSelectionModel}. It expectes
     * to be configured with a {@see DefaultListSelectionModel}. The selection model is used
     * to update the selection state of views depending on messages sent to the model.
     *
     * The model manages a list of {@see LayerListModelListener} which are mainly notified if
     * the model requires views to make a specific list entry visible.
     *
     * It also listens to {@see PropertyChangeEvent}s of every {@see Layer} it manages, in particular to
     * the properties {@see Layer#VISIBLE_PROP} and {@see Layer#NAME_PROP}.
     */
    public static class LayerListModel extends DefaultListModel implements MapView.LayerChangeListener, PropertyChangeListener{

        /** manages list selection state*/
        private DefaultListSelectionModel selectionModel;
        private CopyOnWriteArrayList<LayerListModelListener> listeners;

        /**
         * constructor
         *
         * @param selectionModel the list selection model
         */
        private LayerListModel(DefaultListSelectionModel selectionModel) {
            this.selectionModel = selectionModel;
            listeners = new CopyOnWriteArrayList<LayerListModelListener>();
        }

        /**
         * Adds a listener to this model
         *
         * @param listener the listener
         */
        public void addLayerListModelListener(LayerListModelListener listener) {
            if (listener != null) {
                listeners.addIfAbsent(listener);
            }
        }

        /**
         * removes a listener from  this model
         * @param listener the listener
         *
         */
        public void removeLayerListModelListener(LayerListModelListener listener) {
            listeners.remove(listener);
        }

        /**
         * Fires a make visible event to listeners
         *
         * @param index the index of the row to make visible
         * @param layer the layer at this index
         * @see LayerListModelListener#makeVisible(int, Layer)
         */
        protected void fireMakeVisible(int index, Layer layer) {
            for (LayerListModelListener listener : listeners) {
                listener.makeVisible(index, layer);
            }
        }

        /**
         * Fires a refresh event to listeners of this model
         *
         * @see LayerListModelListener#refresh()
         */
        protected void fireRefresh() {
            for (LayerListModelListener listener : listeners) {
                listener.refresh();
            }
        }

        /**
         * Populates the model with the current layers managed by
         * {@see MapView}.
         *
         */
        public void populate() {
            for (Layer layer: getLayers()) {
                // make sure the model is registered exactly once
                //
                layer.removePropertyChangeListener(this);
                layer.addPropertyChangeListener(this);
            }
            fireContentsChanged(this, 0, getSize());
        }

        /**
         * Marks <code>layer</code> as selected layer. Ignored, if
         * layer is null.
         *
         * @param layer the layer.
         */
        public void setSelectedLayer(Layer layer) {
            if (layer == null)
                return;
            int idx = getLayers().indexOf(layer);
            if (idx >= 0) {
                selectionModel.setSelectionInterval(idx, idx);
            }
            fireContentsChanged(this, 0, getSize());
            ensureSelectedIsVisible();
        }

        /**
         * Replies the list of currently selected layers. Never null, but may
         * be empty.
         *
         * @return the list of currently selected layers. Never null, but may
         * be empty.
         */
        public List<Layer> getSelectedLayers() {
            ArrayList<Layer> selected = new ArrayList<Layer>();
            for (int i=0; i<getLayers().size(); i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    selected.add(getLayers().get(i));
                }
            }
            return selected;
        }

        /**
         * Replies a the list of indices of the selected rows. Never null,
         * but may be empty.
         *
         * @return  the list of indices of the selected rows. Never null,
         * but may be empty.
         */
        public List<Integer> getSelectedRows() {
            ArrayList<Integer> selected = new ArrayList<Integer>();
            for (int i=0; i<getLayers().size();i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    selected.add(i);
                }
            }
            return selected;
        }

        /**
         * Invoked if a layer managed by {@see MapView} is removed
         *
         * @param layer the layer which is removed
         */
        protected void onRemoveLayer(Layer layer) {
            if (layer == null)
                return;
            layer.removePropertyChangeListener(this);
            int size = getSize();
            List<Integer> rows = getSelectedRows();
            if (rows.isEmpty() && size > 0) {
                selectionModel.setSelectionInterval(size-1, size-1);
            }
            fireRefresh();
            ensureActiveSelected();
        }

        /**
         * Invoked when a layer managed by {@see MapView} is added
         *
         * @param layer the layer
         */
        protected void onAddLayer(Layer layer) {
            if (layer == null) return;
            layer.addPropertyChangeListener(this);
            fireContentsChanged(this, 0, getSize());
            int idx = getLayers().indexOf(layer);
            selectionModel.setSelectionInterval(idx, idx);
            ensureSelectedIsVisible();
        }

        /**
         * Replies the first layer. Null if no layers are present
         *
         * @return the first layer. Null if no layers are present
         */
        public Layer getFirstLayer() {
            if (getSize() == 0) return null;
            return getLayers().get(0);
        }

        /**
         * Replies the layer at position <code>index</code>
         *
         * @param index the index
         * @return the layer at position <code>index</code>. Null,
         * if index is out of range.
         */
        public Layer getLayer(int index) {
            if (index < 0 || index >= getSize())
                return null;
            return getLayers().get(index);
        }

        /**
         * Replies true if the currently selected layers can move up
         * by one position
         *
         * @return true if the currently selected layers can move up
         * by one position
         */
        public boolean canMoveUp() {
            List<Integer> sel = getSelectedRows();
            return !sel.isEmpty() && sel.get(0) > 0;
        }

        /**
         * Move up the currently selected layers by one position
         *
         */
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

        /**
         * Replies true if the currently selected layers can move down
         * by one position
         *
         * @return true if the currently selected layers can move down
         * by one position
         */
        public boolean canMoveDown() {
            List<Integer> sel = getSelectedRows();
            return !sel.isEmpty() && sel.get(sel.size()-1) < getLayers().size()-1;
        }

        /**
         * Move down the currently selected layers by one position
         *
         */
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

        /**
         * Make sure the first of the selected layers is visible in the
         * views of this model.
         *
         */
        protected void ensureSelectedIsVisible() {
            int index = selectionModel.getMinSelectionIndex();
            if (index <0 )return;
            if (index >= getLayers().size()) return;
            Layer layer = getLayers().get(index);
            fireMakeVisible(index, layer);
        }

        /**
         * Replies a list of layers which are possible merge targets
         * for <code>source</code>
         *
         * @param source the source layer
         * @return a list of layers which are possible merge targets
         * for <code>source</code>. Never null, but can be empty.
         */
        public List<Layer> getPossibleMergeTargets(Layer source) {
            ArrayList<Layer> targets = new ArrayList<Layer>();
            if (source == null)
                return targets;
            for(Layer target: getLayers()) {
                if (source == target) {
                    continue;
                }
                if (target.isMergable(source)) {
                    targets.add(target);
                }
            }
            return targets;
        }

        /**
         * Replies the list of layers currently managed by {@see MapView}.
         * Never null, but can be empty.
         *
         * @return the list of layers currently managed by {@see MapView}.
         * Never null, but can be empty.
         */
        protected List<Layer> getLayers() {
            if (Main.map == null || Main.map.mapView == null)
                return Collections.<Layer>emptyList();
            return Main.map.mapView.getAllLayersAsList();
        }

        /**
         * Ensures that at least one layer is selected in the layer dialog
         *
         */
        protected void ensureActiveSelected() {
            if (getLayers().size() == 0) return;
            if (getActiveLayer() != null) {
                // there's an active layer - select it and make it
                // visible
                int idx = getLayers().indexOf(getActiveLayer());
                selectionModel.setSelectionInterval(idx, idx);
                ensureSelectedIsVisible();
            } else {
                // no active layer - select the first one and make
                // it visible
                selectionModel.setSelectionInterval(0, 0);
                ensureSelectedIsVisible();
            }
        }

        /**
         * Replies the active layer. null, if no active layer is available
         *
         * @return the active layer. null, if no active layer is available
         */
        protected Layer getActiveLayer() {
            if (Main.map == null || Main.map.mapView == null) return null;
            return Main.map.mapView.getActiveLayer();
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
            ensureActiveSelected();
        }

        public void layerAdded(Layer newLayer) {
            onAddLayer(newLayer);
        }

        public void layerRemoved(final Layer oldLayer) {
            onRemoveLayer(oldLayer);
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

    static class LayerList extends JList {
        public LayerList(ListModel dataModel) {
            super(dataModel);
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            // if the layer list is embedded in a detached dialog, the last row is
            // selected if a user clicks in the empty space *below* the last row.
            // This mouse event filter prevents this.
            //
            int idx = locationToIndex(e.getPoint());
            // sometimes bounds can be null, see #3539
            Rectangle bounds = getCellBounds(idx,idx);
            if (bounds != null && bounds.contains(e.getPoint())) {
                super.processMouseEvent(e);
            }
        }
    }

    /**
     * Creates a {@see ShowHideLayerAction} for <code>layer</code> in the
     * context of this {@see LayerListDialog}.
     *
     * @param layer the layer
     * @return the action
     */
    public ShowHideLayerAction createShowHideLayerAction() {
        return new ShowHideLayerAction();
    }

    /**
     * Creates a {@see DeleteLayerAction} for <code>layer</code> in the
     * context of this {@see LayerListDialog}.
     *
     * @param layer the layer
     * @return the action
     */
    public DeleteLayerAction createDeleteLayerAction() {
        // the delete layer action doesn't depend on the current layer
        return new DeleteLayerAction();
    }

    /**
     * Creates a {@see ActivateLayerAction} for <code>layer</code> in the
     * context of this {@see LayerListDialog}.
     *
     * @param layer the layer
     * @return the action
     */
    public ActivateLayerAction createActivateLayerAction(Layer layer) {
        return new ActivateLayerAction(layer);
    }

    /**
     * Creates a {@see MergeLayerAction} for <code>layer</code> in the
     * context of this {@see LayerListDialog}.
     *
     * @param layer the layer
     * @return the action
     */
    public MergeAction createMergeLayerAction(Layer layer) {
        return new MergeAction(layer);
    }
}
