// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AddVisitor;
import org.openstreetmap.josm.data.osm.visitor.DeleteVisitor;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;

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

    @Override public boolean executeCommand() {
        osm.visit(new AddVisitor(getLayer().data));
        return true;
    }

    @Override public void undoCommand() {
        osm.visit(new DeleteVisitor(getLayer().data));
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        added.add(osm);
    }

    @Override public MutableTreeNode description() {
        NameVisitor v = new NameVisitor();
        osm.visit(v);
        return new DefaultMutableTreeNode(
                new JLabel(tr("Add {0} {1}", tr(v.className), v.name), v.icon, JLabel.HORIZONTAL));
    }
}
