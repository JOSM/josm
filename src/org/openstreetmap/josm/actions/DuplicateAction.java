// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ShortCut;

public final class DuplicateAction extends JosmAction implements SelectionChangedListener {

    public DuplicateAction() {
    	super(tr("Duplicate"), "duplicate",
			tr("Duplicate selection by copy and immediate paste."),
			ShortCut.registerShortCut("system:duplicate", tr("Edit: Duplicate selection"), KeyEvent.VK_D, ShortCut.GROUP_MENU), true);
    	setEnabled(false);
			DataSet.selListeners.add(this);
    }

	public void actionPerformed(ActionEvent e) {
		Main.main.menu.copy.actionPerformed(e);
		Main.main.menu.paste.actionPerformed(e);
    }

	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		setEnabled(! newSelection.isEmpty());
	}
}
