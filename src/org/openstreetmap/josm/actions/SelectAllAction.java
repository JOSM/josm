// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

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
                Shortcut.registerShortcut("system:selectall", tr("Selection: {0}", tr("Select All")), KeyEvent.VK_A, Shortcut.CTRL), true);
        setHelpId(ht("/Action/SelectAll"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        OsmData<?, ?, ?, ?> ds = getLayerManager().getActiveData();
        // Do not use method reference before the Java 11 migration
        // Otherwise we face a compiler bug, see below:
        // https://bugs.openjdk.java.net/browse/JDK-8141508
        // https://bugs.openjdk.java.net/browse/JDK-8142476
        // https://bugs.openjdk.java.net/browse/JDK-8191655
        ds.setSelected(ds.getPrimitives(t -> t.isSelectable()));
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    /**
     * Refreshes the enabled state
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getActiveData() != null);
    }
}
