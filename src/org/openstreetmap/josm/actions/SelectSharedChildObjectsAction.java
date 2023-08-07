// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Select child objects (way nodes and relation members) that are shared by all objects in the current selection.
 */
public class SelectSharedChildObjectsAction extends JosmAction {

    /**
     * Create a new SelectSharedChildObjectsAction
     */
    public SelectSharedChildObjectsAction() {
        super(tr("Shared Child Objects"),
                null,
                tr("Select child objects (way nodes and relation members) that are shared by all objects in the current selection"),
                Shortcut.registerShortcut("selection:sharedchildobjects",
                    tr("Selection: {0}", tr("Shared Child Objects")),
                    KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
                true);
        setHelpId(ht("/Action/SelectSharedChildObjectsAction"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> selection = getLayerManager().getActiveDataSet().getSelected();
        Set<? extends IPrimitive> shared = getSharedChildren(selection);
        getLayerManager().getActiveDataSet().setSelected(shared);
    }

    private Set<? extends IPrimitive> getSharedChildren(Collection<? extends IPrimitive> selection) {
        Set<? extends IPrimitive> sharedChildObjects = new HashSet<>(selection.stream()
            .findAny().map(IPrimitive::getChildren).orElse(Collections.emptyList()));

        for (IPrimitive p : selection) {
            if (sharedChildObjects.isEmpty())
                break;

            sharedChildObjects.retainAll(p.getChildren());
        }

        return sharedChildObjects;
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection(true);
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(!Utils.isEmpty(selection));
    }
}
