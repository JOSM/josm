// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.corrector.ReverseWayTagCorrector;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

public final class ReverseWayAction extends JosmAction {

    public ReverseWayAction() {
    	super(tr("Reverse ways"), "wayflip",
			tr("Reverse the direction of all selected ways."),
			KeyEvent.VK_R, 0, true);
    }

	public void actionPerformed(ActionEvent e) {
    	final Collection<Way> sel = new LinkedList<Way>();
    	new Visitor(){
			public void visit(Node n)    {}
			public void visit(Way w)     {sel.add(w);}
			public void visit(Relation e){}
			public void visitAll() {
				for (OsmPrimitive osm : Main.ds.getSelected())
					osm.visit(this);
			}
    	}.visitAll();

    	if (sel.isEmpty()) {
    		JOptionPane.showMessageDialog(Main.parent,
				tr("Please select at least one way."));
    		return;
    	}

    	boolean propertiesUpdated = false;
		ReverseWayTagCorrector reverseWayTagCorrector = new ReverseWayTagCorrector();
		Collection<Command> c = new LinkedList<Command>();
		for (Way w : sel) {
			Way wnew = new Way(w);
			Collections.reverse(wnew.nodes);
			if (Main.pref.getBoolean("tag-correction.reverse-way", true))
				propertiesUpdated = reverseWayTagCorrector.execute(wnew) || propertiesUpdated;
			c.add(new ChangeCommand(w, wnew));
		}
		Main.main.undoRedo.add(new SequenceCommand(tr("Reverse ways"), c));
		if (propertiesUpdated)
			Main.map.getPropertiesDialog().selectionChanged(Main.ds.getSelected());
		Main.map.repaint();
    }
}
