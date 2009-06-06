// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Mirror the selected nodes or ways along the vertical axis
 *
 * Note: If a ways are selected, their nodes are mirrored
 *
 * @author Teemu Koskinen, based on much copy&Paste from other Actions.
 */
public final class MirrorAction extends JosmAction {

    public MirrorAction() {
        super(tr("Mirror"), "mirror", tr("Mirror selected nodes and ways."),
        Shortcut.registerShortcut("tools:mirror", tr("Tool: {0}", tr("Mirror")),
            KeyEvent.VK_M, Shortcut.GROUP_EDIT, Shortcut.SHIFT_DEFAULT), true);
    }

    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> sel = Main.ds.getSelected();
        HashSet<Node> nodes = new HashSet<Node>();

        for (OsmPrimitive osm : sel) {
            if (osm instanceof Node) {
                nodes.add((Node)osm);
            } else if (osm instanceof Way) {
                nodes.addAll(((Way)osm).nodes);
            }
        }

        if (nodes.size() == 0) {
            JOptionPane.showMessageDialog(Main.parent, tr("Please select at least one node or way."));
            return;
        }

        double minEast = 200.0;
        double maxEast = -200.0;
        for (Node n : nodes) {
            minEast = Math.min(minEast, n.eastNorth.east());
            maxEast = Math.max(maxEast, n.eastNorth.east());
        }
        double middle = (minEast + maxEast) / 2;

        Collection<Command> cmds = new LinkedList<Command>();

        for (Node n : nodes)
            cmds.add(new MoveCommand(n, 2 * (middle - n.eastNorth.east()), 0.0));

        Main.main.undoRedo.add(new SequenceCommand(tr("Mirror"), cmds));
        Main.map.repaint();
    }
}
