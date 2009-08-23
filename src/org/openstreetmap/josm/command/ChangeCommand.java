// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.PrimitiveNameFormatter;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command that basically replaces one OSM primitive by another of the
 * same type.
 *
 * @author Imi
 */
public class ChangeCommand extends Command {

    private final OsmPrimitive osm;
    private final OsmPrimitive newOsm;

    public ChangeCommand(OsmPrimitive osm, OsmPrimitive newOsm) {
        super();
        this.osm = osm;
        this.newOsm = newOsm;
    }

    @Override public boolean executeCommand() {
        super.executeCommand();
        osm.cloneFrom(newOsm);
        osm.modified = true;
        return true;
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        modified.add(osm);
    }

    @Override public MutableTreeNode description() {
        String msg = "";
        switch(OsmPrimitiveType.from(osm)) {
        case NODE: msg = marktr("Change node {0}"); break;
        case WAY: msg = marktr("Change way {0}"); break;
        case RELATION: msg = marktr("Change relation {0}"); break;
        }
        return new DefaultMutableTreeNode(
                new JLabel(tr(msg,
                        new PrimitiveNameFormatter().getName(osm)),
                        ImageProvider.get(OsmPrimitiveType.from(osm)),
                        JLabel.HORIZONTAL));
    }
}
