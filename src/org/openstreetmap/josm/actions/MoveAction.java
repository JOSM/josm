//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

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
import org.openstreetmap.josm.tools.ShortCut;

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
		ShortCut sc;
		String directiontext;
		if        (dir == Direction.UP)   {
			directiontext = tr("up");
			sc = ShortCut.registerShortCut("core:moveup",    tr("Move objects {0}", directiontext), KeyEvent.VK_UP,    ShortCut.GROUPS_ALT1+ShortCut.GROUP_DIRECT);
		} else if (dir == Direction.DOWN)  {
			directiontext = tr("down");
			sc = ShortCut.registerShortCut("core:movedown",  tr("Move objects {0}", directiontext), KeyEvent.VK_DOWN,  ShortCut.GROUPS_ALT1+ShortCut.GROUP_DIRECT);
		} else if (dir == Direction.LEFT)  {
			directiontext = tr("left");
			sc = ShortCut.registerShortCut("core:moveleft",  tr("Move objects {0}", directiontext), KeyEvent.VK_LEFT,  ShortCut.GROUPS_ALT1+ShortCut.GROUP_DIRECT);
		} else { //dir == Direction.RIGHT) {
			directiontext = tr("right");
			sc = ShortCut.registerShortCut("core:moveright", tr("Move objects {0}", directiontext), KeyEvent.VK_RIGHT, ShortCut.GROUPS_ALT1+ShortCut.GROUP_DIRECT);
		}
		if (text) {
			return directiontext;
		} else {
			return sc;
		}
	}

	public MoveAction(Direction dir) {
		super(tr("Move {0}", calltosupermustbefirststatementinconstructor(dir, true)), null,
		      tr("Moves Objects {0}", calltosupermustbefirststatementinconstructor(dir, true)),
		      (ShortCut)calltosupermustbefirststatementinconstructor(dir, false), true);
		myDirection = dir;
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
		default:
			disty = 0;
		}

		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);

		Command c = !Main.main.undoRedo.commands.isEmpty()
		? Main.main.undoRedo.commands.getLast() : null;

		if (c instanceof MoveCommand && affectedNodes.equals(((MoveCommand)c).objects))
			((MoveCommand)c).moveAgain(distx, disty);
		else
			Main.main.undoRedo.add(
					c = new MoveCommand(selection, distx, disty));

		for (Node n : affectedNodes) {
			if (n.coor.isOutSideWorld()) {
				// Revert move
				((MoveCommand) c).moveAgain(-distx, -disty);
				JOptionPane.showMessageDialog(Main.parent,
						tr("Cannot move objects outside of the world."));
				return;
			}
		}

		Main.map.mapView.repaint();
	}
}
