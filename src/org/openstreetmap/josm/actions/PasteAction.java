// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.coor.EastNorth;

public final class PasteAction extends JosmAction {

    public PasteAction() {
    	super(tr("Paste"), "paste",
			tr("Paste contents of paste buffer."),
			KeyEvent.VK_V, KeyEvent.CTRL_MASK, true);
		setEnabled(false);
    }

	public void actionPerformed(ActionEvent e) {
		DataSet pasteBuffer = Main.pasteBuffer;

		/* Find the middle of the pasteBuffer area */ 
		double maxEast = -1E100, minEast = 1E100, maxNorth = -1E100, minNorth = 1E100;
		for (Node n : pasteBuffer.nodes) {
			double east = n.eastNorth.east();
			double north = n.eastNorth.north();
			if (east > maxEast) { maxEast = east; } 
			if (east < minEast) { minEast = east; } 
			if (north > maxNorth) { maxNorth = north; } 
			if (north < minNorth) { minNorth = north; } 
		}

		EastNorth mPosition;
		if((e.getModifiers() & ActionEvent.CTRL_MASK) ==0){
			mPosition = Main.map.mapView.getCenter();
		} else {
			mPosition = Main.map.mapView.getEastNorth(Main.map.mapView.lastMEvent.getX(), Main.map.mapView.lastMEvent.getY());
		}

		double offsetEast  = mPosition.east() - (maxEast + minEast)/2.0;
		double offsetNorth = mPosition.north() - (maxNorth + minNorth)/2.0;
		
		HashMap<OsmPrimitive,OsmPrimitive> map = new HashMap<OsmPrimitive,OsmPrimitive>(); 
		  /* temporarily maps old nodes to new so we can do a true deep copy */
		
		/* do the deep copy of the paste buffer contents, leaving the pasteBuffer unchanged */
		for (Node n : pasteBuffer.nodes) {
			Node nnew = new Node(n);
			nnew.id = 0;
			/* adjust the coordinates to the middle of the visible map area */
			nnew.eastNorth = new EastNorth(nnew.eastNorth.east() + offsetEast, nnew.eastNorth.north() + offsetNorth);
			nnew.coor = Main.proj.eastNorth2latlon(nnew.eastNorth);
			map.put(n, nnew);
		}
		for (Way w : pasteBuffer.ways) {
			Way wnew = new Way();
			wnew.cloneFrom(w);
			wnew.id = 0;
			/* make sure we reference the new nodes corresponding to the old ones */
			List<Node> nodes = new ArrayList<Node>();
			for (Node n : w.nodes) {
				nodes.add((Node)map.get(n));
			}
			wnew.nodes.clear();
			wnew.nodes.addAll(nodes);
			map.put(w, wnew);
		}
		for (Relation r : pasteBuffer.relations) {
			Relation rnew = new Relation(r);
			rnew.id = 0;
			List<RelationMember> members = new ArrayList<RelationMember>();
			for (RelationMember m : r.members) {
				RelationMember mnew = new RelationMember(m);
				mnew.member = map.get(m.member);
				members.add(mnew);
			}
			rnew.members.clear();
			rnew.members.addAll(members);
			map.put(r, rnew);
		}
		
		/* Now execute the commands to add the dupicated contents of the paste buffer to the map */
		Collection<OsmPrimitive> osms = map.values();
		Collection<Command> clist = new LinkedList<Command>();
		for (OsmPrimitive osm : osms) {
			clist.add(new AddCommand(osm));
		}

		Main.main.undoRedo.add(new SequenceCommand(tr("Paste"), clist));
		Main.ds.setSelected(osms);
		Main.map.mapView.repaint();
    }
}
