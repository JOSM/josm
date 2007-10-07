// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.AddSegmentAction;
import org.openstreetmap.josm.actions.mapmode.DeleteAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.SelectionAction;
import org.openstreetmap.josm.actions.mapmode.ZoomAction;
import org.openstreetmap.josm.actions.mapmode.AddNodeAction.AddNodeGroup;
import org.openstreetmap.josm.actions.mapmode.MoveAction.MoveGroup;
import org.openstreetmap.josm.gui.dialogs.CommandStackDialog;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.gui.dialogs.RelationListDialog;
import org.openstreetmap.josm.gui.dialogs.HistoryDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.PropertiesDialog;
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
	 * instead of adding directly to this list.
	 */
	public JToolBar toolBarActions = new JToolBar(JToolBar.VERTICAL);
	/**
	 * The status line below the map
	 */
	private MapStatus statusLine;

	public ConflictDialog conflictDialog;
	/**
	 * The panel list of all toggle dialog icons. To add new toggle dialog actions, use addToggleDialog
	 * instead of adding directly to this list.
	 */
	public JPanel toggleDialogs = new JPanel();

	public final ButtonGroup toolGroup = new ButtonGroup();


	public MapFrame() {
		setSize(400,400);
		setLayout(new BorderLayout());

		add(mapView = new MapView(), BorderLayout.CENTER);

		// show menu entry
		Main.main.menu.viewMenu.setVisible(true);

		// toolbar
		toolBarActions.setFloatable(false);
		toolBarActions.add(new IconToggleButton(new ZoomAction(this)));
		final Action selectionAction = new SelectionAction.Group(this);
		toolBarActions.add(new IconToggleButton(selectionAction));
		toolBarActions.add(new IconToggleButton(new MoveGroup(this)));
		toolBarActions.add(new IconToggleButton(new AddNodeGroup(this)));
		toolBarActions.add(new IconToggleButton(new AddSegmentAction(this)));
		toolBarActions.add(new IconToggleButton(new DeleteAction(this)));

		for (Component c : toolBarActions.getComponents())
			toolGroup.add((AbstractButton)c);
		toolGroup.setSelected(((AbstractButton)toolBarActions.getComponent(0)).getModel(), true);
		
		toolBarActions.addSeparator();
		
		add(toggleDialogs, BorderLayout.EAST);
		toggleDialogs.setLayout(new BoxLayout(toggleDialogs, BoxLayout.Y_AXIS));

		addToggleDialog(new LayerListDialog(this));
		addToggleDialog(new PropertiesDialog(this));
		addToggleDialog(new HistoryDialog());
		addToggleDialog(new SelectionListDialog());
		addToggleDialog(new UserListDialog());
		addToggleDialog(conflictDialog = new ConflictDialog());
		addToggleDialog(new CommandStackDialog(this));
		addToggleDialog(new RelationListDialog());

		// status line below the map
		if (!Main.applet)
	        statusLine = new MapStatus(this);
	}

	/**
	 * Called as some kind of destructor when the last layer has been removed.
	 * Delegates the call to all Destroyables within this component (e.g. MapModes)
	 */
	public void destroy() {
		for (int i = 0; i < toolBarActions.getComponentCount(); ++i)
			if (toolBarActions.getComponent(i) instanceof Destroyable)
				((Destroyable)toolBarActions).destroy();
		
		// remove menu entries
		Main.main.menu.viewMenu.setVisible(false);
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
				boolean sel = Main.pref.getBoolean(((ToggleDialog)c).prefName+".visible");
				((ToggleDialog)c).action.button.setSelected(sel);
				c.setVisible(sel);
			}
		}
	}

	/**
	 * Call this to add new toggle dialogs to the left button-list
	 * @param dlg The toggle dialog. It must not be in the list already.
	 */
	public void addToggleDialog(ToggleDialog dlg) {
		IconToggleButton button = new IconToggleButton(dlg.action);
		dlg.action.button = button;
		dlg.parent = toggleDialogs;
		toolBarActions.add(button);
		toggleDialogs.add(dlg);
	}


	/**
	 * Fires an property changed event "visible".
	 */
	@Override public void setVisible(boolean aFlag) {
		boolean old = isVisible();
		super.setVisible(aFlag);
		if (old != aFlag)
			firePropertyChange("visible", old, aFlag);
	}



	/**
	 * Change the operating map mode for the view. Will call unregister on the
	 * old MapMode and register on the new one.
	 * @param mapMode	The new mode to set.
	 */
	public void selectMapMode(MapMode mapMode) {
		if (mapMode == this.mapMode)
			return;
		if (this.mapMode != null)
			this.mapMode.exitMode();
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
		panel.add(toolBarActions, BorderLayout.WEST);
		if (statusLine != null)
			panel.add(statusLine, BorderLayout.SOUTH);
	}
}
