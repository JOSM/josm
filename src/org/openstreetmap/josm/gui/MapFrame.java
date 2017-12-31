// License: GPL. For details, see LICENSE file.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import org.openstreetmap.josm.actions.LassoModeAction;
import org.openstreetmap.josm.actions.mapmode.DeleteAction;
import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.actions.mapmode.ExtrudeAction;
import org.openstreetmap.josm.actions.mapmode.ImproveWayAccuracyAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.ParallelWayAction;
import org.openstreetmap.josm.actions.mapmode.SelectAction;
import org.openstreetmap.josm.actions.mapmode.ZoomAction;
import org.openstreetmap.josm.data.ViewportData;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.dialogs.ChangesetDialog;
import org.openstreetmap.josm.gui.dialogs.CommandStackDialog;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel;
import org.openstreetmap.josm.gui.dialogs.FilterDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.MapPaintDialog;
import org.openstreetmap.josm.gui.dialogs.MinimapDialog;
import org.openstreetmap.josm.gui.dialogs.NotesDialog;
import org.openstreetmap.josm.gui.dialogs.RelationListDialog;
import org.openstreetmap.josm.gui.dialogs.SelectionListDialog;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.dialogs.UserListDialog;
import org.openstreetmap.josm.gui.dialogs.ValidatorDialog;
import org.openstreetmap.josm.gui.dialogs.properties.PropertiesDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.util.AdvancedKeyPressDetector;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * One Map frame with one dataset behind. This is the container gui class whose
 * display can be set to the different views.
 *
 * @author imi
 */
public class MapFrame extends JPanel implements Destroyable, ActiveLayerChangeListener, LayerChangeListener {
    /**
     * Default width of the toggle dialog area.
     */
    public static final int DEF_TOGGLE_DLG_WIDTH = 330;

    private static final IntegerProperty TOGGLE_DIALOGS_WIDTH = new IntegerProperty("toggleDialogs.width", DEF_TOGGLE_DLG_WIDTH);
    /**
     * Do not require to switch modes (potlatch style workflow) for drawing/selecting map modes.
     * @since 12347
     */
    public static final BooleanProperty MODELESS = new BooleanProperty("modeless", false);
    /**
     * The current mode, this frame operates.
     */
    public MapMode mapMode;

    /**
     * The view control displayed.
     */
    public final MapView mapView;

    /**
     * This object allows to detect key press and release events
     */
    public final transient AdvancedKeyPressDetector keyDetector = new AdvancedKeyPressDetector();

    /**
     * The toolbar with the action icons. To add new toggle dialog buttons,
     * use addToggleDialog, to add a new map mode button use addMapMode.
     */
    private JComponent sideToolBar = new JToolBar(JToolBar.VERTICAL);
    private final ButtonGroup toolBarActionsGroup = new ButtonGroup();
    private final JToolBar toolBarActions = new JToolBar(JToolBar.VERTICAL);
    private final JToolBar toolBarToggle = new JToolBar(JToolBar.VERTICAL);

    private final List<ToggleDialog> allDialogs = new ArrayList<>();
    private final List<IconToggleButton> allDialogButtons = new ArrayList<>();
    /**
     * All map mode buttons. Should only be read form the outside
     */
    public final List<IconToggleButton> allMapModeButtons = new ArrayList<>();

    private final ListAllButtonsAction listAllDialogsAction = new ListAllButtonsAction(allDialogButtons);
    private final ListAllButtonsAction listAllMapModesAction = new ListAllButtonsAction(allMapModeButtons);
    private final JButton listAllToggleDialogsButton = new JButton(listAllDialogsAction);
    private final JButton listAllMapModesButton = new JButton(listAllMapModesAction);

    {
        listAllDialogsAction.setButton(listAllToggleDialogsButton);
        listAllMapModesAction.setButton(listAllMapModesButton);
    }

    // Toggle dialogs

    /** Conflict dialog */
    public final ConflictDialog conflictDialog;
    /** Filter dialog */
    public final FilterDialog filterDialog;
    /** Relation list dialog */
    public final RelationListDialog relationListDialog;
    /** Validator dialog */
    public final ValidatorDialog validatorDialog;
    /** Selection list dialog */
    public final SelectionListDialog selectionListDialog;
    /** Properties dialog */
    public final PropertiesDialog propertiesDialog;
    /** Map paint dialog */
    public final MapPaintDialog mapPaintDialog;
    /** Notes dialog */
    public final NotesDialog noteDialog;

