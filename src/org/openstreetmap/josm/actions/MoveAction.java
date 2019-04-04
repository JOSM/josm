// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Moves the selection
 *
 * @author Frederik Ramm
 */
public class MoveAction extends JosmAction {

    /**
     * Move direction.
     */
    public enum Direction {
        /** Move up */
        UP,
        /** Move left */
        LEFT,
        /** Move right */
        RIGHT,
        /** Move down */
        DOWN
    }

    private final Direction myDirection;

    // any better idea?
    private static String calltosupermustbefirststatementinconstructortext(Direction dir) {
        String directiontext;
        if (dir == Direction.UP) {
            directiontext = tr("up");
        } else if (dir == Direction.DOWN) {
            directiontext = tr("down");
        } else if (dir == Direction.LEFT) {
            directiontext = tr("left");
        } else {
            directiontext = tr("right");
        }
        return directiontext;
    }

    // any better idea?
    private static Shortcut calltosupermustbefirststatementinconstructor(Direction dir) {
        Shortcut sc;
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        if (dir == Direction.UP) {
            sc = Shortcut.registerShortcut("core:moveup",    tr("Move objects {0}", tr("up")),    KeyEvent.VK_UP,    Shortcut.SHIFT);
        } else if (dir == Direction.DOWN) {
            sc = Shortcut.registerShortcut("core:movedown",  tr("Move objects {0}", tr("down")),  KeyEvent.VK_DOWN,  Shortcut.SHIFT);
        } else if (dir == Direction.LEFT) {
            sc = Shortcut.registerShortcut("core:moveleft",  tr("Move objects {0}", tr("left")),  KeyEvent.VK_LEFT,  Shortcut.SHIFT);
        } else { //dir == Direction.RIGHT
            sc = Shortcut.registerShortcut("core:moveright", tr("Move objects {0}", tr("right")), KeyEvent.VK_RIGHT, Shortcut.SHIFT);
        }
        // CHECKSTYLE.ON: SingleSpaceSeparator
        return sc;
    }

    /**
     * Constructs a new {@code MoveAction}.
     * @param dir direction
     */
    public MoveAction(Direction dir) {
        super(tr("Move {0}", calltosupermustbefirststatementinconstructortext(dir)), null,
                tr("Moves Objects {0}", calltosupermustbefirststatementinconstructortext(dir)),
                calltosupermustbefirststatementinconstructor(dir), false);
        myDirection = dir;
        setHelpId(ht("/Action/Move"));
        if (dir == Direction.UP) {
            putValue("toolbar", "action/move/up");
        } else if (dir == Direction.DOWN) {
            putValue("toolbar", "action/move/down");
        } else if (dir == Direction.LEFT) {
            putValue("toolbar", "action/move/left");
        } else { //dir == Direction.RIGHT
            putValue("toolbar", "action/move/right");
        }
        MainApplication.getToolbar().register(this);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        DataSet ds = getLayerManager().getEditDataSet();

        if (!MainApplication.isDisplayingMapView() || ds == null)
            return;

        // find out how many "real" units the objects have to be moved in order to
        // achive an 1-pixel movement

        MapView mapView = MainApplication.getMap().mapView;
        EastNorth en1 = mapView.getEastNorth(100, 100);
        EastNorth en2 = mapView.getEastNorth(101, 101);

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

        Collection<OsmPrimitive> selection = ds.getSelected();
        Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);

        Command c = UndoRedoHandler.getInstance().getLastCommand();

        ds.beginUpdate();
        try {
            if (c instanceof MoveCommand && ds.equals(c.getAffectedDataSet())
                    && affectedNodes.equals(((MoveCommand) c).getParticipatingPrimitives())) {
                ((MoveCommand) c).moveAgain(distx, disty);
            } else {
                c = new MoveCommand(ds, selection, distx, disty);
                UndoRedoHandler.getInstance().add(c);
            }
        } finally {
            ds.endUpdate();
        }

        for (Node n : affectedNodes) {
            if (n.isLatLonKnown() && n.isOutSideWorld()) {
                // Revert move
                ((MoveCommand) c).moveAgain(-distx, -disty);
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        tr("Cannot move objects outside of the world."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }

        mapView.repaint();
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
