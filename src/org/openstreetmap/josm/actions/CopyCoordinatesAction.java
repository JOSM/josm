// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * User action to copy the coordinates of one or several node(s) to the clipboard.
 */
public class CopyCoordinatesAction extends JosmAction {

    /**
     * Constructs a new {@code CopyCoordinatesAction}.
     */
    public CopyCoordinatesAction() {
        super(tr("Copy Coordinates"), null,
                tr("Copy coordinates of selected nodes to clipboard."),
                Shortcut.registerShortcut("copy:coordinates", tr("Edit: {0}", tr("Copy Coordinates")),
                KeyEvent.VK_C, Shortcut.CTRL_SHIFT),
                false);
        setToolbarId("copy/coordinates");
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        StringBuilder s = new StringBuilder();
        for (Node n : getSelectedNodes()) {
            s.append(n.lat());
            s.append(", ");
            s.append(n.lon());
            s.append('\n');
        }
        ClipboardUtils.copyString(s.toString().trim());
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(!getSelectedNodes().isEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledState();
    }

    private Collection<Node> getSelectedNodes() {
        DataSet ds = getLayerManager().getActiveDataSet();
        if (ds == null) {
            return Collections.emptyList();
        } else {
            return ds.getSelectedNodes();
        }
    }
}
