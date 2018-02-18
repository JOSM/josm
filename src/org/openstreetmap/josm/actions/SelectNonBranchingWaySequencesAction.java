// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This allows to select a sequence of non-branching connected ways.
 *
 * @author Marko Mäkelä
 */
public class SelectNonBranchingWaySequencesAction extends JosmAction {

    /**
     * Creates a new {@link SelectNonBranchingWaySequencesAction}
     */
    public SelectNonBranchingWaySequencesAction() {
        super(tr("Non-branching way sequences"),
                "way-select",
                tr("Select non-branching sequences of ways"),
                Shortcut.registerShortcut("wayselector:wayselect", tr("Non-branching way sequences"), KeyEvent.VK_W, Shortcut.SHIFT),
                true);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        DataSet ds = getLayerManager().getActiveDataSet();
        SelectNonBranchingWaySequences ws = new SelectNonBranchingWaySequences(ds.getSelectedWays());
        ws.extend(ds);
    }

    /**
     * Update the enabled state of the action when something in
     * the JOSM state changes, i.e. when a layer is removed or added.
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getActiveDataSet() != null);
    }
}
