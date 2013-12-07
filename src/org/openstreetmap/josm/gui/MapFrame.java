// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.LassoModeAction;
import org.openstreetmap.josm.actions.mapmode.DeleteAction;
import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.actions.mapmode.ExtrudeAction;
import org.openstreetmap.josm.actions.mapmode.ImproveWayAccuracyAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.ParallelWayAction;
import org.openstreetmap.josm.actions.mapmode.SelectAction;
import org.openstreetmap.josm.actions.mapmode.ZoomAction;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.NavigatableComponent.ViewportData;
import org.openstreetmap.josm.gui.dialogs.ChangesetDialog;
import org.openstreetmap.josm.gui.dialogs.CommandStackDialog;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel;
import org.openstreetmap.josm.gui.dialogs.FilterDialog;
import org.openstreetmap.josm.gui.dialogs.HistoryDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.MapPaintDialog;
import org.openstreetmap.josm.gui.dialogs.RelationListDialog;
import org.openstreetmap.josm.gui.dialogs.SelectionListDialog;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.dialogs.UserListDialog;
import org.openstreetmap.josm.gui.dialogs.ValidatorDialog;
import org.openstreetmap.josm.gui.dialogs.properties.PropertiesDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;


/**
 * One Map frame with one dataset behind. This is the container gui class whose
 * display can be set to the different views.
 *
 * @author imi
 */
public class MapFrame extends JPanel implements Destroyable, LayerChangeListener {

    /**
     * The current mode, this frame operates.
     */
    public MapMode mapMode;

    /**
     * The view control displayed.
     */
    public final MapView mapView;

    /**
     * The toolbar with the action icons. To add new toggle dialog buttons,
     * use addToggleDialog, to add a new map mode button use addMapMode.
     */
    private JComponent sideToolBar = new JToolBar(JToolBar.VERTICAL);
    private final ButtonGroup toolBarActionsGroup = new ButtonGroup();
    private final JToolBar toolBarActions = new JToolBar(JToolBar.VERTICAL);
    private final JToolBar toolBarToggle = new JToolBar(JToolBar.VERTICAL);

    private final List<ToggleDialog> allDialogs = new ArrayList<ToggleDialog>();
    private final List<MapMode> mapModes = new ArrayList<MapMode>();
    private final List<IconToggleButton> allDialogButtons = new ArrayList<IconToggleButton>();
    public final List<IconToggleButton> allMapModeButtons = new ArrayList<IconToggleButton>();

    private final ListAllButtonsAction listAllDialogsAction = new ListAllButtonsAction(allDialogButtons);
    private final ListAllButtonsAction listAllMapModesAction = new ListAllButtonsAction(allMapModeButtons);
    private final JButton listAllToggleDialogsButton = new JButton(listAllDialogsAction);
    private final JButton listAllMapModesButton = new JButton(listAllMapModesAction);
    {
        listAllDialogsAction.setButton(listAllToggleDialogsButton);
        listAllMapModesAction.setButton(listAllMapModesButton);
    }

    // Toggle dialogs
    public ConflictDialog conflictDialog;
    public FilterDialog filterDialog;
    public RelationListDialog relationListDialog;
    public ValidatorDialog validatorDialog;
    public SelectionListDialog selectionListDialog;
    public PropertiesDialog propertiesDialog;

    // Map modes
    public final SelectAction mapModeSelect;
    private final Map<Layer, MapMode> lastMapMode = new HashMap<Layer, MapMode>();
    private final MapMode mapModeDraw;
    private final MapMode mapModeZoom;

    /**
     * The status line below the map
     */
    public MapStatus statusLine;

    /**
     * The split pane with the mapview (leftPanel) and toggle dialogs (dialogsPanel).
     */
    private final JSplitPane splitPane;
    private final JPanel leftPanel;
    private final DialogsPanel dialogsPanel;

    /**
     * Default width of the toggle dialog area.
     */
    public static final int DEF_TOGGLE_DLG_WIDTH = 330;

