// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * User action to invert the selection in the current dataset.
 */
public class InvertSelectionAction extends JosmAction {

    /**
     * Constructs a new {@code SelectAllAction}.
     */
    public InvertSelectionAction() {
        super(tr("Invert Selection"), "invert_selection", tr("Invert Selection"),
                Shortcut.registerShortcut("selection:invertselection",
                tr("Selection: {0}", tr("Invert Selection")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
        setHelpId(ht("/Action/InvertSelection"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        OsmData<?, ?, ?, ?> ds = getLayerManager().getActiveData();
        ds.setSelected(ds.getPrimitives(t -> !t.isSelected()));
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getActiveData() != null);
    }
}
