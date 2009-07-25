// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.Shortcut;

public final class PasteTagsAction extends JosmAction {

    public PasteTagsAction(JosmAction copyAction) {
        super(tr("Paste Tags"), "pastetags",
                tr("Apply tags of contents of paste buffer to all selected items."),
                Shortcut.registerShortcut("system:pastestyle", tr("Edit: {0}", tr("Paste Tags")), KeyEvent.VK_V, Shortcut.GROUP_MENU, Shortcut.SHIFT_DEFAULT), true);
        copyAction.addListener(this);
    }

    private void pasteKeys(Collection<Command> clist, Collection<? extends OsmPrimitive> pasteBufferSubset, Collection<OsmPrimitive> selectionSubset) {
        /* scan the paste buffer, and add tags to each of the selected objects.
         * If a tag already exists, it is overwritten */
        if (selectionSubset == null || selectionSubset.isEmpty())
            return;

        for (Iterator<? extends OsmPrimitive> it = pasteBufferSubset.iterator(); it.hasNext();) {
            OsmPrimitive osm = it.next();
            Map<String, String> m = osm.keys;
            if(m == null) {
                continue;
            }

            for (String key : m.keySet()) {
                clist.add(new ChangePropertyCommand(selectionSubset, key, osm.get(key)));
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        Collection<Command> clist = new LinkedList<Command>();
        String pbSource = "Multiple Sources";
        if(Main.pasteBuffer.dataSources.size() == 1) {
            pbSource = ((DataSource) Main.pasteBuffer.dataSources.toArray()[0]).origin;
        }

        boolean pbNodes = Main.pasteBuffer.nodes.size() > 0;
        boolean pbWays  = Main.pasteBuffer.ways.size() > 0;

        boolean seNodes = getCurrentDataSet().getSelectedNodes().size() > 0;
        boolean seWays  = getCurrentDataSet().getSelectedWays().size() > 0;
        boolean seRels  = getCurrentDataSet().getSelectedRelations().size() > 0;

        if(!seNodes && seWays && !seRels && pbNodes && pbSource.equals("Copied Nodes")) {
            // Copy from nodes to ways
            pasteKeys(clist, Main.pasteBuffer.nodes, getCurrentDataSet().getSelectedWays());
        } else if(seNodes && !seWays && !seRels && pbWays && pbSource.equals("Copied Ways")) {
            // Copy from ways to nodes
            pasteKeys(clist, Main.pasteBuffer.ways, getCurrentDataSet().getSelectedNodes());
        } else {
            // Copy from equal to equal
            pasteKeys(clist, Main.pasteBuffer.nodes, getCurrentDataSet().getSelectedNodes());
            pasteKeys(clist, Main.pasteBuffer.ways, getCurrentDataSet().getSelectedWays());
            pasteKeys(clist, Main.pasteBuffer.relations, getCurrentDataSet().getSelectedRelations());
        }
        Main.main.undoRedo.add(new SequenceCommand(tr("Paste Tags"), clist));
        getCurrentDataSet().setSelected(getCurrentDataSet().getSelected()); // to force selection listeners, in particular the tag panel, to update
        Main.map.mapView.repaint();
    }

    private boolean containsSameKeysWithDifferentValues(Collection<? extends OsmPrimitive> osms) {
        Map<String,String> kvSeen = new HashMap<String,String>();
        for (OsmPrimitive osm:osms) {
            for (String key : osm.keySet()) {
                String value = osm.get(key);
                if (! kvSeen.containsKey(key)) {
                    kvSeen.put(key, value);
                } else if (! kvSeen.get(key).equals(value))
                    return true;
            }
        }
        return false;
    }

    /**
     * Determines whether to enable the widget depending on the contents of the paste
     * buffer and current selection
     * @param pasteBuffer
     */
    private void possiblyEnable(Collection<? extends OsmPrimitive> selection, DataSet pasteBuffer) {
        /* only enable if there is something selected to paste into and
            if we don't have conflicting keys in the pastebuffer */
        setEnabled(selection != null &&
                ! selection.isEmpty() &&
                ! pasteBuffer.allPrimitives().isEmpty() &&
                (getCurrentDataSet().getSelectedNodes().isEmpty() ||
                        ! containsSameKeysWithDifferentValues(pasteBuffer.nodes)) &&
                        (getCurrentDataSet().getSelectedWays().isEmpty() ||
                                ! containsSameKeysWithDifferentValues(pasteBuffer.ways)) &&
                                (getCurrentDataSet().getSelectedRelations().isEmpty() ||
                                        ! containsSameKeysWithDifferentValues(pasteBuffer.relations)));
    }

    @Override public void pasteBufferChanged(DataSet newPasteBuffer) {
        possiblyEnable(getCurrentDataSet().getSelected(), newPasteBuffer);
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null || Main.pasteBuffer == null) {
            setEnabled(false);
            return;
        }
        possiblyEnable(getCurrentDataSet().getSelected(), Main.pasteBuffer);
    }
}
