// License: GPL. For details, see LICENSE file.
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import org.openstreetmap.josm.Main;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveDeepCopy;
import org.openstreetmap.josm.tools.Shortcut;

public final class DuplicateAction extends JosmAction{

    public DuplicateAction() {
        super(tr("Duplicate"), "duplicate",
                tr("Duplicate selection by copy and immediate paste."),
                Shortcut.registerShortcut("system:duplicate", tr("Edit: {0}", tr("Duplicate")), KeyEvent.VK_D, Shortcut.CTRL), true);
        putValue("help", ht("/Action/Duplicate"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Main.main.menu.paste.pasteData(new PrimitiveDeepCopy(getCurrentDataSet().getSelected()), getEditLayer(), e);
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
