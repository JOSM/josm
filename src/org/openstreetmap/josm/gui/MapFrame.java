// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
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
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.DeleteAction;
import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.actions.mapmode.ExtrudeAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.SelectAction;
import org.openstreetmap.josm.actions.mapmode.ZoomAction;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.ChangesetDialog;
import org.openstreetmap.josm.gui.dialogs.CommandStackDialog;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel;
import org.openstreetmap.josm.gui.dialogs.FilterDialog;
import org.openstreetmap.josm.gui.dialogs.HistoryDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.RelationListDialog;
import org.openstreetmap.josm.gui.dialogs.SelectionListDialog;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.dialogs.UserListDialog;
import org.openstreetmap.josm.gui.dialogs.properties.PropertiesDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.Destroyable;

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

    private final List<MapMode> mapModes = new ArrayList<MapMode>();
    /**
     * The view control displayed.
     */
    public MapView mapView;
    /**
     * The toolbar with the action icons. To add new toggle dialog actions, use addToggleDialog
     * instead of adding directly to this list. To add a new mode use addMapMode.
     */
    private JToolBar toolBarActions = new JToolBar(JToolBar.VERTICAL);
    private JToolBar toolBarToggle = new JToolBar(JToolBar.VERTICAL);
    /**
     * The status line below the map
     */
    public MapStatus statusLine;

    public ConflictDialog conflictDialog;
    public FilterDialog filterDialog;
    public RelationListDialog relationListDialog;
    public SelectionListDialog selectionListDialog;
    /**
     * The panel list of all toggle dialog icons. To add new toggle dialog actions, use addToggleDialog
     * instead of adding directly to this list.
     */
    private List<ToggleDialog> allDialogs = new ArrayList<ToggleDialog>();
    private final DialogsPanel dialogsPanel;

    public final ButtonGroup toolGroup = new ButtonGroup();

    public final JButton otherButton = new JButton(new OtherButtonsAction());

    /**
     * Default width of the toggle dialog area.
     */
    public static final int DEF_TOGGLE_DLG_WIDTH = 330;

    private final Map<Layer, MapMode> lastMapMode = new HashMap<Layer, MapMode>();

    public MapFrame(JPanel contentPane) {
        setSize(400,400);
        setLayout(new BorderLayout());

        mapView = new MapView(contentPane);

        new FileDrop(mapView);

        // show menu entry
        Main.main.menu.viewMenu.setVisible(true);

        // toolbar
        toolBarActions.setFloatable(false);
        addMapMode(new IconToggleButton(new SelectAction(this)));
        addMapMode(new IconToggleButton(new DrawAction(this)));
        addMapMode(new IconToggleButton(new ExtrudeAction(this)));
        addMapMode(new IconToggleButton(new ZoomAction(this)));
        addMapMode(new IconToggleButton(new DeleteAction(this)));

        toolGroup.setSelected(((AbstractButton)toolBarActions.getComponent(0)).getModel(), true);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        dialogsPanel = new DialogsPanel(splitPane);
        splitPane.setLeftComponent(mapView);
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

        toolBarToggle.setFloatable(false);
        LayerListDialog.createInstance(this);
        addToggleDialog(LayerListDialog.getInstance());
        addToggleDialog(new PropertiesDialog(this));
        addToggleDialog(selectionListDialog = new SelectionListDialog());
        addToggleDialog(relationListDialog = new RelationListDialog());
        addToggleDialog(new CommandStackDialog(this));
        addToggleDialog(new UserListDialog());
        addToggleDialog(new HistoryDialog());
        addToggleDialog(conflictDialog = new ConflictDialog());
        if(Main.pref.getBoolean("displayfilter", true)) {
            addToggleDialog(filterDialog = new FilterDialog());
        }
        addToggleDialog(new ChangesetDialog(this));

        // status line below the map
        statusLine = new MapStatus(this);
        MapView.addLayerChangeListener(this);
    }

    public void selectSelectTool(boolean onlyIfModeless) {
        if(onlyIfModeless && !Main.pref.getBoolean("modeless", false))
            return;

        selectMapMode((MapMode)getDefaultButtonAction());
    }

    public void selectDrawTool(boolean onlyIfModeless) {
        if(onlyIfModeless && !Main.pref.getBoolean("modeless", false))
            return;

        Action drawAction = ((AbstractButton)toolBarActions.getComponent(1)).getAction();
        selectMapMode((MapMode)drawAction);
    }

    /**
     * Called as some kind of destructor when the last layer has been removed.
     * Delegates the call to all Destroyables within this component (e.g. MapModes)
     */
    public void destroy() {
        MapView.removeLayerChangeListener(this);
        dialogsPanel.destroy();
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

        // remove menu entries
        Main.main.menu.viewMenu.setVisible(false);

        // MapFrame gets destroyed when the last layer is removed, but the status line background
        // thread that collects the information doesn't get destroyed automatically.
        if(statusLine.thread != null) {
            try {
                statusLine.thread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    /**
     * Call this to add new toggle dialogs to the left button-list
     * @param dlg The toggle dialog. It must not be in the list already.
     */
    public IconToggleButton addToggleDialog(final ToggleDialog dlg) {
        final IconToggleButton button = new IconToggleButton(dlg.getToggleAction());
        toolBarToggle.add(button);
        button.addMouseListener(new PopupMenuLauncher(new JPopupMenu() {
            {
                add(new AbstractAction() {
                    {
                        putValue(NAME, tr("Hide this button"));
                        putValue(SHORT_DESCRIPTION, tr("Click the arrow at the bottom to show it again."));
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dlg.hideButton();
                    }
                });
            }
        }));
        dlg.setButton(button);
        allDialogs.add(dlg);
        if (dialogsPanel.initialized) {
            dialogsPanel.add(dlg);
        }
        return button;
    }

    public void addMapMode(IconToggleButton b) {
        toolBarActions.add(b);
        toolGroup.add(b);
        if (b.getAction() instanceof MapMode) {
            mapModes.add((MapMode) b.getAction());
        } else
            throw new IllegalArgumentException("MapMode action must be subclass of MapMode");
    }

    /**
     * Fires an property changed event "visible".
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
     * old MapMode and register on the new one.
     * @param mapMode   The new mode to set.
     */
    public void selectMapMode(MapMode newMapMode) {
        MapMode oldMapMode = this.mapMode;
        if (newMapMode == oldMapMode)
            return;
        if (oldMapMode != null) {
            oldMapMode.exitMode();
        }
        this.mapMode = newMapMode;
        newMapMode.enterMode();
        lastMapMode.put(mapView.getActiveLayer(), newMapMode);
        fireMapModeChanged(oldMapMode, newMapMode);
    }

    /**
     * Fill the given panel by adding all necessary components to the different
     * locations.
     *
     * @param panel The container to fill. Must have an BorderLayout.
     */
    public void fillPanel(Container panel) {
        panel.add(this, BorderLayout.CENTER);
        JToolBar jb = new JToolBar(JToolBar.VERTICAL);
        jb.setFloatable(false);
        toolBarActions.setAlignmentX(0.5f);
        jb.add(toolBarActions);

        jb.addSeparator(new Dimension(0,10));
        toolBarToggle.setAlignmentX(0.5f);
        jb.add(toolBarToggle);
        otherButton.setAlignmentX(0.5f);
        otherButton.setBorder(null);
        otherButton.setFont(otherButton.getFont().deriveFont(Font.PLAIN));
        jb.add(otherButton);

        if(Main.pref.getBoolean("sidetoolbar.visible", true))
        {
            if(Main.pref.getBoolean("sidetoolbar.scrollable", true)) {
                final ScrollViewport svp = new ScrollViewport(jb, ScrollViewport.VERTICAL_DIRECTION);
                panel.add(svp, BorderLayout.WEST);
                jb.addMouseWheelListener(new MouseWheelListener() {
                    public void mouseWheelMoved(MouseWheelEvent e) {
                        svp.scroll(0,e.getUnitsToScroll() * 5);
                    }
                });
            } else {
                panel.add(jb, BorderLayout.WEST);
            }
        }
        if (statusLine != null && Main.pref.getBoolean("statusline.visible", true)) {
            panel.add(statusLine, BorderLayout.SOUTH);
        }
    }

    class OtherButtonsAction extends AbstractAction {

        public OtherButtonsAction() {
            putValue(NAME, ">>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JPopupMenu menu = new JPopupMenu();
            for (final ToggleDialog t : allDialogs) {
                menu.add(new JCheckBoxMenuItem(new AbstractAction() {
                    {
                        putValue(NAME, t.getToggleAction().getValue(NAME));
                        putValue(SMALL_ICON, t.getToggleAction().getValue(SMALL_ICON));
                        putValue(SELECTED_KEY, !t.isButtonHidden());
                        putValue(SHORT_DESCRIPTION, tr("Hide or show this toggle button"));
                    }
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if ((Boolean) getValue(SELECTED_KEY)) {
                            t.showButton();
                        } else {
                            t.hideButton();
                        }
                    }
                }));
            }
            Rectangle bounds = otherButton.getBounds();
            menu.show(otherButton, bounds.x+bounds.width, 0);
        }
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

    /**
     * Returns the current width of the (possibly resized) toggle dialog area
     */
    public int getToggleDlgWidth() {
        return dialogsPanel.getWidth();
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
            MapMode newMapMode = lastMapMode.get(newLayer);
            modeChanged = newMapMode != mapMode;
            if (newMapMode != null) {
                selectMapMode(newMapMode);
            } // it would be nice to select first supported mode when layer is first selected, but it don't work well with for example editgpx layer
        }
        if (!modeChanged && mapMode != null) {
            // Let mapmodes know about new active layer
            mapMode.exitMode();
            mapMode.enterMode();
        }
    }

    @Override
    public void layerAdded(Layer newLayer) { }

    @Override
    public void layerRemoved(Layer oldLayer) {
        lastMapMode.remove(oldLayer);
    }
}
