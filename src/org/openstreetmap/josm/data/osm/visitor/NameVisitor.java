// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

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
    public String classNamePlural;
    /**
     * The name of this item.
     */
    public String name;
    /**
     * The icon of this item.
     */
    public Icon icon;

    /**
     * If the node has a name-key or id-key, this is displayed. If not, (lat,lon)
     * is displayed.
     */
    public void visit(Node n) {
        name = n.getName();
        addId(n);
        icon = ImageProvider.get("data", "node");
        className = "node";
        classNamePlural = trn("node", "nodes", 2);
    }

    /**
     * If the way has a name-key or id-key, this is displayed. If not, (x nodes)
     * is displayed with x being the number of nodes in the way.
     */
    public void visit(Way w) {
        name = w.getName();
        addId(w);
        icon = ImageProvider.get("data", "way");
        className = "way";
        classNamePlural = trn("way", "ways", 2);
    }

    /**
     */
    public void visit(Relation e) {
        name = e.getName();
        addId(e);
        icon = ImageProvider.get("data", "relation");
        className = "relation";
        classNamePlural = trn("relation", "relations", 2);
    }

    public JLabel toLabel() {
        return new JLabel(name, icon, JLabel.HORIZONTAL);
    }


    private void addId(OsmPrimitive osm) {
        if (Main.pref.getBoolean("osm-primitives.showid"))
            name += tr(" [id: {0}]", osm.id);
    }
}
