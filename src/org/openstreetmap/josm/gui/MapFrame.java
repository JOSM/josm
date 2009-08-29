// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.DeleteAction;
import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.actions.mapmode.ExtrudeAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.SelectAction;
import org.openstreetmap.josm.actions.mapmode.ZoomAction;
import org.openstreetmap.josm.gui.ScrollViewport;
import org.openstreetmap.josm.gui.dialogs.CommandStackDialog;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.gui.dialogs.HistoryDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.PropertiesDialog;
import org.openstreetmap.josm.gui.dialogs.RelationListDialog;
import org.openstreetmap.josm.gui.dialogs.SelectionListDialog;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.dialogs.UserListDialog;
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
    /**
     * The dialog that shows all relations and lets the user edit them.
     */
    public RelationListDialog relationListDialog;
    /**
     * The panel list of all toggle dialog icons. To add new toggle dialog actions, use addToggleDialog
     * instead of adding directly to this list.
     */
    private JPanel toggleDialogs = new JPanel();
    private ArrayList<ToggleDialog> allDialogs = new ArrayList<ToggleDialog>();

    public final ButtonGroup toolGroup = new ButtonGroup();

    public MapFrame() {
        setSize(400,400);
        setLayout(new BorderLayout());

        add(mapView = new MapView(), BorderLayout.CENTER);

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

        add(toggleDialogs, BorderLayout.EAST);
        toggleDialogs.setLayout(new BoxLayout(toggleDialogs, BoxLayout.Y_AXIS));

        toolBarToggle.setFloatable(false);
        LayerListDialog.createInstance(this);
        addToggleDialog(LayerListDialog.getInstance());
        addToggleDialog(new PropertiesDialog(this));
        addToggleDialog(new HistoryDialog());
        addToggleDialog(new SelectionListDialog());
        addToggleDialog(new UserListDialog());
        addToggleDialog(conflictDialog = new ConflictDialog());
        addToggleDialog(new CommandStackDialog(this));
        addToggleDialog(relationListDialog = new RelationListDialog());

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
        for (ToggleDialog t : allDialogs) {
            t.closeDetachedDialog();
        }
        for (int i = 0; i < toolBarActions.getComponentCount(); ++i)
            if (toolBarActions.getComponent(i) instanceof Destroyable) {
                ((Destroyable)toolBarActions).destroy();
            }
        for (int i = 0; i < toolBarToggle.getComponentCount(); ++i)
            if (toolBarToggle.getComponent(i) instanceof Destroyable) {
                ((Destroyable)toolBarToggle).destroy();
            }

        // remove menu entries
        Main.main.menu.viewMenu.setVisible(false);

        // MapFrame gets destroyed when the last layer is removed, but the status line background
        // thread that collects the information doesn't get destroyed automatically.
        if(statusLine.thread == null) return;
        try {
            statusLine.thread.interrupt();
        } catch (Exception e) {}
    }

    public Action getDefaultButtonAction() {
        return ((AbstractButton)toolBarActions.getComponent(0)).getAction();
    }

    /**
     * Open all ToggleDialogs that have their preferences property set. Close all others.
     */
    public void setVisibleDialogs() {
        for (Component c : toggleDialogs.getComponents()) {
            if (c instanceof ToggleDialog) {
                ToggleDialog td = (ToggleDialog)c;
                if (Main.pref.getBoolean(td.getPreferencePrefix()+".visible")) {
                    td.showDialog();
                }
            }
        }
    }

    /**
     * Call this to add new toggle dialogs to the left button-list
     * @param dlg The toggle dialog. It must not be in the list already.
     */
    public IconToggleButton addToggleDialog(ToggleDialog dlg) {
        IconToggleButton button = new IconToggleButton(dlg.getToggleAction());
        dlg.setParent(toggleDialogs);
        toolBarToggle.add(button);
        toggleDialogs.add(dlg);
        allDialogs.add(dlg);
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
    public void selectMapMode(MapMode mapMode) {
        if (mapMode == this.mapMode)
            return;
        if (this.mapMode != null) {
            this.mapMode.exitMode();
        }
        this.mapMode = mapMode;
        mapMode.enterMode();
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
        jb.addSeparator();
        jb.add(toolBarToggle);
        if(Main.pref.getBoolean("sidetoolbar.visible", true))
        {
            if(Main.pref.getBoolean("sidetoolbar.scrollable", true)) {
                panel.add(new ScrollViewport(jb, ScrollViewport.VERTICAL_DIRECTION),
                        BorderLayout.WEST);
            } else {
                panel.add(jb, BorderLayout.WEST);
            }
        }
        if (statusLine != null && Main.pref.getBoolean("statusline.visible", true)) {
            panel.add(statusLine, BorderLayout.SOUTH);
        }
    }
}
