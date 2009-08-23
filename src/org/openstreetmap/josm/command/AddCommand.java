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
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A command that adds an osm primitive to a dataset. Keys cannot be added this
 * way.
 *
 * See {@see ChangeCommand} for comments on relation back references.
 *
 * @author imi
 */
public class AddCommand extends Command {

    /**
     * The primitive to add to the dataset.
     */
    private final OsmPrimitive osm;

    /**
     * Create the command and specify the element to add.
     */
    public AddCommand(OsmPrimitive osm) {
        super();
        this.osm = osm;
    }

    /**
     * Create the command and specify the element to add.
     */
    public AddCommand(OsmDataLayer layer, OsmPrimitive osm) {
        super(layer);
        this.osm = osm;
    }

    @Override public boolean executeCommand() {
        getLayer().data.addPrimitive(osm);
        return true;
    }

    @Override public void undoCommand() {
        getLayer().data.removePrimitive(osm);
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        added.add(osm);
    }

    @Override public MutableTreeNode description() {
        String msg;
        switch(OsmPrimitiveType.from(osm)) {
        case NODE: msg = marktr("Add node {0}"); break;
        case WAY: msg = marktr("Add way {0}"); break;
        case RELATION: msg = marktr("Add relation {0}"); break;
        default: /* should not happen */msg = ""; break;
        }

        return new DefaultMutableTreeNode(
                new JLabel(
                        tr(msg,
                                osm.getDisplayName(DefaultNameFormatter.getInstance())
                        ),
                        ImageProvider.get(OsmPrimitiveType.from(osm)),
                        JLabel.HORIZONTAL
                )
        );
    }
}
