//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Shortcut;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

public class JoinNodeWayAction extends JosmAction {

    protected final boolean joinWayToNode;

    protected JoinNodeWayAction(boolean joinWayToNode, String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar) {
        super(name, iconName, tooltip, shortcut, registerInToolbar);
        this.joinWayToNode = joinWayToNode;
    }

    /**
     * Constructs a Join Node to Way action.
     */
    public static JoinNodeWayAction createJoinNodeToWayAction() {
        JoinNodeWayAction action = new JoinNodeWayAction(false,
                tr("Join Node to Way"), "joinnodeway", tr("Include a node into the nearest way segments"),
                Shortcut.registerShortcut("tools:joinnodeway", tr("Tool: {0}", tr("Join Node to Way")), KeyEvent.VK_J, Shortcut.DIRECT), true);
        action.putValue("help", ht("/Action/JoinNodeWay"));
        return action;
    }

    /**
     * Constructs a Move Node onto Way action.
     */
    public static JoinNodeWayAction createMoveNodeOntoWayAction() {
        JoinNodeWayAction action = new JoinNodeWayAction(true,
                tr("Move Node onto Way"), "movewayontonode", tr("Move the node onto the nearest way segments and include it"),
                null, true);
        return action;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        Collection<Node> selectedNodes = getCurrentDataSet().getSelectedNodes();
        // Allow multiple selected nodes too?
        if (selectedNodes.size() != 1) return;

        final Node node = selectedNodes.iterator().next();

        Collection<Command> cmds = new LinkedList<>();

        // If the user has selected some ways, only join the node to these.
        boolean restrictToSelectedWays =
                !getCurrentDataSet().getSelectedWays().isEmpty();

        List<WaySegment> wss = Main.map.mapView.getNearestWaySegments(
                Main.map.mapView.getPoint(node), OsmPrimitive.isSelectablePredicate);
        MultiMap<Way, Integer> insertPoints = new MultiMap<>();
        for (WaySegment ws : wss) {
            // Maybe cleaner to pass a "isSelected" predicate to getNearestWaySegments, but this is less invasive.
            if (restrictToSelectedWays && !ws.way.isSelected()) {
                continue;
            }

            if (ws.getFirstNode() != node && ws.getSecondNode() != node) {
                insertPoints.put(ws.way, ws.lowerIndex);
            }
        }

        for (Map.Entry<Way, Set<Integer>> entry : insertPoints.entrySet()) {
            final Way w = entry.getKey();
            final Set<Integer> insertPointsForWay = entry.getValue();
            if (insertPointsForWay.isEmpty()) {
                continue;
            }

            List<Node> nodesToAdd = w.getNodes();
            for (int i : pruneSuccsAndReverse(insertPointsForWay)) {
                if (joinWayToNode) {
                    EastNorth newPosition = Geometry.closestPointToSegment(
                            w.getNode(i).getEastNorth(), w.getNode(i + 1).getEastNorth(), node.getEastNorth());
                    cmds.add(new MoveCommand(node, Projections.inverseProject(newPosition)));
                }
                nodesToAdd.add(i + 1, node);
            }
            Way wnew = new Way(w);
            wnew.setNodes(nodesToAdd);
            cmds.add(new ChangeCommand(w, wnew));
        }
        if (cmds.isEmpty()) return;
        Main.main.undoRedo.add(new SequenceCommand(getValue(NAME).toString(), cmds));
        Main.map.repaint();
    }

    private static SortedSet<Integer> pruneSuccsAndReverse(Collection<Integer> is) {
        SortedSet<Integer> is2 = new TreeSet<>(Collections.reverseOrder());
        for (int i : is) {
            if (!is2.contains(i - 1) && !is2.contains(i + 1)) {
                is2.add(i);
            }
        }
        return is2;
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
