// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.KeyEvent;

import java.util.Collection;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.dialogs.InspectPrimitiveDialog;
import org.openstreetmap.josm.tools.Shortcut;

public class InfoAction extends JosmAction {

    /**
     * Constructs a new {@code InfoAction}.
     */
    public InfoAction() {
        super(tr("Advanced info"), "about",
            tr("Display advanced object information about OSM nodes, ways, or relations."),
            Shortcut.registerShortcut("core:info",
                tr("Advanced info"), KeyEvent.VK_I, Shortcut.CTRL),
            true, "action/info", true);
        putValue("help", ht("/Action/InfoAboutElements"));
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        DataSet set = getCurrentDataSet();
        if (set != null) {
            new InspectPrimitiveDialog(set.getAllSelected(), Main.main.getEditLayer()).showDialog();
        }
    }

    @Override
    public void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getAllSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(!selection.isEmpty());
    }
}
