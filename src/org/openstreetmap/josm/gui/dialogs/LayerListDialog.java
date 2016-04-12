// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.MergeLayerAction;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.layer.NativeScaleLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.DisableShortcutsOnFocusGainedTextField;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.MultikeyActionsHandler;
import org.openstreetmap.josm.tools.MultikeyShortcutAction;
import org.openstreetmap.josm.tools.MultikeyShortcutAction.MultikeyInfo;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is a toggle dialog which displays the list of layers. Actions allow to
 * change the ordering of the layers, to hide/show layers, to activate layers,
 * and to delete layers.
 * @since 17
 */
public class LayerListDialog extends ToggleDialog {
    /** the unique instance of the dialog */
    private static volatile LayerListDialog instance;

    /**
     * Creates the instance of the dialog. It's connected to the map frame <code>mapFrame</code>
     *
     * @param mapFrame the map frame
     */
    public static void createInstance(MapFrame mapFrame) {
        if (instance != null)
            throw new IllegalStateException("Dialog was already created");
        instance = new LayerListDialog(mapFrame);
    }

    /**
     * Replies the instance of the dialog
     *
     * @return the instance of the dialog
     * @throws IllegalStateException if the dialog is not created yet
     * @see #createInstance(MapFrame)
     */
    public static LayerListDialog getInstance() {
        if (instance == null)
            throw new IllegalStateException("Dialog not created yet. Invoke createInstance() first");
        return instance;
    }

    /** the model for the layer list */
    private final LayerListModel model;

    /** the list of layers (technically its a JTable, but appears like a list) */
    private final LayerList layerList;

    private final ActivateLayerAction activateLayerAction;
    private final ShowHideLayerAction showHideLayerAction;

    //TODO This duplicates ShowHide actions functionality
    /** stores which layer index to toggle and executes the ShowHide action if the layer is present */
    private final class ToggleLayerIndexVisibility extends AbstractAction {
        private final int layerIndex;

        ToggleLayerIndexVisibility(int layerIndex) {
            this.layerIndex = layerIndex;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Layer l = model.getLayer(model.getRowCount() - layerIndex - 1);
            if (l != null) {
                l.toggleVisible();
            }
        }
    }

    private final transient Shortcut[] visibilityToggleShortcuts = new Shortcut[10];
    private final ToggleLayerIndexVisibility[] visibilityToggleActions = new ToggleLayerIndexVisibility[10];

    /**
     * registers (shortcut to toggle right hand side toggle dialogs)+(number keys) shortcuts
     * to toggle the visibility of the first ten layers.
     */
    private void createVisibilityToggleShortcuts() {
        for (int i = 0; i < 10; i++) {
            final int i1 = i + 1;
            /* POSSIBLE SHORTCUTS: 1,2,3,4,5,6,7,8,9,0=10 */
            visibilityToggleShortcuts[i] = Shortcut.registerShortcut("subwindow:layers:toggleLayer" + i1,
                    tr("Toggle visibility of layer: {0}", i1), KeyEvent.VK_0 + (i1 % 10), Shortcut.ALT);
            visibilityToggleActions[i] = new ToggleLayerIndexVisibility(i);
            Main.registerActionShortcut(visibilityToggleActions[i], visibilityToggleShortcuts[i]);
        }
    }

