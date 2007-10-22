// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.xnap.commons.i18n.I18n.marktr;

import java.awt.event.ActionEvent;
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
		marktr("selection"),
		marktr("layer"),
		marktr("conflict")
	};
	private final String mode;

	public AutoScaleAction(String mode) {
		super(tr("Zoom to {0}", mode), "dialogs/autoscale/"+mode, tr("Zoom the view to {0}.", tr(mode)), 0, 0, true);
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
			// special case to zoom nicely to one single node
			if (v.min != null && v.max != null && v.min.north() == v.max.north() && v.min.east() == v.max.east()) {
				EastNorth en = Main.proj.latlon2eastNorth(new LatLon(0.001, 0.001));
				v.min = new EastNorth(v.min.east()-en.east(), v.min.north()-en.north());
				v.max = new EastNorth(v.max.east()+en.east(), v.max.north()+en.north());
			}
		}
		return v;
	}
}