    // Map modes

    /** Select mode */
    public final SelectAction mapModeSelect;
    /** Draw mode */
    public final DrawAction mapModeDraw;
    /** Zoom mode */
    public final ZoomAction mapModeZoom;
    /** Delete mode */
    public final DeleteAction mapModeDelete;
    /** Select Lasso mode */
    public LassoModeAction mapModeSelectLasso;

    private final transient Map<Layer, MapMode> lastMapMode = new HashMap<>();

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
     * Constructs a new {@code MapFrame}.
     * @param viewportData the initial viewport of the map. Can be null, then
     * the viewport is derived from the layer data.
     * @since 11713
     */
    public MapFrame(ViewportData viewportData) {
        setSize(400, 400);
        setLayout(new BorderLayout());

        mapView = new MapView(MainApplication.getLayerManager(), viewportData);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);

        leftPanel = new JPanel(new GridBagLayout());
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
        splitPane.setUI(new NoBorderSplitPaneUI());

        // JSplitPane supports F6 and F8 shortcuts by default, but we need them for Audio actions
        splitPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), new Object());
        splitPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), new Object());

        add(splitPane, BorderLayout.CENTER);

        dialogsPanel.setLayout(new BoxLayout(dialogsPanel, BoxLayout.Y_AXIS));
        dialogsPanel.setPreferredSize(new Dimension(TOGGLE_DIALOGS_WIDTH.get(), 0));
        dialogsPanel.setMinimumSize(new Dimension(24, 0));
        mapView.setMinimumSize(new Dimension(10, 0));

        // toolBarActions, map mode buttons
        mapModeSelect = new SelectAction(this);
        mapModeSelectLasso = new LassoModeAction();
        mapModeDraw = new DrawAction();
        mapModeZoom = new ZoomAction(this);
        mapModeDelete = new DeleteAction();

        addMapMode(new IconToggleButton(mapModeSelect));
        addMapMode(new IconToggleButton(mapModeSelectLasso, true));
        addMapMode(new IconToggleButton(mapModeDraw));
        addMapMode(new IconToggleButton(mapModeZoom, true));
        addMapMode(new IconToggleButton(mapModeDelete, true));
        addMapMode(new IconToggleButton(new ParallelWayAction(this), true));
        addMapMode(new IconToggleButton(new ExtrudeAction(), true));
        addMapMode(new IconToggleButton(new ImproveWayAccuracyAction(), false));
        toolBarActionsGroup.setSelected(allMapModeButtons.get(0).getModel(), true);
        toolBarActions.setFloatable(false);

        // toolBarToggles, toggle dialog buttons
        LayerListDialog.createInstance(mapView.getLayerManager());
        propertiesDialog = new PropertiesDialog();
        selectionListDialog = new SelectionListDialog();
        relationListDialog = new RelationListDialog();
        conflictDialog = new ConflictDialog();
        validatorDialog = new ValidatorDialog();
        filterDialog = new FilterDialog();
        mapPaintDialog = new MapPaintDialog();
        noteDialog = new NotesDialog();

        addToggleDialog(LayerListDialog.getInstance());
        addToggleDialog(propertiesDialog);
        addToggleDialog(selectionListDialog);
        addToggleDialog(relationListDialog);
        addToggleDialog(new MinimapDialog());
        addToggleDialog(new CommandStackDialog());
        addToggleDialog(new UserListDialog());
        addToggleDialog(conflictDialog);
        addToggleDialog(validatorDialog);
        addToggleDialog(filterDialog);
        addToggleDialog(new ChangesetDialog(), true);
        addToggleDialog(mapPaintDialog);
        addToggleDialog(noteDialog);
        toolBarToggle.setFloatable(false);

        // status line below the map
        statusLine = new MapStatus(this);
        MainApplication.getLayerManager().addLayerChangeListener(this);
        MainApplication.getLayerManager().addActiveLayerChangeListener(this);

        boolean unregisterTab = Shortcut.findShortcut(KeyEvent.VK_TAB, 0).isPresent();
        if (unregisterTab) {
            for (JComponent c: allDialogButtons) {
                c.setFocusTraversalKeysEnabled(false);
            }
            for (JComponent c: allMapModeButtons) {
                c.setFocusTraversalKeysEnabled(false);
            }
        }

        if (Config.getPref().getBoolean("debug.advanced-keypress-detector.enable", true)) {
            keyDetector.register();
        }
    }

    /**
     * Enables the select tool
     * @param onlyIfModeless Only enable if modeless mode is active
     * @return <code>true</code> if it is selected
     */
    public boolean selectSelectTool(boolean onlyIfModeless) {
        if (onlyIfModeless && !MODELESS.get())
            return false;

        return selectMapMode(mapModeSelect);
    }

    /**
     * Enables the draw tool
     * @param onlyIfModeless Only enable if modeless mode is active
     * @return <code>true</code> if it is selected
     */
    public boolean selectDrawTool(boolean onlyIfModeless) {
        if (onlyIfModeless && !MODELESS.get())
            return false;

        return selectMapMode(mapModeDraw);
    }

    /**
     * Enables the zoom tool
     * @param onlyIfModeless Only enable if modeless mode is active
     * @return <code>true</code> if it is selected
     */
    public boolean selectZoomTool(boolean onlyIfModeless) {
        if (onlyIfModeless && !MODELESS.get())
            return false;

        return selectMapMode(mapModeZoom);
    }

    /**
     * Called as some kind of destructor when the last layer has been removed.
     * Delegates the call to all Destroyables within this component (e.g. MapModes)
     */
    @Override
    public void destroy() {
        MainApplication.getLayerManager().removeLayerChangeListener(this);
        MainApplication.getLayerManager().removeActiveLayerChangeListener(this);
        dialogsPanel.destroy();
        Config.getPref().removePreferenceChangeListener(sidetoolbarPreferencesChangedListener);
        for (int i = 0; i < toolBarActions.getComponentCount(); ++i) {
            if (toolBarActions.getComponent(i) instanceof Destroyable) {
                ((Destroyable) toolBarActions.getComponent(i)).destroy();
            }
        }
        toolBarActions.removeAll();
        for (int i = 0; i < toolBarToggle.getComponentCount(); ++i) {
            if (toolBarToggle.getComponent(i) instanceof Destroyable) {
                ((Destroyable) toolBarToggle.getComponent(i)).destroy();
            }
        }
        toolBarToggle.removeAll();

        statusLine.destroy();
        mapView.destroy();
        keyDetector.unregister();

        allDialogs.clear();
        allDialogButtons.clear();
        allMapModeButtons.clear();
    }

    /**
     * Gets the action of the default (first) map mode
     * @return That action
     */
    public Action getDefaultButtonAction() {
        return ((AbstractButton) toolBarActions.getComponent(0)).getAction();
    }

    /**
     * Open all ToggleDialogs that have their preferences property set. Close all others.
     */
    public void initializeDialogsPane() {
        dialogsPanel.initialize(allDialogs);
    }

    /**
     * Adds a new toggle dialog to the left button list. It is displayed in expert and normal mode
     * @param dlg The dialog
     * @return The button
     */
    public IconToggleButton addToggleDialog(final ToggleDialog dlg) {
        return addToggleDialog(dlg, false);
    }

    /**
     * Call this to add new toggle dialogs to the left button-list
     * @param dlg The toggle dialog. It must not be in the list already.
     * @param isExpert {@code true} if it's reserved to expert mode
     * @return button allowing to toggle the dialog
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

    /**
     * Call this to remove existing toggle dialog from the left button-list
     * @param dlg The toggle dialog. It must be already in the list.
     * @since 10851
     */
    public void removeToggleDialog(final ToggleDialog dlg) {
        final JToggleButton button = dlg.getButton();
        if (button != null) {
            allDialogButtons.remove(button);
            toolBarToggle.remove(button);
        }
        dialogsPanel.remove(dlg);
        allDialogs.remove(dlg);
    }

    /**
     * Adds a new map mode button
     * @param b The map mode button with a {@link MapMode} action.
     */
    public void addMapMode(IconToggleButton b) {
        if (!(b.getAction() instanceof MapMode))
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
        return selectMapMode(newMapMode, mapView.getLayerManager().getActiveLayer());
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
     * @param panel The container to fill. Must have a BorderLayout.
     */
    public void fillPanel(Container panel) {
        panel.add(this, BorderLayout.CENTER);

        /**
         * sideToolBar: add map modes icons
         */
        if (Config.getPref().getBoolean("sidetoolbar.mapmodes.visible", true)) {
            toolBarActions.setAlignmentX(0.5f);
            toolBarActions.setBorder(null);
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
        if (Config.getPref().getBoolean("sidetoolbar.toggledialogs.visible", true)) {
            ((JToolBar) sideToolBar).addSeparator(new Dimension(0, 18));
            toolBarToggle.setAlignmentX(0.5f);
            toolBarToggle.setBorder(null);
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
        sideToolBar.setComponentPopupMenu(new SideToolbarPopupMenu());
        ((JToolBar) sideToolBar).setFloatable(false);
        sideToolBar.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));

        /**
         * sideToolBar: decide scroll- and visibility
         */
        if (Config.getPref().getBoolean("sidetoolbar.scrollable", true)) {
            final ScrollViewport svp = new ScrollViewport(sideToolBar, ScrollViewport.VERTICAL_DIRECTION);
            sideToolBar = svp;
        }
        sideToolBar.setVisible(Config.getPref().getBoolean("sidetoolbar.visible", true));
        sidetoolbarPreferencesChangedListener = e -> {
            if ("sidetoolbar.visible".equals(e.getKey())) {
                sideToolBar.setVisible(Config.getPref().getBoolean("sidetoolbar.visible"));
            }
        };
        Config.getPref().addPreferenceChangeListener(sidetoolbarPreferencesChangedListener);

        /**
         * sideToolBar: add it to the panel
         */
        panel.add(sideToolBar, BorderLayout.WEST);

        /**
         * statusLine: add to panel
         */
        if (statusLine != null && Config.getPref().getBoolean("statusline.visible", true)) {
            panel.add(statusLine, BorderLayout.SOUTH);
        }
    }

    static final class NoBorderSplitPaneUI extends BasicSplitPaneUI {
        static final class NoBorderBasicSplitPaneDivider extends BasicSplitPaneDivider {
            NoBorderBasicSplitPaneDivider(BasicSplitPaneUI ui) {
                super(ui);
            }

            @Override
            public void setBorder(Border b) {
                // Do nothing
            }
        }

        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new NoBorderBasicSplitPaneDivider(this);
        }
    }

    private final class SideToolbarPopupMenu extends JPopupMenu {
        private static final int staticMenuEntryCount = 2;
        private final JCheckBoxMenuItem doNotHide = new JCheckBoxMenuItem(new AbstractAction(tr("Do not hide toolbar")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean sel = ((JCheckBoxMenuItem) e.getSource()).getState();
                Config.getPref().putBoolean("sidetoolbar.always-visible", sel);
            }
        });
        {
            addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    final Object src = ((JPopupMenu) e.getSource()).getInvoker();
                    if (src instanceof IconToggleButton) {
                        insert(new Separator(), 0);
                        insert(new AbstractAction() {
                            {
                                putValue(NAME, tr("Hide this button"));
                                putValue(SHORT_DESCRIPTION, tr("Click the arrow at the bottom to show it again."));
                            }

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                ((IconToggleButton) src).setButtonHidden(true);
                                validateToolBarsVisibility();
                            }
                        }, 0);
                    }
                    doNotHide.setSelected(Config.getPref().getBoolean("sidetoolbar.always-visible", true));
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    while (getComponentCount() > staticMenuEntryCount) {
                        remove(0);
                    }
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                    // Do nothing
                }
            });

            add(new AbstractAction(tr("Hide edit toolbar")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Config.getPref().putBoolean("sidetoolbar.visible", false);
                }
            });
            add(doNotHide);
        }
    }

    class ListAllButtonsAction extends AbstractAction {

        private JButton button;
        private final transient Collection<? extends HideableButton> buttons;

        ListAllButtonsAction(Collection<? extends HideableButton> buttons) {
            this.buttons = buttons;
        }

        public void setButton(JButton button) {
            this.button = button;
            final ImageIcon icon = ImageProvider.get("audio-fwd");
            putValue(SMALL_ICON, icon);
            button.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight() + 64));
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
            if (button != null) {
                Rectangle bounds = button.getBounds();
                menu.show(button, bounds.x + bounds.width, 0);
            }
        }
    }

    /**
     * Validate the visibility of all tool bars and hide the ones that should be hidden
     */
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
     * Replies the instance of a toggle dialog of type <code>type</code> managed by this map frame
     *
     * @param <T> toggle dialog type
     * @param type the class of the toggle dialog, i.e. UserListDialog.class
     * @return the instance of a toggle dialog of type <code>type</code> managed by this
     * map frame; null, if no such dialog exists
     *
     */
    public <T> T getToggleDialog(Class<T> type) {
        return dialogsPanel.getToggleDialog(type);
    }

    /**
     * Shows or hides the side dialog panel
     * @param visible The new visibility
     */
    public void setDialogsPanelVisible(boolean visible) {
        rememberToggleDialogWidth();
        dialogsPanel.setVisible(visible);
        splitPane.setDividerLocation(visible ? splitPane.getWidth() - TOGGLE_DIALOGS_WIDTH.get() : 0);
        splitPane.setDividerSize(visible ? 5 : 0);
    }

    /**
     * Remember the current width of the (possibly resized) toggle dialog area
     */
    public void rememberToggleDialogWidth() {
        if (dialogsPanel.isVisible()) {
            TOGGLE_DIALOGS_WIDTH.put(splitPane.getWidth() - splitPane.getDividerLocation());
        }
    }

    /**
     * Remove panel from top of MapView by class
     * @param type type of panel
     */
    public void removeTopPanel(Class<?> type) {
        int n = leftPanel.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = leftPanel.getComponent(i);
            if (type.isInstance(c)) {
                leftPanel.remove(i);
                leftPanel.doLayout();
                return;
            }
        }
    }

    /**
     * Find panel on top of MapView by class
     * @param <T> type
     * @param type type of panel
     * @return found panel
     */
    public <T> T getTopPanel(Class<T> type) {
        int n = leftPanel.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component c = leftPanel.getComponent(i);
            if (type.isInstance(c))
                return type.cast(c);
        }
        return null;
    }

    /**
     * Add component {@code c} on top of MapView
     * @param c component
     */
    public void addTopPanel(Component c) {
        leftPanel.add(c, GBC.eol().fill(GBC.HORIZONTAL), leftPanel.getComponentCount()-1);
        leftPanel.doLayout();
        c.doLayout();
    }

    /**
     * Interface to notify listeners of the change of the mapMode.
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    public interface MapModeChangeListener {
        /**
         * Trigerred when map mode changes.
         * @param oldMapMode old map mode
         * @param newMapMode new map mode
         */
        void mapModeChange(MapMode oldMapMode, MapMode newMapMode);
    }

    /**
     * the mapMode listeners
     */
    private static final CopyOnWriteArrayList<MapModeChangeListener> mapModeChangeListeners = new CopyOnWriteArrayList<>();

    private transient PreferenceChangedListener sidetoolbarPreferencesChangedListener;
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
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        boolean modeChanged = false;
        Layer newLayer = e.getSource().getActiveLayer();
        if (mapMode == null || !mapMode.layerIsSupported(newLayer)) {
            MapMode newMapMode = getLastMapMode(newLayer);
            modeChanged = newMapMode != mapMode;
            if (newMapMode != null) {
                // it would be nice to select first supported mode when layer is first selected,
                // but it don't work well with for example editgpx layer
                selectMapMode(newMapMode, newLayer);
            } else if (mapMode != null) {
                mapMode.exitMode(); // if new mode is null - simply exit from previous mode
                mapMode = null;
            }
        }
        // if this is really a change (and not the first active layer)
        if (e.getPreviousActiveLayer() != null && !modeChanged && mapMode != null) {
            // Let mapmodes know about new active layer
            mapMode.exitMode();
            mapMode.enterMode();
        }

        // After all listeners notice new layer, some buttons will be disabled/enabled
        // and possibly need to be hidden/shown.
        validateToolBarsVisibility();
    }

    private MapMode getLastMapMode(Layer newLayer) {
        MapMode mode = lastMapMode.get(newLayer);
        if (mode == null) {
            // if no action is selected - try to select default action
            Action defaultMode = getDefaultButtonAction();
            if (defaultMode instanceof MapMode && ((MapMode) defaultMode).layerIsSupported(newLayer)) {
                mode = (MapMode) defaultMode;
            }
        }
        return mode;
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        // ignored
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        lastMapMode.remove(e.getRemovedLayer());
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // ignored
    }

}