    /**
     * Creates a layer list and attach it to the given mapView.
     * @param mapFrame map frame
     */
    protected LayerListDialog(MapFrame mapFrame) {
        super(tr("Layers"), "layerlist", tr("Open a list of all loaded layers."),
                Shortcut.registerShortcut("subwindow:layers", tr("Toggle: {0}", tr("Layers")), KeyEvent.VK_L,
                        Shortcut.ALT_SHIFT), 100, true);

        // create the models
        //
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        model = new LayerListModel(selectionModel);

        // create the list control
        //
        layerList = new LayerList(model);
        layerList.setSelectionModel(selectionModel);
        layerList.addMouseListener(new PopupMenuHandler());
        layerList.setBackground(UIManager.getColor("Button.background"));
        layerList.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        layerList.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);
        layerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        layerList.setTableHeader(null);
        layerList.setShowGrid(false);
        layerList.setIntercellSpacing(new Dimension(0, 0));
        layerList.getColumnModel().getColumn(0).setCellRenderer(new ActiveLayerCellRenderer());
        layerList.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new ActiveLayerCheckBox()));
        layerList.getColumnModel().getColumn(0).setMaxWidth(12);
        layerList.getColumnModel().getColumn(0).setPreferredWidth(12);
        layerList.getColumnModel().getColumn(0).setResizable(false);

        layerList.getColumnModel().getColumn(1).setCellRenderer(new NativeScaleLayerCellRenderer());
        layerList.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new NativeScaleLayerCheckBox()));
        layerList.getColumnModel().getColumn(1).setMaxWidth(12);
        layerList.getColumnModel().getColumn(1).setPreferredWidth(12);
        layerList.getColumnModel().getColumn(1).setResizable(false);

        layerList.getColumnModel().getColumn(2).setCellRenderer(new LayerVisibleCellRenderer());
        layerList.getColumnModel().getColumn(2).setCellEditor(new LayerVisibleCellEditor(new LayerVisibleCheckBox()));
        layerList.getColumnModel().getColumn(2).setMaxWidth(16);
        layerList.getColumnModel().getColumn(2).setPreferredWidth(16);
        layerList.getColumnModel().getColumn(2).setResizable(false);

        layerList.getColumnModel().getColumn(3).setCellRenderer(new LayerNameCellRenderer());
        layerList.getColumnModel().getColumn(3).setCellEditor(new LayerNameCellEditor(new DisableShortcutsOnFocusGainedTextField()));
        // Disable some default JTable shortcuts to use JOSM ones (see #5678, #10458)
        for (KeyStroke ks : new KeyStroke[] {
                KeyStroke.getKeyStroke(KeyEvent.VK_C, GuiHelper.getMenuShortcutKeyMaskEx()),
                KeyStroke.getKeyStroke(KeyEvent.VK_V, GuiHelper.getMenuShortcutKeyMaskEx()),
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0),
        }) {
            layerList.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, new Object());
        }

        // init the model
        //
        final MapView mapView = mapFrame.mapView;
        model.populate();
        model.setSelectedLayer(mapView.getActiveLayer());
        model.addLayerListModelListener(
                new LayerListModelListener() {
                    @Override
                    public void makeVisible(int row, Layer layer) {
                        layerList.scrollToVisible(row, 0);
                        layerList.repaint();
                    }

                    @Override
                    public void refresh() {
                        layerList.repaint();
                    }
                }
                );

        // -- move up action
        MoveUpAction moveUpAction = new MoveUpAction();
        adaptTo(moveUpAction, model);
        adaptTo(moveUpAction, selectionModel);

        // -- move down action
        MoveDownAction moveDownAction = new MoveDownAction();
        adaptTo(moveDownAction, model);
        adaptTo(moveDownAction, selectionModel);

        // -- activate action
        activateLayerAction = new ActivateLayerAction();
        activateLayerAction.updateEnabledState();
        MultikeyActionsHandler.getInstance().addAction(activateLayerAction);
        adaptTo(activateLayerAction, selectionModel);

        JumpToMarkerActions.initialize();

        // -- show hide action
        showHideLayerAction = new ShowHideLayerAction();
        MultikeyActionsHandler.getInstance().addAction(showHideLayerAction);
        adaptTo(showHideLayerAction, selectionModel);

        LayerVisibilityAction visibilityAction = new LayerVisibilityAction(model);
        adaptTo(visibilityAction, selectionModel);
        SideButton visibilityButton = new SideButton(visibilityAction, false);
        visibilityAction.setCorrespondingSideButton(visibilityButton);

        // -- delete layer action
        DeleteLayerAction deleteLayerAction = new DeleteLayerAction();
        layerList.getActionMap().put("deleteLayer", deleteLayerAction);
        adaptTo(deleteLayerAction, selectionModel);
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete"
                );
        getActionMap().put("delete", deleteLayerAction);

        // Activate layer on Enter key press
        InputMapUtils.addEnterAction(layerList, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                activateLayerAction.actionPerformed(null);
                layerList.requestFocus();
            }
        });

        // Show/Activate layer on Enter key press
        InputMapUtils.addSpacebarAction(layerList, showHideLayerAction);

        createLayout(layerList, true, Arrays.asList(
                new SideButton(moveUpAction, false),
                new SideButton(moveDownAction, false),
                new SideButton(activateLayerAction, false),
                visibilityButton,
                new SideButton(deleteLayerAction, false)
        ));

        createVisibilityToggleShortcuts();
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

    /**
     * Returns the layer list model.
     * @return the layer list model
     */
    public LayerListModel getModel() {
        return model;
    }

    protected interface IEnabledStateUpdating {
        void updateEnabledState();
    }

    /**
     * Wires <code>listener</code> to <code>listSelectionModel</code> in such a way, that
     * <code>listener</code> receives a {@link IEnabledStateUpdating#updateEnabledState()}
     * on every {@link ListSelectionEvent}.
     *
     * @param listener  the listener
     * @param listSelectionModel  the source emitting {@link ListSelectionEvent}s
     */
    protected void adaptTo(final IEnabledStateUpdating listener, ListSelectionModel listSelectionModel) {
        listSelectionModel.addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        listener.updateEnabledState();
                    }
                }
                );
    }

    /**
     * Wires <code>listener</code> to <code>listModel</code> in such a way, that
     * <code>listener</code> receives a {@link IEnabledStateUpdating#updateEnabledState()}
     * on every {@link ListDataEvent}.
     *
     * @param listener the listener
     * @param listModel the source emitting {@link ListDataEvent}s
     */
    protected void adaptTo(final IEnabledStateUpdating listener, LayerListModel listModel) {
        listModel.addTableModelListener(
                new TableModelListener() {

                    @Override
                    public void tableChanged(TableModelEvent e) {
                        listener.updateEnabledState();
                    }
                }
                );
    }

    @Override
    public void destroy() {
        for (int i = 0; i < 10; i++) {
            Main.unregisterActionShortcut(visibilityToggleActions[i], visibilityToggleShortcuts[i]);
        }
        MultikeyActionsHandler.getInstance().removeAction(activateLayerAction);
        MultikeyActionsHandler.getInstance().removeAction(showHideLayerAction);
        JumpToMarkerActions.unregisterActions();
        super.destroy();
        instance = null;
    }

    /**
     * The action to delete the currently selected layer
     */
    public final class DeleteLayerAction extends AbstractAction implements IEnabledStateUpdating, LayerAction {

        /**
         * Creates a {@link DeleteLayerAction} which will delete the currently
         * selected layers in the layer dialog.
         */
        public DeleteLayerAction() {
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            putValue(SHORT_DESCRIPTION, tr("Delete the selected layers."));
            putValue(NAME, tr("Delete"));
            putValue("help", HelpUtil.ht("/Dialog/LayerList#DeleteLayer"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            List<Layer> selectedLayers = getModel().getSelectedLayers();
            if (selectedLayers.isEmpty())
                return;
            if (!Main.saveUnsavedModifications(selectedLayers, false))
                return;
            for (Layer l: selectedLayers) {
                Main.main.removeLayer(l);
            }
        }

        @Override
        public void updateEnabledState() {
            setEnabled(!getModel().getSelectedLayers().isEmpty());
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

    /**
     * Action which will toggle the visibility of the currently selected layers.
     */
    public final class ShowHideLayerAction extends AbstractAction implements IEnabledStateUpdating, LayerAction, MultikeyShortcutAction {

        private transient WeakReference<Layer> lastLayer;
        private final transient Shortcut multikeyShortcut;

        /**
         * Creates a {@link ShowHideLayerAction} which will toggle the visibility of
         * the currently selected layers
         */
        public ShowHideLayerAction() {
            putValue(NAME, tr("Show/hide"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "showhide"));
            putValue(SHORT_DESCRIPTION, tr("Toggle visible state of the selected layer."));
            putValue("help", HelpUtil.ht("/Dialog/LayerList#ShowHideLayer"));
            multikeyShortcut = Shortcut.registerShortcut("core_multikey:showHideLayer", tr("Multikey: {0}",
                    tr("Show/hide layer")), KeyEvent.VK_S, Shortcut.SHIFT);
            multikeyShortcut.setAccelerator(this);
            updateEnabledState();
        }

        @Override
        public Shortcut getMultikeyShortcut() {
            return multikeyShortcut;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (Layer l : model.getSelectedLayers()) {
                l.toggleVisible();
            }
        }

        @Override
        public void executeMultikeyAction(int index, boolean repeat) {
            Layer l = LayerListDialog.getLayerForIndex(index);
            if (l != null) {
                l.toggleVisible();
                lastLayer = new WeakReference<>(l);
            } else if (repeat && lastLayer != null) {
                l = lastLayer.get();
                if (LayerListDialog.isLayerValid(l)) {
                    l.toggleVisible();
                }
            }
        }

        @Override
        public void updateEnabledState() {
            setEnabled(!model.getSelectedLayers().isEmpty());
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

        @Override
        public List<MultikeyInfo> getMultikeyCombinations() {
            return LayerListDialog.getLayerInfoByClass(Layer.class);
        }

        @Override
        public MultikeyInfo getLastMultikeyAction() {
            if (lastLayer != null)
                return LayerListDialog.getLayerInfo(lastLayer.get());
            return null;
        }
    }

    /**
     * This is a menu that includes all settings for the layer visibility. It combines gamma/opacity sliders and the visible-checkbox.
     *
     * @author Michael Zangl
     */
    public static final class LayerVisibilityAction extends AbstractAction implements IEnabledStateUpdating, LayerAction {
        protected static final int SLIDER_STEPS = 100;
        private static final double MAX_GAMMA_FACTOR = 2;
        private static final double MAX_SHARPNESS_FACTOR = 2;
        private static final double MAX_COLORFUL_FACTOR = 2;
        private final LayerListModel model;
        private final JPopupMenu popup;
        private SideButton sideButton;
        private JCheckBox visibilityCheckbox;
        final OpacitySlider opacitySlider = new OpacitySlider();
        private final ArrayList<FilterSlider<?>> sliders = new ArrayList<>();

        /**
         * Creates a new {@link LayerVisibilityAction}
         * @param model The list to get the selection from.
         */
        public LayerVisibilityAction(LayerListModel model) {
            this.model = model;
            popup = new JPopupMenu();

            // just to add a border
            JPanel content = new JPanel();
            popup.add(content);
            content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            content.setLayout(new GridBagLayout());

            putValue(SMALL_ICON, ImageProvider.get("dialogs/layerlist", "visibility"));
            putValue(SHORT_DESCRIPTION, tr("Change visibility of the selected layer."));

            visibilityCheckbox = new JCheckBox(tr("Show layer"));
            visibilityCheckbox.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    setVisibleFlag(visibilityCheckbox.isSelected());
                }
            });
            content.add(visibilityCheckbox, GBC.eop());

            addSlider(content, opacitySlider);
            addSlider(content, new ColorfulnessSlider());
            addSlider(content, new GammaFilterSlider());
            addSlider(content, new SharpnessSlider());
        }

        private void addSlider(JPanel content, FilterSlider<?> slider) {
            content.add(new JLabel(slider.getIcon()), GBC.std().span(1, 2).insets(0, 0, 5, 0));
            content.add(new JLabel(slider.getLabel()), GBC.eol());
            content.add(slider, GBC.eop());
            sliders.add(slider);
        }

        protected void setVisibleFlag(boolean visible) {
            for (Layer l : model.getSelectedLayers()) {
                l.setVisible(visible);
            }
            updateValues();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            updateValues();
            if (e.getSource() == sideButton) {
                popup.show(sideButton, 0, sideButton.getHeight());
            } else {
                // Action can be trigger either by opacity button or by popup menu (in case toggle buttons are hidden).
                // In that case, show it in the middle of screen (because opacityButton is not visible)
                popup.show(Main.parent, Main.parent.getWidth() / 2, (Main.parent.getHeight() - popup.getHeight()) / 2);
            }
        }

        protected void updateValues() {
            List<Layer> layers = model.getSelectedLayers();

            visibilityCheckbox.setEnabled(!layers.isEmpty());
            boolean allVisible = true;
            boolean allHidden = true;
            for (Layer l : layers) {
                allVisible &= l.isVisible();
                allHidden &= !l.isVisible();
            }
            // TODO: Indicate tristate.
            visibilityCheckbox.setSelected(allVisible && !allHidden);

            for (FilterSlider<?> slider : sliders) {
                slider.updateSlider(layers, allHidden);
            }
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return !layers.isEmpty();
        }

        @Override
        public Component createMenuComponent() {
            return new JMenuItem(this);
        }

        @Override
        public void updateEnabledState() {
            setEnabled(!model.getSelectedLayers().isEmpty());
        }

        /**
         * Sets the corresponding side button.
         * @param sideButton the corresponding side button
         */
        void setCorrespondingSideButton(SideButton sideButton) {
            this.sideButton = sideButton;
        }

        /**
         * This is a slider for a filter value.
         * @author Michael Zangl
         *
         * @param <T> The layer type.
         */
        private abstract class FilterSlider<T extends Layer> extends JSlider {
            private final double minValue;
            private final double maxValue;
            private final Class<T> layerClassFilter;

            /**
             * Create a new filter slider.
             * @param minValue The minimum value to map to the left side.
             * @param maxValue The maximum value to map to the right side.
             * @param layerClassFilter The type of layer influenced by this filter.
             */
            FilterSlider(double minValue, double maxValue, Class<T> layerClassFilter) {
                super(JSlider.HORIZONTAL);
                this.minValue = minValue;
                this.maxValue = maxValue;
                this.layerClassFilter = layerClassFilter;
                setMaximum(SLIDER_STEPS);
                int tick = convertFromRealValue(1);
                setMinorTickSpacing(tick);
                setMajorTickSpacing(tick);
                setPaintTicks(true);

                addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        onStateChanged();
                    }
                });
            }

            /**
             * Called whenever the state of the slider was changed.
             * @see #getValueIsAdjusting()
             * @see #getRealValue()
             */
            protected void onStateChanged() {
                Collection<T> layers = filterLayers(model.getSelectedLayers());
                for (T layer : layers) {
                    applyValueToLayer(layer);
                }
            }

            protected void applyValueToLayer(T layer) {
            }

            protected double getRealValue() {
                return convertToRealValue(getValue());
            }

            protected double convertToRealValue(int value) {
                double s = (double) value / SLIDER_STEPS;
                return s * maxValue + (1-s) * minValue;
            }

            protected void setRealValue(double value) {
                setValue(convertFromRealValue(value));
            }

            protected int convertFromRealValue(double value) {
                int i = (int) ((value - minValue) / (maxValue - minValue) * SLIDER_STEPS + .5);
                if (i < getMinimum()) {
                    return getMinimum();
                } else if (i > getMaximum()) {
                    return getMaximum();
                } else {
                    return i;
                }
            }

            public abstract ImageIcon getIcon();

            public abstract String getLabel();

            public void updateSlider(List<Layer> layers, boolean allHidden) {
                Collection<? extends Layer> usedLayers = filterLayers(layers);
                if (usedLayers.isEmpty() || allHidden) {
                    setEnabled(false);
                } else {
                    setEnabled(true);
                    updateSliderWhileEnabled(usedLayers, allHidden);
                }
            }

            protected Collection<T> filterLayers(List<Layer> layers) {
                return Utils.filteredCollection(layers, layerClassFilter);
            }

            protected abstract void updateSliderWhileEnabled(Collection<? extends Layer> usedLayers, boolean allHidden);
        }

        /**
         * This slider allows you to change the opacity of a layer.
         *
         * @author Michael Zangl
         * @see Layer#setOpacity(double)
         */
        class OpacitySlider extends FilterSlider<Layer> {
            /**
             * Creaate a new {@link OpacitySlider}.
             */
            OpacitySlider() {
                super(0, 1, Layer.class);
                setToolTipText(tr("Adjust opacity of the layer."));

            }

            @Override
            protected void onStateChanged() {
                if (getRealValue() <= 0.001 && !getValueIsAdjusting()) {
                    setVisibleFlag(false);
                } else {
                    super.onStateChanged();
                }
            }

            @Override
            protected void applyValueToLayer(Layer layer) {
                layer.setOpacity(getRealValue());
            }

            @Override
            protected void updateSliderWhileEnabled(Collection<? extends Layer> usedLayers, boolean allHidden) {
                double opacity = 0;
                for (Layer l : usedLayers) {
                    opacity += l.getOpacity();
                }
                opacity /= usedLayers.size();
                if (opacity == 0) {
                    opacity = 1;
                    setVisibleFlag(true);
                }
                setRealValue(opacity);
            }

            @Override
            public String getLabel() {
                return tr("Opacity");
            }

            @Override
            public ImageIcon getIcon() {
                return ImageProvider.get("dialogs/layerlist", "transparency");
            }

            @Override
            public String toString() {
                return "OpacitySlider [getRealValue()=" + getRealValue() + "]";
            }
        }

        /**
         * This slider allows you to change the gamma value of a layer.
         *
         * @author Michael Zangl
         * @see ImageryLayer#setGamma(double)
         */
        private class GammaFilterSlider extends FilterSlider<ImageryLayer> {

            /**
             * Create a new {@link GammaFilterSlider}
             */
            GammaFilterSlider() {
                super(0, MAX_GAMMA_FACTOR, ImageryLayer.class);
                setToolTipText(tr("Adjust gamma value of the layer."));
            }

            @Override
            protected void updateSliderWhileEnabled(Collection<? extends Layer> usedLayers, boolean allHidden) {
                double gamma = ((ImageryLayer) usedLayers.iterator().next()).getGamma();
                setRealValue(gamma);
            }

            @Override
            protected void applyValueToLayer(ImageryLayer layer) {
                layer.setGamma(getRealValue());
            }

            @Override
            public ImageIcon getIcon() {
               return ImageProvider.get("dialogs/layerlist", "gamma");
            }

            @Override
            public String getLabel() {
                return tr("Gamma");
            }
        }

        /**
         * This slider allows you to change the sharpness of a layer.
         *
         * @author Michael Zangl
         * @see ImageryLayer#setSharpenLevel(double)
         */
        private class SharpnessSlider extends FilterSlider<ImageryLayer> {

            /**
             * Creates a new {@link SharpnessSlider}
             */
            SharpnessSlider() {
                super(0, MAX_SHARPNESS_FACTOR, ImageryLayer.class);
                setToolTipText(tr("Adjust sharpness/blur value of the layer."));
            }

            @Override
            protected void updateSliderWhileEnabled(Collection<? extends Layer> usedLayers, boolean allHidden) {
                setRealValue(((ImageryLayer) usedLayers.iterator().next()).getSharpenLevel());
            }

            @Override
            protected void applyValueToLayer(ImageryLayer layer) {
                layer.setSharpenLevel(getRealValue());
            }

            @Override
            public ImageIcon getIcon() {
               return ImageProvider.get("dialogs/layerlist", "sharpness");
            }

            @Override
            public String getLabel() {
                return tr("Sharpness");
            }
        }

        /**
         * This slider allows you to change the colorfulness of a layer.
         *
         * @author Michael Zangl
         * @see ImageryLayer#setColorfulness(double)
         */
        private class ColorfulnessSlider extends FilterSlider<ImageryLayer> {

            /**
             * Create a new {@link ColorfulnessSlider}
             */
            ColorfulnessSlider() {
                super(0, MAX_COLORFUL_FACTOR, ImageryLayer.class);
                setToolTipText(tr("Adjust colorfulness of the layer."));
            }

            @Override
            protected void updateSliderWhileEnabled(Collection<? extends Layer> usedLayers, boolean allHidden) {
                setRealValue(((ImageryLayer) usedLayers.iterator().next()).getColorfulness());
            }

            @Override
            protected void applyValueToLayer(ImageryLayer layer) {
                layer.setColorfulness(getRealValue());
            }

            @Override
            public ImageIcon getIcon() {
               return ImageProvider.get("dialogs/layerlist", "colorfulness");
            }

            @Override
            public String getLabel() {
                return tr("Colorfulness");
            }
        }
    }

    /**
     * The action to activate the currently selected layer
     */

    public final class ActivateLayerAction extends AbstractAction
    implements IEnabledStateUpdating, MapView.LayerChangeListener, MultikeyShortcutAction {
        private transient Layer layer;
        private transient Shortcut multikeyShortcut;

        /**
         * Constructs a new {@code ActivateLayerAction}.
         * @param layer the layer
         */
        public ActivateLayerAction(Layer layer) {
            this();
            CheckParameterUtil.ensureParameterNotNull(layer, "layer");
            this.layer = layer;
            putValue(NAME, tr("Activate"));
            updateEnabledState();
        }

        /**
         * Constructs a new {@code ActivateLayerAction}.
         */
        public ActivateLayerAction() {
            putValue(NAME, tr("Activate"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "activate"));
            putValue(SHORT_DESCRIPTION, tr("Activate the selected layer"));
            multikeyShortcut = Shortcut.registerShortcut("core_multikey:activateLayer", tr("Multikey: {0}",
                    tr("Activate layer")), KeyEvent.VK_A, Shortcut.SHIFT);
            multikeyShortcut.setAccelerator(this);
            putValue("help", HelpUtil.ht("/Dialog/LayerList#ActivateLayer"));
        }

        @Override
        public Shortcut getMultikeyShortcut() {
            return multikeyShortcut;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Layer toActivate;
            if (layer != null) {
                toActivate = layer;
            } else {
                toActivate = model.getSelectedLayers().get(0);
            }
            execute(toActivate);
        }

        private void execute(Layer layer) {
            // model is  going to be updated via LayerChangeListener and PropertyChangeEvents
            Main.map.mapView.setActiveLayer(layer);
            layer.setVisible(true);
        }

        protected boolean isActiveLayer(Layer layer) {
            if (!Main.isDisplayingMapView()) return false;
            return Main.map.mapView.getActiveLayer() == layer;
        }

        @Override
        public void updateEnabledState() {
            GuiHelper.runInEDTAndWait(new Runnable() {
                @Override
                public void run() {
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
            });
        }

        @Override
        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            updateEnabledState();
        }

        @Override
        public void layerAdded(Layer newLayer) {
            updateEnabledState();
        }

        @Override
        public void layerRemoved(Layer oldLayer) {
            updateEnabledState();
        }

        @Override
        public void executeMultikeyAction(int index, boolean repeat) {
            Layer l = LayerListDialog.getLayerForIndex(index);
            if (l != null) {
                execute(l);
            }
        }

        @Override
        public List<MultikeyInfo> getMultikeyCombinations() {
            return LayerListDialog.getLayerInfoByClass(Layer.class);
        }

        @Override
        public MultikeyInfo getLastMultikeyAction() {
            return null; // Repeating action doesn't make much sense for activating
        }
    }

    /**
     * The action to merge the currently selected layer into another layer.
     */
    public final class MergeAction extends AbstractAction implements IEnabledStateUpdating, LayerAction, Layer.MultiLayerAction {
        private transient Layer layer;
        private transient List<Layer> layers;

        /**
         * Constructs a new {@code MergeAction}.
         * @param layer the layer
         * @throws IllegalArgumentException if {@code layer} is null
         */
        public MergeAction(Layer layer) {
            this(layer, null);
            CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        }

        /**
         * Constructs a new {@code MergeAction}.
         * @param layers the layer list
         * @throws IllegalArgumentException if {@code layers} is null
         */
        public MergeAction(List<Layer> layers) {
            this(null, layers);
            CheckParameterUtil.ensureParameterNotNull(layers, "layers");
        }

        /**
         * Constructs a new {@code MergeAction}.
         * @param layer the layer (null if layer list if specified)
         * @param layers the layer list (null if a single layer is specified)
         */
        private MergeAction(Layer layer, List<Layer> layers) {
            this.layer = layer;
            this.layers = layers;
            putValue(NAME, tr("Merge"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "mergedown"));
            putValue(SHORT_DESCRIPTION, tr("Merge this layer into another layer"));
            putValue("help", HelpUtil.ht("/Dialog/LayerList#MergeLayer"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (layer != null) {
                Main.main.menu.merge.merge(layer);
            } else if (layers != null) {
                Main.main.menu.merge.merge(layers);
            } else {
                if (getModel().getSelectedLayers().size() == 1) {
                    Layer selectedLayer = getModel().getSelectedLayers().get(0);
                    Main.main.menu.merge.merge(selectedLayer);
                } else {
                    Main.main.menu.merge.merge(getModel().getSelectedLayers());
                }
            }
        }

        @Override
        public void updateEnabledState() {
            if (layer == null && layers == null) {
                if (getModel().getSelectedLayers().isEmpty()) {
                    setEnabled(false);
                } else  if (getModel().getSelectedLayers().size() > 1) {
                    setEnabled(supportLayers(getModel().getSelectedLayers()));
                } else {
                    Layer selectedLayer = getModel().getSelectedLayers().get(0);
                    List<Layer> targets = getModel().getPossibleMergeTargets(selectedLayer);
                    setEnabled(!targets.isEmpty());
                }
            } else if (layer != null) {
                List<Layer> targets = getModel().getPossibleMergeTargets(layer);
                setEnabled(!targets.isEmpty());
            } else {
                setEnabled(supportLayers(layers));
            }
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            if (layers.isEmpty()) {
                return false;
            } else {
                final Layer firstLayer = layers.get(0);
                final List<Layer> remainingLayers = layers.subList(1, layers.size());
                return getModel().getPossibleMergeTargets(firstLayer).containsAll(remainingLayers);
            }
        }

        @Override
        public Component createMenuComponent() {
            return new JMenuItem(this);
        }

        @Override
        public MergeAction getMultiLayerAction(List<Layer> layers) {
            return new MergeAction(layers);
        }
    }

    /**
     * The action to merge the currently selected layer into another layer.
     */
    public final class DuplicateAction extends AbstractAction implements IEnabledStateUpdating {
        private transient Layer layer;

        /**
         * Constructs a new {@code DuplicateAction}.
         * @param layer the layer
         * @throws IllegalArgumentException if {@code layer} is null
         */
        public DuplicateAction(Layer layer) {
            this();
            CheckParameterUtil.ensureParameterNotNull(layer, "layer");
            this.layer = layer;
            updateEnabledState();
        }

        /**
         * Constructs a new {@code DuplicateAction}.
         */
        public DuplicateAction() {
            putValue(NAME, tr("Duplicate"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "duplicatelayer"));
            putValue(SHORT_DESCRIPTION, tr("Duplicate this layer"));
            putValue("help", HelpUtil.ht("/Dialog/LayerList#DuplicateLayer"));
            updateEnabledState();
        }

        private void duplicate(Layer layer) {
            if (!Main.isDisplayingMapView())
                return;

            List<String> layerNames = new ArrayList<>();
            for (Layer l: Main.map.mapView.getAllLayers()) {
                layerNames.add(l.getName());
            }
            if (layer instanceof OsmDataLayer) {
                OsmDataLayer oldLayer = (OsmDataLayer) layer;
                // Translators: "Copy of {layer name}"
                String newName = tr("Copy of {0}", oldLayer.getName());
                int i = 2;
                while (layerNames.contains(newName)) {
                    // Translators: "Copy {number} of {layer name}"
                    newName = tr("Copy {1} of {0}", oldLayer.getName(), i);
                    i++;
                }
                Main.main.addLayer(new OsmDataLayer(oldLayer.data.clone(), newName, null));
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (layer != null) {
                duplicate(layer);
            } else {
                duplicate(getModel().getSelectedLayers().get(0));
            }
        }

        @Override
        public void updateEnabledState() {
            if (layer == null) {
                if (getModel().getSelectedLayers().size() == 1) {
                    setEnabled(getModel().getSelectedLayers().get(0) instanceof OsmDataLayer);
                } else {
                    setEnabled(false);
                }
            } else {
                setEnabled(layer instanceof OsmDataLayer);
            }
        }
    }

    private static class ActiveLayerCheckBox extends JCheckBox {
        ActiveLayerCheckBox() {
            setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            ImageIcon blank = ImageProvider.get("dialogs/layerlist", "blank");
            ImageIcon active = ImageProvider.get("dialogs/layerlist", "active");
            setIcon(blank);
            setSelectedIcon(active);
            setRolloverIcon(blank);
            setRolloverSelectedIcon(active);
            setPressedIcon(ImageProvider.get("dialogs/layerlist", "active-pressed"));
        }
    }

    private static class LayerVisibleCheckBox extends JCheckBox {
        private final ImageIcon iconEye;
        private final ImageIcon iconEyeTranslucent;
        private boolean isTranslucent;

        /**
         * Constructs a new {@code LayerVisibleCheckBox}.
         */
        LayerVisibleCheckBox() {
            setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            iconEye = ImageProvider.get("dialogs/layerlist", "eye");
            iconEyeTranslucent = ImageProvider.get("dialogs/layerlist", "eye-translucent");
            setIcon(ImageProvider.get("dialogs/layerlist", "eye-off"));
            setPressedIcon(ImageProvider.get("dialogs/layerlist", "eye-pressed"));
            setSelectedIcon(iconEye);
            isTranslucent = false;
        }

        public void setTranslucent(boolean isTranslucent) {
            if (this.isTranslucent == isTranslucent) return;
            if (isTranslucent) {
                setSelectedIcon(iconEyeTranslucent);
            } else {
                setSelectedIcon(iconEye);
            }
            this.isTranslucent = isTranslucent;
        }

        public void updateStatus(Layer layer) {
            boolean visible = layer.isVisible();
            setSelected(visible);
            setTranslucent(layer.getOpacity() < 1.0);
            setToolTipText(visible ?
                tr("layer is currently visible (click to hide layer)") :
                tr("layer is currently hidden (click to show layer)"));
        }
    }

    private static class NativeScaleLayerCheckBox extends JCheckBox {
        NativeScaleLayerCheckBox() {
            setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            ImageIcon blank = ImageProvider.get("dialogs/layerlist", "blank");
            ImageIcon active = ImageProvider.get("dialogs/layerlist", "scale");
            setIcon(blank);
            setSelectedIcon(active);
        }
    }

    private static class ActiveLayerCellRenderer implements TableCellRenderer {
        private final JCheckBox cb;

        /**
         * Constructs a new {@code ActiveLayerCellRenderer}.
         */
        ActiveLayerCellRenderer() {
            cb = new ActiveLayerCheckBox();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            boolean active = value != null && (Boolean) value;
            cb.setSelected(active);
            cb.setToolTipText(active ? tr("this layer is the active layer") : tr("this layer is not currently active (click to activate)"));
            return cb;
        }
    }

    private static class LayerVisibleCellRenderer implements TableCellRenderer {
        private final LayerVisibleCheckBox cb;

        /**
         * Constructs a new {@code LayerVisibleCellRenderer}.
         */
        LayerVisibleCellRenderer() {
            this.cb = new LayerVisibleCheckBox();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null) {
                cb.updateStatus((Layer) value);
            }
            return cb;
        }
    }

    private static class LayerVisibleCellEditor extends DefaultCellEditor {
        private final LayerVisibleCheckBox cb;

        LayerVisibleCellEditor(LayerVisibleCheckBox cb) {
            super(cb);
            this.cb = cb;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            cb.updateStatus((Layer) value);
            return cb;
        }
    }

    private static class NativeScaleLayerCellRenderer implements TableCellRenderer {
        private final JCheckBox cb;

        /**
         * Constructs a new {@code ActiveLayerCellRenderer}.
         */
        NativeScaleLayerCellRenderer() {
            cb = new NativeScaleLayerCheckBox();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Layer layer = (Layer) value;
            if (layer instanceof NativeScaleLayer) {
                boolean active = ((NativeScaleLayer) layer) == Main.map.mapView.getNativeScaleLayer();
                cb.setSelected(active);
                cb.setToolTipText(active
                    ? tr("scale follows native resolution of this layer")
                    : tr("scale follows native resolution of another layer (click to set this layer)")
                );
            } else {
                cb.setSelected(false);
                cb.setToolTipText(tr("this layer has no native resolution"));
            }
            return cb;
        }
    }

    private class LayerNameCellRenderer extends DefaultTableCellRenderer {

        protected boolean isActiveLayer(Layer layer) {
            if (!Main.isDisplayingMapView()) return false;
            return Main.map.mapView.getActiveLayer() == layer;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null)
                return this;
            Layer layer = (Layer) value;
            JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                    layer.getName(), isSelected, hasFocus, row, column);
            if (isActiveLayer(layer)) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
            if (Main.pref.getBoolean("dialog.layer.colorname", true)) {
                Color c = layer.getColor(false);
                if (c != null) {
                    Color oc = null;
                    for (Layer l : model.getLayers()) {
                        oc = l.getColor(false);
                        if (oc != null) {
                            if (oc.equals(c)) {
                                oc = null;
                            } else {
                                break;
                            }
                        }
                    }
                    /* not more than one color, don't use coloring */
                    if (oc == null) {
                        c = null;
                    }
                }
                if (c == null) {
                    c = UIManager.getColor(isSelected ? "Table.selectionForeground" : "Table.foreground");
                }
                label.setForeground(c);
            }
            label.setIcon(layer.getIcon());
            label.setToolTipText(layer.getToolTipText());
            return label;
        }
    }

    private static class LayerNameCellEditor extends DefaultCellEditor {
        LayerNameCellEditor(DisableShortcutsOnFocusGainedTextField tf) {
            super(tf);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            JosmTextField tf = (JosmTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);
            tf.setText(value == null ? "" : ((Layer) value).getName());
            return tf;
        }
    }

    class PopupMenuHandler extends PopupMenuLauncher {
        @Override
        public void showMenu(MouseEvent evt) {
            menu = new LayerListPopup(getModel().getSelectedLayers());
            super.showMenu(evt);
        }
    }

    /**
     * The action to move up the currently selected entries in the list.
     */
    class MoveUpAction extends AbstractAction implements  IEnabledStateUpdating {
        MoveUpAction() {
            putValue(NAME, tr("Move up"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "up"));
            putValue(SHORT_DESCRIPTION, tr("Move the selected layer one row up."));
            updateEnabledState();
        }

        @Override
        public void updateEnabledState() {
            setEnabled(model.canMoveUp());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.moveUp();
        }
    }

    /**
     * The action to move down the currently selected entries in the list.
     */
    class MoveDownAction extends AbstractAction implements IEnabledStateUpdating {
        MoveDownAction() {
            putValue(NAME, tr("Move down"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "down"));
            putValue(SHORT_DESCRIPTION, tr("Move the selected layer one row down."));
            updateEnabledState();
        }

        @Override
        public void updateEnabledState() {
            setEnabled(model.canMoveDown());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.moveDown();
        }
    }

    /**
     * Observer interface to be implemented by views using {@link LayerListModel}.
     */
    public interface LayerListModelListener {

        /**
         * Fired when a layer is made visible.
         * @param index the layer index
         * @param layer the layer
         */
        void makeVisible(int index, Layer layer);


        /**
         * Fired when something has changed in the layer list model.
         */
        void refresh();
    }

    /**
     * The layer list model. The model manages a list of layers and provides methods for
     * moving layers up and down, for toggling their visibility, and for activating a layer.
     *
     * The model is a {@link TableModel} and it provides a {@link ListSelectionModel}. It expects
     * to be configured with a {@link DefaultListSelectionModel}. The selection model is used
     * to update the selection state of views depending on messages sent to the model.
     *
     * The model manages a list of {@link LayerListModelListener} which are mainly notified if
     * the model requires views to make a specific list entry visible.
     *
     * It also listens to {@link PropertyChangeEvent}s of every {@link Layer} it manages, in particular to
     * the properties {@link Layer#VISIBLE_PROP} and {@link Layer#NAME_PROP}.
     */
    public static final class LayerListModel extends AbstractTableModel implements MapView.LayerChangeListener, PropertyChangeListener {
        /** manages list selection state*/
        private final DefaultListSelectionModel selectionModel;
        private final CopyOnWriteArrayList<LayerListModelListener> listeners;
        private LayerList layerList;

        /**
         * constructor
         *
         * @param selectionModel the list selection model
         */
        LayerListModel(DefaultListSelectionModel selectionModel) {
            this.selectionModel = selectionModel;
            listeners = new CopyOnWriteArrayList<>();
        }

        void setlayerList(LayerList layerList) {
            this.layerList = layerList;
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
         * Populates the model with the current layers managed by {@link MapView}.
         */
        public void populate() {
            for (Layer layer: getLayers()) {
                // make sure the model is registered exactly once
                layer.removePropertyChangeListener(this);
                layer.addPropertyChangeListener(this);
            }
            fireTableDataChanged();
        }

        /**
         * Marks <code>layer</code> as selected layer. Ignored, if layer is null.
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
            ensureSelectedIsVisible();
        }

        /**
         * Replies the list of currently selected layers. Never null, but may be empty.
         *
         * @return the list of currently selected layers. Never null, but may be empty.
         */
        public List<Layer> getSelectedLayers() {
            List<Layer> selected = new ArrayList<>();
            List<Layer> layers = getLayers();
            for (int i = 0; i < layers.size(); i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    selected.add(layers.get(i));
                }
            }
            return selected;
        }

        /**
         * Replies a the list of indices of the selected rows. Never null, but may be empty.
         *
         * @return  the list of indices of the selected rows. Never null, but may be empty.
         */
        public List<Integer> getSelectedRows() {
            List<Integer> selected = new ArrayList<>();
            for (int i = 0; i < getLayers().size(); i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    selected.add(i);
                }
            }
            return selected;
        }

        /**
         * Invoked if a layer managed by {@link MapView} is removed
         *
         * @param layer the layer which is removed
         */
        protected void onRemoveLayer(Layer layer) {
            if (layer == null)
                return;
            layer.removePropertyChangeListener(this);
            final int size = getRowCount();
            final List<Integer> rows = getSelectedRows();
            GuiHelper.runInEDTAndWait(new Runnable() {
                @Override
                public void run() {
                    if (rows.isEmpty() && size > 0) {
                        selectionModel.setSelectionInterval(size-1, size-1);
                    }
                    fireTableDataChanged();
                    fireRefresh();
                    ensureActiveSelected();
                }
            });
        }

        /**
         * Invoked when a layer managed by {@link MapView} is added
         *
         * @param layer the layer
         */
        protected void onAddLayer(Layer layer) {
            if (layer == null) return;
            layer.addPropertyChangeListener(this);
            fireTableDataChanged();
            int idx = getLayers().indexOf(layer);
            layerList.setRowHeight(idx, Math.max(16, layer.getIcon().getIconHeight()));
            selectionModel.setSelectionInterval(idx, idx);
            ensureSelectedIsVisible();
        }

        /**
         * Replies the first layer. Null if no layers are present
         *
         * @return the first layer. Null if no layers are present
         */
        public Layer getFirstLayer() {
            if (getRowCount() == 0) return null;
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
            if (index < 0 || index >= getRowCount())
                return null;
            return getLayers().get(index);
        }

        /**
         * Replies true if the currently selected layers can move up by one position
         *
         * @return true if the currently selected layers can move up by one position
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
            List<Layer> layers = getLayers();
            for (int row : sel) {
                Layer l1 = layers.get(row);
                Layer l2 = layers.get(row-1);
                Main.map.mapView.moveLayer(l2, row);
                Main.map.mapView.moveLayer(l1, row-1);
            }
            fireTableDataChanged();
            selectionModel.clearSelection();
            for (int row : sel) {
                selectionModel.addSelectionInterval(row-1, row-1);
            }
            ensureSelectedIsVisible();
        }

        /**
         * Replies true if the currently selected layers can move down by one position
         *
         * @return true if the currently selected layers can move down by one position
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
            List<Layer> layers = getLayers();
            for (int row : sel) {
                Layer l1 = layers.get(row);
                Layer l2 = layers.get(row+1);
                Main.map.mapView.moveLayer(l1, row+1);
                Main.map.mapView.moveLayer(l2, row);
            }
            fireTableDataChanged();
            selectionModel.clearSelection();
            for (int row : sel) {
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
            if (index < 0) return;
            List<Layer> layers = getLayers();
            if (index >= layers.size()) return;
            Layer layer = layers.get(index);
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
            List<Layer> targets = new ArrayList<>();
            if (source == null || !Main.isDisplayingMapView()) {
                return targets;
            }
            for (Layer target : Main.map.mapView.getAllLayersAsList()) {
                if (source == target) {
                    continue;
                }
                if (target.isMergable(source) && source.isMergable(target)) {
                    targets.add(target);
                }
            }
            return targets;
        }

        /**
         * Replies the list of layers currently managed by {@link MapView}.
         * Never null, but can be empty.
         *
         * @return the list of layers currently managed by {@link MapView}.
         * Never null, but can be empty.
         */
        public List<Layer> getLayers() {
            if (!Main.isDisplayingMapView())
                return Collections.<Layer>emptyList();
            return Main.map.mapView.getAllLayersAsList();
        }

        /**
         * Ensures that at least one layer is selected in the layer dialog
         *
         */
        protected void ensureActiveSelected() {
            List<Layer> layers = getLayers();
            if (layers.isEmpty())
                return;
            final Layer activeLayer = getActiveLayer();
            if (activeLayer != null) {
                // there's an active layer - select it and make it visible
                int idx = layers.indexOf(activeLayer);
                selectionModel.setSelectionInterval(idx, idx);
                ensureSelectedIsVisible();
            } else {
                // no active layer - select the first one and make it visible
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
            if (!Main.isDisplayingMapView()) return null;
            return Main.map.mapView.getActiveLayer();
        }

        /**
         * Replies the scale layer. null, if no active layer is available
         *
         * @return the scale layer. null, if no active layer is available
         */
        protected NativeScaleLayer getNativeScaleLayer() {
            if (!Main.isDisplayingMapView()) return null;
            return Main.map.mapView.getNativeScaleLayer();
        }

        /* ------------------------------------------------------------------------------ */
        /* Interface TableModel                                                           */
        /* ------------------------------------------------------------------------------ */

        @Override
        public int getRowCount() {
            List<Layer> layers = getLayers();
            if (layers == null) return 0;
            return layers.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public Object getValueAt(int row, int col) {
            List<Layer> layers = getLayers();
            if (row >= 0 && row < layers.size()) {
                switch (col) {
                case 0: return layers.get(row) == getActiveLayer();
                case 1: return layers.get(row);
                case 2: return layers.get(row);
                case 3: return layers.get(row);
                default: throw new RuntimeException();
                }
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            if (col == 0 && getActiveLayer() == getLayers().get(row))
                return false;
            return true;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            List<Layer> layers = getLayers();
            if (row < layers.size()) {
                Layer l = layers.get(row);
                switch (col) {
                case 0:
                    Main.map.mapView.setActiveLayer(l);
                    l.setVisible(true);
                    break;
                case 1:
                    NativeScaleLayer oldLayer = Main.map.mapView.getNativeScaleLayer();
                    if (oldLayer == l) {
                        Main.map.mapView.setNativeScaleLayer(null);
                    } else if (l instanceof NativeScaleLayer) {
                        Main.map.mapView.setNativeScaleLayer((NativeScaleLayer) l);
                        if (oldLayer != null) {
                            int idx = getLayers().indexOf(oldLayer);
                            if (idx >= 0) {
                                fireTableCellUpdated(idx, col);
                            }
                        }
                    }
                    break;
                case 2:
                    l.setVisible((Boolean) value);
                    break;
                case 3:
                    l.rename((String) value);
                    break;
                default: throw new RuntimeException();
                }
                fireTableCellUpdated(row, col);
            }
        }

        /* ------------------------------------------------------------------------------ */
        /* Interface LayerChangeListener                                                  */
        /* ------------------------------------------------------------------------------ */
        @Override
        public void activeLayerChange(final Layer oldLayer, final Layer newLayer) {
            GuiHelper.runInEDTAndWait(new Runnable() {
                @Override
                public void run() {
                    if (oldLayer != null) {
                        int idx = getLayers().indexOf(oldLayer);
                        if (idx >= 0) {
                            fireTableRowsUpdated(idx, idx);
                        }
                    }

                    if (newLayer != null) {
                        int idx = getLayers().indexOf(newLayer);
                        if (idx >= 0) {
                            fireTableRowsUpdated(idx, idx);
                        }
                    }
                    ensureActiveSelected();
                }
            });
        }

        @Override
        public void layerAdded(Layer newLayer) {
            onAddLayer(newLayer);
        }

        @Override
        public void layerRemoved(final Layer oldLayer) {
            onRemoveLayer(oldLayer);
        }

        /* ------------------------------------------------------------------------------ */
        /* Interface PropertyChangeListener                                               */
        /* ------------------------------------------------------------------------------ */
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() instanceof Layer) {
                Layer layer = (Layer) evt.getSource();
                final int idx = getLayers().indexOf(layer);
                if (idx < 0) return;
                fireRefresh();
            }
        }
    }

    static class LayerList extends JTable {
        LayerList(LayerListModel dataModel) {
            super(dataModel);
            dataModel.setlayerList(this);
        }

        public void scrollToVisible(int row, int col) {
            if (!(getParent() instanceof JViewport))
                return;
            JViewport viewport = (JViewport) getParent();
            Rectangle rect = getCellRect(row, col, true);
            Point pt = viewport.getViewPosition();
            rect.setLocation(rect.x - pt.x, rect.y - pt.y);
            viewport.scrollRectToVisible(rect);
        }
    }

    /**
     * Creates a {@link ShowHideLayerAction} in the
     * context of this {@link LayerListDialog}.
     *
     * @return the action
     */
    public ShowHideLayerAction createShowHideLayerAction() {
        return new ShowHideLayerAction();
    }

    /**
     * Creates a {@link DeleteLayerAction} in the
     * context of this {@link LayerListDialog}.
     *
     * @return the action
     */
    public DeleteLayerAction createDeleteLayerAction() {
        return new DeleteLayerAction();
    }

    /**
     * Creates a {@link ActivateLayerAction} for <code>layer</code> in the
     * context of this {@link LayerListDialog}.
     *
     * @param layer the layer
     * @return the action
     */
    public ActivateLayerAction createActivateLayerAction(Layer layer) {
        return new ActivateLayerAction(layer);
    }

    /**
     * Creates a {@link MergeLayerAction} for <code>layer</code> in the
     * context of this {@link LayerListDialog}.
     *
     * @param layer the layer
     * @return the action
     */
    public MergeAction createMergeLayerAction(Layer layer) {
        return new MergeAction(layer);
    }

    /**
     * Creates a {@link DuplicateAction} for <code>layer</code> in the
     * context of this {@link LayerListDialog}.
     *
     * @param layer the layer
     * @return the action
     */
    public DuplicateAction createDuplicateLayerAction(Layer layer) {
        return new DuplicateAction(layer);
    }

    /**
     * Returns the layer at given index, or {@code null}.
     * @param index the index
     * @return the layer at given index, or {@code null} if index out of range
     */
    public static Layer getLayerForIndex(int index) {

        if (!Main.isDisplayingMapView())
            return null;

        List<Layer> layers = Main.map.mapView.getAllLayersAsList();

        if (index < layers.size() && index >= 0)
            return layers.get(index);
        else
            return null;
    }

    /**
     * Returns a list of info on all layers of a given class.
     * @param layerClass The layer class. This is not {@code Class<? extends Layer>} on purpose,
     *                   to allow asking for layers implementing some interface
     * @return list of info on all layers assignable from {@code layerClass}
     */
    public static List<MultikeyInfo> getLayerInfoByClass(Class<?> layerClass) {

        List<MultikeyInfo> result = new ArrayList<>();

        if (!Main.isDisplayingMapView())
            return result;

        List<Layer> layers = Main.map.mapView.getAllLayersAsList();

        int index = 0;
        for (Layer l: layers) {
            if (layerClass.isAssignableFrom(l.getClass())) {
                result.add(new MultikeyInfo(index, l.getName()));
            }
            index++;
        }

        return result;
    }

    /**
     * Determines if a layer is valid (contained in layer list).
     * @param l the layer
     * @return {@code true} if layer {@code l} is contained in current layer list
     */
    public static boolean isLayerValid(Layer l) {

        if (l == null || !Main.isDisplayingMapView())
            return false;

        return Main.map.mapView.getAllLayersAsList().contains(l);
    }

    /**
     * Returns info about layer.
     * @param l the layer
     * @return info about layer {@code l}
     */
    public static MultikeyInfo getLayerInfo(Layer l) {

        if (l == null || !Main.isDisplayingMapView())
            return null;

        int index = Main.map.mapView.getAllLayersAsList().indexOf(l);
        if (index < 0)
            return null;

        return new MultikeyInfo(index, l.getName());
    }
}
