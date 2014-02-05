//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.tools.Shortcut;

public class JoinNodeWayAction extends JosmAction {

    /**
     * Constructs a new {@code JoinNodeWayAction}.
     */
    public JoinNodeWayAction() {
        super(tr("Join Node to Way"), "joinnodeway", tr("Include a node into the nearest way segments"),
                Shortcut.registerShortcut("tools:joinnodeway", tr("Tool: {0}", tr("Join Node to Way")), KeyEvent.VK_J, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/JoinNodeWay"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        Collection<Node> selectedNodes = getCurrentDataSet().getSelectedNodes();
        // Allow multiple selected nodes too?
        if (selectedNodes.size() != 1) return;

        Node node = selectedNodes.iterator().next();

        Collection<Command> cmds = new LinkedList<Command>();

        // If the user has selected some ways, only join the node to these.
        boolean restrictToSelectedWays =
                !getCurrentDataSet().getSelectedWays().isEmpty();

            List<WaySegment> wss = Main.map.mapView.getNearestWaySegments(
                    Main.map.mapView.getPoint(node), OsmPrimitive.isSelectablePredicate);
            HashMap<Way, List<Integer>> insertPoints = new HashMap<Way, List<Integer>>();
            for (WaySegment ws : wss) {
                // Maybe cleaner to pass a "isSelected" predicate to getNearestWaySegments, but this is less invasive.
                if(restrictToSelectedWays && !ws.way.isSelected()) {
                    continue;
                }

                List<Integer> is;
                if (insertPoints.containsKey(ws.way)) {
                    is = insertPoints.get(ws.way);
                } else {
                    is = new ArrayList<Integer>();
                    insertPoints.put(ws.way, is);
                }

                if (ws.way.getNode(ws.lowerIndex) != node
                        && ws.way.getNode(ws.lowerIndex+1) != node) {
                    is.add(ws.lowerIndex);
                }
            }

            for (Map.Entry<Way, List<Integer>> insertPoint : insertPoints.entrySet()) {
                List<Integer> is = insertPoint.getValue();
                if (is.isEmpty()) {
                    continue;
                }

                Way w = insertPoint.getKey();
                List<Node> nodesToAdd = w.getNodes();
                pruneSuccsAndReverse(is);
                for (int i : is) {
                    nodesToAdd.add(i+1, node);
                }
                Way wnew = new Way(w);
                wnew.setNodes(nodesToAdd);
                cmds.add(new ChangeCommand(w, wnew));
            }
            if (cmds.isEmpty()) return;
            Main.main.undoRedo.add(new SequenceCommand(tr("Join Node and Line"), cmds));
            Main.map.repaint();
    }

    private static void pruneSuccsAndReverse(List<Integer> is) {
        HashSet<Integer> is2 = new HashSet<Integer>();
        for (int i : is) {
            if (!is2.contains(i - 1) && !is2.contains(i + 1)) {
                is2.add(i);
            }
        }
        is.clear();
        is.addAll(is2);
        Collections.sort(is);
        Collections.reverse(is);
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