    /**
     * Constructs a new {@code MapFrame}.
     * @param contentPane The content pane used to register shortcuts in its
     * {@link javax.swing.InputMap} and {@link javax.swing.ActionMap}
     * @param viewportData the initial viewport of the map. Can be null, then
     * the viewport is derived from the layer data.
     */
    public MapFrame(JPanel contentPane, ViewportData viewportData) {
        setSize(400,400);
        setLayout(new BorderLayout());

        mapView = new MapView(contentPane, viewportData);
        new FileDrop(mapView);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);

        leftPanel = new JPanel();
        leftPanel.setLayout(new GridBagLayout());
        leftPanel.add(mapView, GBC.std().fill());
        splitPane.setLeftComponent(leftPanel);

        dialogsPanel = new DialogsPanel(splitPane);
        splitPane.setRightComponent(dialogsPanel);

        /**
         * All additional space goes to the mapView
         */
        splitPane.setResizeWeight(1.0);

        /**
         * Some beautifications.
         */
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void setBorder(Border b) {
                    }
                };
            }
        });

        // JSplitPane supports F6 and F8 shortcuts by default, but we need them for Audio actions
        splitPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), new Object());
        splitPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), new Object());

        add(splitPane, BorderLayout.CENTER);

        dialogsPanel.setLayout(new BoxLayout(dialogsPanel, BoxLayout.Y_AXIS));
        dialogsPanel.setPreferredSize(new Dimension(Main.pref.getInteger("toggleDialogs.width",DEF_TOGGLE_DLG_WIDTH), 0));
        dialogsPanel.setMinimumSize(new Dimension(24, 0));
        mapView.setMinimumSize(new Dimension(10,0));

        // toolBarActions, map mode buttons
        addMapMode(new IconToggleButton(mapModeSelect = new SelectAction(this)));
        addMapMode(new IconToggleButton(new LassoModeAction(), true));
        addMapMode(new IconToggleButton(mapModeDraw = new DrawAction(this)));
        addMapMode(new IconToggleButton(mapModeZoom = new ZoomAction(this)));
        addMapMode(new IconToggleButton(new DeleteAction(this), true));
        addMapMode(new IconToggleButton(new ParallelWayAction(this), true));
        addMapMode(new IconToggleButton(new ExtrudeAction(this), true));
        addMapMode(new IconToggleButton(new ImproveWayAccuracyAction(Main.map), false));
        toolBarActionsGroup.setSelected(allMapModeButtons.get(0).getModel(), true);
        toolBarActions.setFloatable(false);

        // toolBarToggles, toggle dialog buttons
        LayerListDialog.createInstance(this);
        addToggleDialog(LayerListDialog.getInstance());
        addToggleDialog(propertiesDialog = new PropertiesDialog());
        addToggleDialog(selectionListDialog = new SelectionListDialog());
        addToggleDialog(relationListDialog = new RelationListDialog());
        addToggleDialog(new CommandStackDialog());
        addToggleDialog(new UserListDialog());
        addToggleDialog(new HistoryDialog(), true);
        addToggleDialog(conflictDialog = new ConflictDialog());
        addToggleDialog(validatorDialog = new ValidatorDialog());
        addToggleDialog(filterDialog = new FilterDialog());
        addToggleDialog(new ChangesetDialog(), true);
        addToggleDialog(new MapPaintDialog());
        toolBarToggle.setFloatable(false);

        // status line below the map
        statusLine = new MapStatus(this);
        MapView.addLayerChangeListener(this);

        boolean unregisterTab = Shortcut.findShortcut(KeyEvent.VK_TAB, 0)!=null;
        if (unregisterTab) {
            for (JComponent c: allDialogButtons) c.setFocusTraversalKeysEnabled(false);
            for (JComponent c: allMapModeButtons) c.setFocusTraversalKeysEnabled(false);
        }
    }

    public boolean selectSelectTool(boolean onlyIfModeless) {
        if(onlyIfModeless && !Main.pref.getBoolean("modeless", false))
            return false;

        return selectMapMode(mapModeSelect);
    }

    public boolean selectDrawTool(boolean onlyIfModeless) {
        if(onlyIfModeless && !Main.pref.getBoolean("modeless", false))
            return false;

        return selectMapMode(mapModeDraw);
    }

    public boolean selectZoomTool(boolean onlyIfModeless) {
        if(onlyIfModeless && !Main.pref.getBoolean("modeless", false))
            return false;

        return selectMapMode(mapModeZoom);
    }

    /**
     * Called as some kind of destructor when the last layer has been removed.
     * Delegates the call to all Destroyables within this component (e.g. MapModes)
     */
    @Override
    public void destroy() {
        MapView.removeLayerChangeListener(this);
        dialogsPanel.destroy();
        Main.pref.removePreferenceChangeListener(sidetoolbarPreferencesChangedListener);
        for (int i = 0; i < toolBarActions.getComponentCount(); ++i) {
            if (toolBarActions.getComponent(i) instanceof Destroyable) {
                ((Destroyable)toolBarActions.getComponent(i)).destroy();
            }
        }
        for (int i = 0; i < toolBarToggle.getComponentCount(); ++i) {
            if (toolBarToggle.getComponent(i) instanceof Destroyable) {
                ((Destroyable)toolBarToggle.getComponent(i)).destroy();
            }
        }

        statusLine.destroy();
        mapView.destroy();
    }

    public Action getDefaultButtonAction() {
        return ((AbstractButton)toolBarActions.getComponent(0)).getAction();
    }

    /**
     * Open all ToggleDialogs that have their preferences property set. Close all others.
     */
    public void initializeDialogsPane() {
        dialogsPanel.initialize(allDialogs);
    }

    public IconToggleButton addToggleDialog(final ToggleDialog dlg) {
        return addToggleDialog(dlg, false);
    }

    /**
     * Call this to add new toggle dialogs to the left button-list
     * @param dlg The toggle dialog. It must not be in the list already.
     */
    public IconToggleButton addToggleDialog(final ToggleDialog dlg, boolean isExpert) {
        final IconToggleButton button = new IconToggleButton(dlg.getToggleAction(), isExpert);
        button.setShowHideButtonListener(dlg);
        button.setInheritsPopupMenu(true);
        dlg.setButton(button);
        toolBarToggle.add(button);
        allDialogs.add(dlg);
        allDialogButtons.add(button);
        button.applyButtonHiddenPreferences();
        if (dialogsPanel.initialized) {
            dialogsPanel.add(dlg);
        }
        return button;
    }



    public void addMapMode(IconToggleButton b) {
        if (b.getAction() instanceof MapMode) {
            mapModes.add((MapMode) b.getAction());
        } else
            throw new IllegalArgumentException("MapMode action must be subclass of MapMode");
        allMapModeButtons.add(b);
        toolBarActionsGroup.add(b);
        toolBarActions.add(b);
        b.applyButtonHiddenPreferences();
        b.setInheritsPopupMenu(true);
    }

    /**
     * Fires an property changed event "visible".
     * @param aFlag {@code true} if display should be visible
     */
    @Override public void setVisible(boolean aFlag) {
        boolean old = isVisible();
        super.setVisible(aFlag);
        if (old != aFlag) {
            firePropertyChange("visible", old, aFlag);
        }
    }

    /**
     * Change the operating map mode for the view. Will call unregister on the
     * old MapMode and register on the new one. Now this function also verifies
     * if new map mode is correct mode for current layer and does not change mode
     * in such cases.
     * @param newMapMode The new mode to set.
     * @return {@code true} if mode is really selected
     */
    public boolean selectMapMode(MapMode newMapMode) {
        return selectMapMode(newMapMode, mapView.getActiveLayer());
    }

    /**
     * Another version of the selectMapMode for changing layer action.
     * Pass newly selected layer to this method.
     * @param newMapMode The new mode to set.
     * @param newLayer newly selected layer
     * @return {@code true} if mode is really selected
     */
    public boolean selectMapMode(MapMode newMapMode, Layer newLayer) {
        if (newMapMode == null || !newMapMode.layerIsSupported(newLayer))
            return false;

        MapMode oldMapMode = this.mapMode;
        if (newMapMode == oldMapMode)
            return true;
        if (oldMapMode != null) {
            oldMapMode.exitMode();
        }
        this.mapMode = newMapMode;
        newMapMode.enterMode();
        lastMapMode.put(newLayer, newMapMode);
        fireMapModeChanged(oldMapMode, newMapMode);
        return true;
    }

    /**
     * Fill the given panel by adding all necessary components to the different
     * locations.
     *
     * @param panel The container to fill. Must have an BorderLayout.
     */
    public void fillPanel(Container panel) {
        panel.add(this, BorderLayout.CENTER);

        /**
         * sideToolBar: add map modes icons
         */
        if(Main.pref.getBoolean("sidetoolbar.mapmodes.visible", true)) {
        toolBarActions.setAlignmentX(0.5f);
            toolBarActions.setInheritsPopupMenu(true);
            sideToolBar.add(toolBarActions);
            listAllMapModesButton.setAlignmentX(0.5f);
            listAllMapModesButton.setBorder(null);
            listAllMapModesButton.setFont(listAllMapModesButton.getFont().deriveFont(Font.PLAIN));
            listAllMapModesButton.setInheritsPopupMenu(true);
            sideToolBar.add(listAllMapModesButton);
        }

        /**
         * sideToolBar: add toggle dialogs icons
         */
        if(Main.pref.getBoolean("sidetoolbar.toggledialogs.visible", true)) {
            ((JToolBar)sideToolBar).addSeparator(new Dimension(0,18));
            toolBarToggle.setAlignmentX(0.5f);
            toolBarToggle.setInheritsPopupMenu(true);
            sideToolBar.add(toolBarToggle);
            listAllToggleDialogsButton.setAlignmentX(0.5f);
            listAllToggleDialogsButton.setBorder(null);
            listAllToggleDialogsButton.setFont(listAllToggleDialogsButton.getFont().deriveFont(Font.PLAIN));
            listAllToggleDialogsButton.setInheritsPopupMenu(true);
            sideToolBar.add(listAllToggleDialogsButton);
        }

        /**
         * sideToolBar: add dynamic popup menu
         */
        sideToolBar.setComponentPopupMenu(new JPopupMenu() {
            static final int staticMenuEntryCount = 2;
            JCheckBoxMenuItem doNotHide = new JCheckBoxMenuItem(new AbstractAction(tr("Do not hide toolbar")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean sel = ((JCheckBoxMenuItem) e.getSource()).getState();
                    Main.pref.put("sidetoolbar.always-visible", sel);
                }
            });
            {
                addPopupMenuListener(new PopupMenuListener() {
                    @Override
                    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                        final Object src = ((JPopupMenu)e.getSource()).getInvoker();
                        if (src instanceof IconToggleButton) {
                            insert(new Separator(), 0);
                            insert(new AbstractAction() {
                                {
                                    putValue(NAME, tr("Hide this button"));
                                    putValue(SHORT_DESCRIPTION, tr("Click the arrow at the bottom to show it again."));
        }
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    ((IconToggleButton)src).setButtonHidden(true);
                                    validateToolBarsVisibility();
                                }
                            }, 0);
                        }
                        doNotHide.setSelected(Main.pref.getBoolean("sidetoolbar.always-visible", true));
                    }
                    @Override
                    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                        while (getComponentCount() > staticMenuEntryCount) {
                            remove(0);
                        }
                    }
                    @Override
                    public void popupMenuCanceled(PopupMenuEvent e) {}
                });

                add(new AbstractAction(tr("Hide edit toolbar")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Main.pref.put("sidetoolbar.visible", false);
                    }
                });
                add(doNotHide);
            }
        });
        ((JToolBar)sideToolBar).setFloatable(false);

        /**
         * sideToolBar: decide scroll- and visibility
         */
        if(Main.pref.getBoolean("sidetoolbar.scrollable", true)) {
            final ScrollViewport svp = new ScrollViewport(sideToolBar, ScrollViewport.VERTICAL_DIRECTION);
            svp.addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    svp.scroll(0, e.getUnitsToScroll() * 5);
                }
            });
            sideToolBar = svp;
        }
        sideToolBar.setVisible(Main.pref.getBoolean("sidetoolbar.visible", true));
        sidetoolbarPreferencesChangedListener = new Preferences.PreferenceChangedListener() {
            @Override
            public void preferenceChanged(PreferenceChangeEvent e) {
                if ("sidetoolbar.visible".equals(e.getKey())) {
                    sideToolBar.setVisible(Main.pref.getBoolean("sidetoolbar.visible"));
                }
            }
        };
        Main.pref.addPreferenceChangeListener(sidetoolbarPreferencesChangedListener);

        /**
         * sideToolBar: add it to the panel
         */
        panel.add(sideToolBar, BorderLayout.WEST);

        /**
         * statusLine: add to panel
         */
        if (statusLine != null && Main.pref.getBoolean("statusline.visible", true)) {
            panel.add(statusLine, BorderLayout.SOUTH);
        }
    }

    class ListAllButtonsAction extends AbstractAction {

        private JButton button;
        private Collection<? extends HideableButton> buttons;


        public ListAllButtonsAction(Collection<? extends HideableButton> buttons) {
            this.buttons = buttons;
            putValue(NAME, ">>");
        }

        public void setButton(JButton button) {
            this.button =  button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JPopupMenu menu = new JPopupMenu();
            for (HideableButton b : buttons) {
                final HideableButton t = b;
                menu.add(new JCheckBoxMenuItem(new AbstractAction() {
                    {
                        putValue(NAME, t.getActionName());
                        putValue(SMALL_ICON, t.getIcon());
                        putValue(SELECTED_KEY, t.isButtonVisible());
                        putValue(SHORT_DESCRIPTION, tr("Hide or show this toggle button"));
                    }
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if ((Boolean) getValue(SELECTED_KEY)) {
                            t.showButton();
                        } else {
                            t.hideButton();
                        }
                        validateToolBarsVisibility();
                    }
                }));
            }
            Rectangle bounds = button.getBounds();
            menu.show(button, bounds.x + bounds.width, 0);
        }
    }

    public void validateToolBarsVisibility() {
        for (IconToggleButton b : allDialogButtons) {
            b.applyButtonHiddenPreferences();
        }
        toolBarToggle.repaint();
        for (IconToggleButton b : allMapModeButtons) {
            b.applyButtonHiddenPreferences();
        }
        toolBarActions.repaint();
    }

    /**
     * Replies the instance of a toggle dialog of type <code>type</code> managed by this
     * map frame
     *
     * @param <T>
     * @param type the class of the toggle dialog, i.e. UserListDialog.class
     * @return the instance of a toggle dialog of type <code>type</code> managed by this
     * map frame; null, if no such dialog exists
     *
     */
    public <T> T getToggleDialog(Class<T> type) {
        return dialogsPanel.getToggleDialog(type);
    }

    public void setDialogsPanelVisible(boolean visible) {
        rememberToggleDialogWidth();
        dialogsPanel.setVisible(visible);
        splitPane.setDividerLocation(visible?splitPane.getWidth()-Main.pref.getInteger("toggleDialogs.width",DEF_TOGGLE_DLG_WIDTH):0);
        splitPane.setDividerSize(visible?5:0);
    }

    /**
     * Remember the current width of the (possibly resized) toggle dialog area
     */
    public void rememberToggleDialogWidth() {
        if (dialogsPanel.isVisible()) {
            Main.pref.putInteger("toggleDialogs.width", splitPane.getWidth()-splitPane.getDividerLocation());
    }
    }

    /**
     * Remove panel from top of MapView by class     
     */
    public void removeTopPanel(Class<?> type) {
        int n = leftPanel.getComponentCount();
        for (int i=0; i<n; i++) {
            Component c = leftPanel.getComponent(i);
            if (type.isInstance(c)) {
                leftPanel.remove(i);
                leftPanel.doLayout();
                return;
            }
        }
    }

    /*
     * Find panel on top of MapView by class
     */
    public <T> T getTopPanel(Class<T> type) {
        int n = leftPanel.getComponentCount();
        for (int i=0; i<n; i++) {
            Component c = leftPanel.getComponent(i);
            if (type.isInstance(c))
                return type.cast(c);
        }
        return null;
    }

    /**
     * Add component @param c on top of MapView
     */
    public void addTopPanel(Component c) {
        leftPanel.add(c, GBC.eol().fill(GBC.HORIZONTAL), leftPanel.getComponentCount()-1);
        leftPanel.doLayout();
        c.doLayout();
    }

    /**
     * Interface to notify listeners of the change of the mapMode.
     */
    public interface MapModeChangeListener {
        void mapModeChange(MapMode oldMapMode, MapMode newMapMode);
    }

    /**
     * the mapMode listeners
     */
    private static final CopyOnWriteArrayList<MapModeChangeListener> mapModeChangeListeners = new CopyOnWriteArrayList<MapModeChangeListener>();

    private PreferenceChangedListener sidetoolbarPreferencesChangedListener;
    /**
     * Adds a mapMode change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     */
    public static void addMapModeChangeListener(MapModeChangeListener listener) {
        if (listener != null) {
            mapModeChangeListeners.addIfAbsent(listener);
        }
    }
    /**
     * Removes a mapMode change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     */
    public static void removeMapModeChangeListener(MapModeChangeListener listener) {
        mapModeChangeListeners.remove(listener);
    }

    protected static void fireMapModeChanged(MapMode oldMapMode, MapMode newMapMode) {
        for (MapModeChangeListener l : mapModeChangeListeners) {
            l.mapModeChange(oldMapMode, newMapMode);
        }
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        boolean modeChanged = false;
        if (mapMode == null || !mapMode.layerIsSupported(newLayer)) {
            MapMode newMapMode = getLastMapMode(newLayer);
            modeChanged = newMapMode != mapMode;
            if (newMapMode != null) {
                selectMapMode(newMapMode, newLayer); // it would be nice to select first supported mode when layer is first selected, but it don't work well with for example editgpx layer
            } else if (mapMode != null) {
                mapMode.exitMode(); // if new mode is null - simply exit from previous mode
            }
        }
        // if this is really a change (and not the first active layer)
        if (oldLayer != null) {
            if (!modeChanged && mapMode != null) {
                // Let mapmodes know about new active layer
                mapMode.exitMode();
                mapMode.enterMode();
            }
            // invalidate repaint cache
            Main.map.mapView.preferenceChanged(null);
        }

        // After all listeners notice new layer, some buttons will be disabled/enabled
        // and possibly need to be hidden/shown.
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                validateToolBarsVisibility();
            }
        });
    }


    private MapMode getLastMapMode(Layer newLayer) {
        MapMode mode = lastMapMode.get(newLayer);
        if (mode == null) {
            // if no action is selected - try to select default action
            Action defaultMode = getDefaultButtonAction();
            if (defaultMode instanceof MapMode && ((MapMode)defaultMode).layerIsSupported(newLayer)) {
                mode = (MapMode) defaultMode;
            }
        }
        return mode;
    }

    @Override
    public void layerAdded(Layer newLayer) { }

    @Override
    public void layerRemoved(Layer oldLayer) {
        lastMapMode.remove(oldLayer);
    }
}
