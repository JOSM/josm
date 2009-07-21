// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.tools.Shortcut;

public final class DuplicateAction extends JosmAction{

    public DuplicateAction() {
        super(tr("Duplicate"), "duplicate",
                tr("Duplicate selection by copy and immediate paste."),
                Shortcut.registerShortcut("system:duplicate", tr("Edit: {0}", tr("Duplicate")), KeyEvent.VK_D, Shortcut.GROUP_MENU), true);
    }

    public void actionPerformed(ActionEvent e) {
        new PasteAction().pasteData(new CopyAction().copyData(), getEditLayer(), e);
    }


    @Override
    protected void updateEnabledState() {
        setEnabled(getCurrentDataSet() != null && ! getCurrentDataSet().getSelected().isEmpty());
    }
}
