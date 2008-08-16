// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.layer.Layer;

/**
 * Toggles the autoScale feature of the mapView
 * @author imi
 */
public class AutoScaleAction extends JosmAction {

	public static final String[] modes = {
		marktr("data"),
		marktr("layer"),
		marktr("selection"),
		marktr("conflict")
	};
	private final String mode;

	private static int getModeShortcut(String mode) {
		int shortcut = -1;

		if(mode.equals("data")) {
			shortcut = KeyEvent.VK_1;
		}        
		if(mode.equals("layer")) {
			shortcut = KeyEvent.VK_2;
		}
		if(mode.equals("selection")) {
			shortcut = KeyEvent.VK_3;
		}
		if(mode.equals("conflict")) {
			shortcut = KeyEvent.VK_4;
		}

		return shortcut;
	}
    
	public AutoScaleAction(String mode) {
		super(tr("Zoom to {0}", tr(mode)), "dialogs/autoscale/"+mode, tr("Zoom the view to {0}.", tr(mode)), AutoScaleAction.getModeShortcut(mode), 0, true);
		String modeHelp = Character.toUpperCase(mode.charAt(0))+mode.substring(1);
		putValue("help", "Action/AutoScale/"+modeHelp);
		this.mode = mode;
	}

	public void actionPerformed(ActionEvent e) {
		if (Main.map != null) {
			BoundingXYVisitor bbox = getBoundingBox();
			if (bbox != null) {
				Main.map.mapView.recalculateCenterScale(bbox);
			}
		}
		putValue("active", true);
	}

	private BoundingXYVisitor getBoundingBox() {
		BoundingXYVisitor v = new BoundingXYVisitor();
		if (mode.equals("data")) {
			for (Layer l : Main.map.mapView.getAllLayers())
				l.visitBoundingBox(v);
		} else if (mode.equals("layer"))
			Main.map.mapView.getActiveLayer().visitBoundingBox(v);
		else if (mode.equals("selection") || mode.equals("conflict")) {
			Collection<OsmPrimitive> sel = mode.equals("selection") ? Main.ds.getSelected() : Main.map.conflictDialog.conflicts.keySet();
			if (sel.isEmpty()) {
	    		JOptionPane.showMessageDialog(Main.parent,
	    				mode.equals("selection") ? tr("Nothing selected to zoom to.") : tr("No conflicts to zoom to"));
	        		return null;
			}
			for (OsmPrimitive osm : sel)
				osm.visit(v);
			// increase bbox by 0.001 degrees on each side. this is required 
			// especially if the bbox contains one single node, but helpful 
			// in most other cases as well.
			if (v.min != null && v.max != null) // && v.min.north() == v.max.north() && v.min.east() == v.max.east()) {
			{
				EastNorth en = new EastNorth(0.0001,0.0001);
				v.min = new EastNorth(v.min.east()-en.east(), v.min.north()-en.north());
				v.max = new EastNorth(v.max.east()+en.east(), v.max.north()+en.north());
			}
		}
		return v;
	}
}
