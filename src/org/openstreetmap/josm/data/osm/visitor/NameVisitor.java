// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Able to create a name and an icon for each data element.
 * 
 * @author imi
 */
public class NameVisitor implements Visitor {

	/**
	 * The name of the item class
	 */
	public String className;
	/**
	 * The name of this item.
	 */
	public String name;
	/**
	 * The icon of this item.
	 */
	public Icon icon;
	
	/** 
	 * For formatting lat/lon
	 */
	public static NumberFormat latLonFormat = new DecimalFormat("###0.0000000");
	
	/**
	 * If the node has a name-key or id-key, this is displayed. If not, (lat,lon)
	 * is displayed.
	 */
	public void visit(Node n) {
		if (n.incomplete) {
			name = tr("incomplete");
		} else {
			name = n.get("name");
			if (name == null) {
				name = n.id == 0 ? "" : ""+n.id;
			}
			name += " ("+latLonFormat.format(n.coor.lat())+", "+latLonFormat.format(n.coor.lon())+")";
		}
		addId(n);
		icon = ImageProvider.get("data", "node");
		trn("node", "nodes", 0); // no marktrn available
		className = "node";
	}

	/**
	 * If the way has a name-key or id-key, this is displayed. If not, (x nodes)
	 * is displayed with x being the number of nodes in the way.
	 */
	public void visit(Way w) {
		if (w.incomplete) {
			name = tr("incomplete");
		} else {
			name = w.get("name");
			if (name == null) name = w.get("ref");
			if (name == null) {
				name = 
					(w.get("highway") != null) ? "highway" :
					(w.get("railway") != null) ? "railway" :
					(w.get("waterway") != null) ? "waterway" : "";
			}

			int nodesNo = new HashSet<Node>(w.nodes).size();
			name += trn(" ({0} node)", " ({0} nodes)", nodesNo, nodesNo);
		}
		addId(w);
		icon = ImageProvider.get("data", "way");
		trn("way", "ways", 0); // no marktrn available
		className = "way";
	}
	
	/**
	 */
	public void visit(Relation e) {
		if (e.incomplete) {
			name = tr("incomplete");
		} else {
			name = e.get("type");
			// FIXME add names of members
			if (name == null)
				name = "relation";
			
			name += " (";
			String nameTag = e.get("name");
			if (nameTag == null) nameTag = e.get("ref");
			if (nameTag != null) name += "\"" + nameTag + "\", ";
			int mbno = e.members.size();
			name += trn("{0} member", "{0} members", mbno, mbno) + ")";
		}
		addId(e);
		icon = ImageProvider.get("data", "relation");
		trn("relation", "relations", 0); // no marktrn available
		className = "relation";
	}
	
	public JLabel toLabel() {
		return new JLabel(name, icon, JLabel.HORIZONTAL);
	}


	private void addId(OsmPrimitive osm) {
	    if (Main.pref.getBoolean("osm-primitives.showid"))
			name += " (id: "+osm.id+")";
    }
}
