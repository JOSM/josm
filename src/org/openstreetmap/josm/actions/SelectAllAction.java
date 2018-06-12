// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * User action to select all primitives in the current dataset.
 */
public class SelectAllAction extends JosmAction {

    /**
     * Constructs a new {@code SelectAllAction}.
     */
    public SelectAllAction() {
        super(tr("Select All"), "selectall", tr("Select all undeleted objects in the data layer. This selects incomplete objects too."),
                Shortcut.registerShortcut("system:selectall", tr("Edit: {0}", tr("Select All")), KeyEvent.VK_A, Shortcut.CTRL), true);
        putValue("help", ht("/Action/SelectAll"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        OsmData<?, ?, ?, ?> ds = getLayerManager().getActiveData();
        ds.setSelected(ds.getPrimitives(IPrimitive::isSelectable));
    }

    /**
     * Refreshes the enabled state
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getActiveData() != null);
    }
}
