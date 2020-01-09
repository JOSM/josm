// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Locale;

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
        UP(tr("up"), "up", KeyEvent.VK_UP),
        /* SHORTCUT(Move objects up, core:move:up, SHIFT, UP) */
        /** Move left */
        LEFT(tr("left"), "previous", KeyEvent.VK_LEFT),
        /* SHORTCUT(Move objects left, core:move:left, SHIFT, LEFT) */
        /** Move right */
        RIGHT(tr("right"), "next", KeyEvent.VK_RIGHT),
        /* SHORTCUT(Move objects right, core:move:right, SHIFT, RIGHT) */
        /** Move down */
        DOWN(tr("down"), "down", KeyEvent.VK_DOWN);
        /* SHORTCUT(Move objects down, core:move:down, SHIFT, DOWN) */

        private final String localizedName;
        private final String icon;
        private final int shortcutKey;

        Direction(String localizedName, String icon, int shortcutKey) {
            this.localizedName = localizedName;
            this.icon = icon;
            this.shortcutKey = shortcutKey;
        }

        String getId() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        String getLocalizedName() {
            return localizedName;
        }

        String getIcon() {
            return "dialogs/" + icon;
        }

        String getToolbarName() {
            return "action/move/" + getId();
        }

        int getShortcutKey() {
            return shortcutKey;
        }

        Shortcut getShortcut() {
            return Shortcut.registerShortcut(/* NO-SHORTCUT - adapt definition above when modified */
                    "core:move" + getId(), tr("Move objects {0}", getLocalizedName()), getShortcutKey(), Shortcut.SHIFT);
        }
    }

    private final Direction myDirection;

    /**
     * Constructs a new {@code MoveAction}.
     * @param dir direction
     */
    public MoveAction(Direction dir) {
        super(tr("Move {0}", dir.getLocalizedName()), dir.getIcon(),
                tr("Moves Objects {0}", dir.getLocalizedName()),
                dir.getShortcut(), true, dir.getToolbarName(), true);
        myDirection = dir;
        setHelpId(ht("/Action/Move"));
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
