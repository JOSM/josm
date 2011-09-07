//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.history.HistoryBrowserDialogManager;
import org.openstreetmap.josm.tools.Shortcut;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

public class HistoryInfoAction extends JosmAction {

	public HistoryInfoAction() {
		super(tr("History"), "about",
				tr("Display history information about OSM ways, nodes, or relations."),
				Shortcut.registerShortcut("core:historyinfo",
				tr("History"), KeyEvent.VK_H, Shortcut.GROUP_HOTKEY), false);
		putValue("help", ht("/Action/ObjectHistory"));
		putValue("toolbar", "action/historyinfo");
		Main.toolbar.register(this);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		DataSet set = getCurrentDataSet();
		if (set != null) {
			HistoryBrowserDialogManager.getInstance().showHistory(set.getSelected());
		}
	}

	@Override
	public void updateEnabledState() {
		if (getCurrentDataSet() == null) {
			setEnabled(false);
		} else {
			updateEnabledState(getCurrentDataSet().getSelected());
		}
	}

	@Override
	protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
		setEnabled(!selection.isEmpty());
	}
}
