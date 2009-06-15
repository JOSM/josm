// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents a command for undeleting a node which was deleted on the server.
 * The command remembers the former node id and sets the node id to 0. This turns
 * the node into a new node which can be uploaded to the server.
 *
 */
public class UndeletePrimitivesCommand extends Command {

    /** the node to undelete */
    private ArrayList<OsmPrimitive> toUndelete;

    /**
     * constructor
     * @param node  the node to undelete
     */
    public UndeletePrimitivesCommand(OsmPrimitive node) {
        toUndelete = new ArrayList<OsmPrimitive>();
        toUndelete.add(node);
    }

    /**
     * constructor
     * @param node  the node to undelete
     */
    public UndeletePrimitivesCommand(OsmPrimitive ... toUndelete) {
        this.toUndelete = new ArrayList<OsmPrimitive>();
        for (int i=0; i < toUndelete.length; i++) {
            this.toUndelete.add(toUndelete[i]);
        }
    }

    /**
     * constructor
     * @param node  the node to undelete
     */
    public UndeletePrimitivesCommand(Collection<OsmPrimitive> toUndelete) {
        this.toUndelete = new ArrayList<OsmPrimitive>();
        this.toUndelete.addAll(toUndelete);
    }


    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                        tr("Undelete {0} primitives", toUndelete.size()),
                        ImageProvider.get("data", "object"),
                        JLabel.HORIZONTAL
                )
        );
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        for(OsmPrimitive primitive: toUndelete) {
            primitive.id = 0;
        }
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.addAll(toUndelete);
    }
}
