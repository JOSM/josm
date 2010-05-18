//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Moves the selection
 *
 * @author Frederik Ramm
 */
public class MoveAction extends JosmAction {

    public enum Direction { UP, LEFT, RIGHT, DOWN }
    private Direction myDirection;

    // any better idea?
    private static Object calltosupermustbefirststatementinconstructor(Direction dir, boolean text) {
        Shortcut sc;
        String directiontext;
        if        (dir == Direction.UP)   {
            directiontext = tr("up");
            sc = Shortcut.registerShortcut("core:moveup",    tr("Move objects {0}", directiontext), KeyEvent.VK_UP,    Shortcut.GROUPS_ALT1+Shortcut.GROUP_DIRECT);
        } else if (dir == Direction.DOWN)  {
            directiontext = tr("down");
            sc = Shortcut.registerShortcut("core:movedown",  tr("Move objects {0}", directiontext), KeyEvent.VK_DOWN,  Shortcut.GROUPS_ALT1+Shortcut.GROUP_DIRECT);
        } else if (dir == Direction.LEFT)  {
            directiontext = tr("left");
            sc = Shortcut.registerShortcut("core:moveleft",  tr("Move objects {0}", directiontext), KeyEvent.VK_LEFT,  Shortcut.GROUPS_ALT1+Shortcut.GROUP_DIRECT);
        } else { //dir == Direction.RIGHT) {
            directiontext = tr("right");
            sc = Shortcut.registerShortcut("core:moveright", tr("Move objects {0}", directiontext), KeyEvent.VK_RIGHT, Shortcut.GROUPS_ALT1+Shortcut.GROUP_DIRECT);
        }
        if (text)
            return directiontext;
        else
            return sc;
    }

    public MoveAction(Direction dir) {
        super(tr("Move {0}", calltosupermustbefirststatementinconstructor(dir, true)), null,
                tr("Moves Objects {0}", calltosupermustbefirststatementinconstructor(dir, true)),
                (Shortcut)calltosupermustbefirststatementinconstructor(dir, false), true);
        myDirection = dir;
        putValue("help", ht("/Action/Move"));
    }

    public void actionPerformed(ActionEvent event) {

        // find out how many "real" units the objects have to be moved in order to
        // achive an 1-pixel movement

        EastNorth en1 = Main.map.mapView.getEastNorth(100, 100);
        EastNorth en2 = Main.map.mapView.getEastNorth(101, 101);

        double distx = en2.east() - en1.east();
        double disty = en2.north() - en1.north();

        switch (myDirection) {
            case UP:
                distx = 0;
                disty = -disty;
                break;
            case DOWN:
                distx = 0;
                break;
            case LEFT:
                disty = 0;
                distx = -distx;
                break;
            default:
                disty = 0;
        }

        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);

        Command c = !Main.main.undoRedo.commands.isEmpty()
        ? Main.main.undoRedo.commands.getLast() : null;

        if (c instanceof MoveCommand && affectedNodes.equals(((MoveCommand)c).getParticipatingPrimitives())) {
            ((MoveCommand)c).moveAgain(distx, disty);
        } else {
            Main.main.undoRedo.add(
                    c = new MoveCommand(selection, distx, disty));
        }

        for (Node n : affectedNodes) {
            if (n.getCoor().isOutSideWorld()) {
                // Revert move
                ((MoveCommand) c).moveAgain(-distx, -disty);
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Cannot move objects outside of the world."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }

        Main.map.mapView.repaint();
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
