// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Mirror the selected nodes or ways along the vertical axis
 *
 * Note: If a ways are selected, their nodes are mirrored
 *
 * @author Teemu Koskinen
 */
public final class MirrorAction extends JosmAction {

    /**
     * Constructs a new {@code MirrorAction}.
     */
    public MirrorAction() {
        super(tr("Mirror"), "mirror", tr("Mirror selected nodes and ways."),
                Shortcut.registerShortcut("tools:mirror", tr("Tool: {0}", tr("Mirror")),
                        KeyEvent.VK_M, Shortcut.SHIFT), true);
        putValue("help", ht("/Action/Mirror"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> sel = getLayerManager().getEditDataSet().getSelected();
        Set<Node> nodes = new HashSet<>();

        for (OsmPrimitive osm : sel) {
            if (osm instanceof Node) {
                nodes.add((Node) osm);
            } else if (osm instanceof Way) {
                nodes.addAll(((Way) osm).getNodes());
            }
        }

        if (nodes.isEmpty()) {
            new Notification(
                    tr("Please select at least one node or way."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        double minEast = 20000000000.0;
        double maxEast = -20000000000.0;
        for (Node n : nodes) {
            double east = n.getEastNorth().east();
            minEast = Math.min(minEast, east);
            maxEast = Math.max(maxEast, east);
        }
        double middle = (minEast + maxEast) / 2;

        Collection<Command> cmds = new LinkedList<>();

        for (Node n : nodes) {
            cmds.add(new MoveCommand(n, 2 * (middle - n.getEastNorth().east()), 0.0));
        }

        UndoRedoHandler.getInstance().add(new SequenceCommand(tr("Mirror"), cmds));
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledStateOnModifiableSelection(selection);
    }
}
