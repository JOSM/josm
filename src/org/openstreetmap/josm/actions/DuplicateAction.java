// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.actions.CopyAction;
import org.openstreetmap.josm.actions.PasteAction;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.Main;

public final class DuplicateAction extends JosmAction implements SelectionChangedListener {

    public DuplicateAction() {
        super(tr("Duplicate"), "duplicate",
            tr("Duplicate selection by copy and immediate paste."),
            Shortcut.registerShortcut("system:duplicate", tr("Edit: {0}", tr("Duplicate")), KeyEvent.VK_D, Shortcut.GROUP_MENU), true);
        setEnabled(false);
        DataSet.selListeners.add(this);
    }

    public void actionPerformed(ActionEvent e) {
        PasteAction.pasteData(CopyAction.copyData(), Main.main.createOrGetEditLayer(), e);
    }

    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        setEnabled(! newSelection.isEmpty());
    }
}
