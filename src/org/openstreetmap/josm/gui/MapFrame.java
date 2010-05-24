// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
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
import org.openstreetmap.josm.tools.Destroyable;

/**
 * One Map frame with one dataset behind. This is the container gui class whose
 * display can be set to the different views.
 *
 * @author imi
 */
public class MapFrame extends JPanel implements Destroyable {

    /**
     * The current mode, this frame operates.
     */
    public MapMode mapMode;
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
    /**
     * The dialog that shows all relations and lets the user edit them.
     */
    public RelationListDialog relationListDialog;
    /**
     * The panel list of all toggle dialog icons. To add new toggle dialog actions, use addToggleDialog
     * instead of adding directly to this list.
     */
    private List<ToggleDialog> allDialogs = new ArrayList<ToggleDialog>();
    private final DialogsPanel dialogsPanel;

    public final ButtonGroup toolGroup = new ButtonGroup();

    /**
     * Default width of the toggle dialog area.
     */
    public final int DEF_TOGGLE_DLG_WIDTH = 330;

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
        addToggleDialog(new SelectionListDialog());
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
        dialogsPanel.destroy();
        for (int i = 0; i < toolBarActions.getComponentCount(); ++i)
            if (toolBarActions.getComponent(i) instanceof Destroyable) {
                ((Destroyable)toolBarActions.getComponent(i)).destroy();
            }
        for (int i = 0; i < toolBarToggle.getComponentCount(); ++i)
            if (toolBarToggle.getComponent(i) instanceof Destroyable) {
                ((Destroyable)toolBarToggle.getComponent(i)).destroy();
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
    public IconToggleButton addToggleDialog(ToggleDialog dlg) {
        IconToggleButton button = new IconToggleButton(dlg.getToggleAction());
        toolBarToggle.add(button);
        allDialogs.add(dlg);
        if (dialogsPanel.initialized) {
            dialogsPanel.add(dlg);
        }
        return button;
    }

    public void addMapMode(IconToggleButton b) {
        toolBarActions.add(b);
        toolGroup.add(b);
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
        jb.add(toolBarActions);
        jb.addSeparator(new Dimension(0,10));
        jb.add(toolBarToggle);
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
}
