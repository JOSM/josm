// License: GPL. Copyright 2007 by Immanuel Scholz and others
/**
 * 
 */
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

public final class ReverseSegmentAction extends JosmAction {

    public ReverseSegmentAction() {
    	super(tr("Reverse segments"), "segmentflip", tr("Reverse the direction of all selected Segments."), KeyEvent.VK_R, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK, true);
    }

	public void actionPerformed(ActionEvent e) {
    	final Collection<Segment> sel = new LinkedList<Segment>();
    	new Visitor(){
			public void visit(Node n)    {}
			public void visit(Segment s) {sel.add(s);}
			public void visit(Way w)     {sel.addAll(w.segments);}
			public void visitAll() {
				for (OsmPrimitive osm : Main.ds.getSelected())
					osm.visit(this);
			}
    	}.visitAll();

    	if (sel.isEmpty()) {
    		JOptionPane.showMessageDialog(Main.parent, tr("Please select at least one segment."));
    		return;
    	}
    	Collection<Command> c = new LinkedList<Command>();
    	for (Segment s : sel) {
    		Segment snew = new Segment(s);
    		Node n = snew.from;
    		snew.from = snew.to;
    		snew.to = n;
    		c.add(new ChangeCommand(s, snew));
    	}
    	Main.main.undoRedo.add(new SequenceCommand(tr("Reverse Segments"), c));
    	Main.map.repaint();
    }
}