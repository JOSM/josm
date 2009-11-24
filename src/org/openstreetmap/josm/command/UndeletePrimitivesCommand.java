// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents a command for undeleting an {@see OsmPrimitive} which was deleted on the server.
 * The command remembers the former node id and sets the node id to 0. This turns
 * the node into a new node which can be uploaded to the server.
 *
 */
public class UndeletePrimitivesCommand extends ConflictResolveCommand {
    static private final Logger logger = Logger.getLogger(UndeletePrimitivesCommand.class.getName());

    /** the node to undelete */
    private final List<OsmPrimitive> toUndelete = new ArrayList<OsmPrimitive>();

    /**
     * constructor
     * @param node  the node to undelete
     */
    public UndeletePrimitivesCommand(OsmPrimitive node) {
        toUndelete.add(node);
    }

    /**
     * constructor
     * @param node  the node to undelete
     */
    public UndeletePrimitivesCommand(OsmPrimitive ... toUndelete) {
        for (int i=0; i < toUndelete.length; i++) {
            this.toUndelete.add(toUndelete[i]);
        }
    }

    /**
     * constructor
     * @param node  the node to undelete
     */
    public UndeletePrimitivesCommand(Collection<OsmPrimitive> toUndelete) {
        this();
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
            if(getLayer().getConflicts().hasConflictForMy(primitive)) {
                rememberConflict(getLayer().getConflicts().getConflictForMy(primitive));
                getLayer().getConflicts().remove(primitive);
            }
            primitive.clearOsmId();
        }
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.addAll(toUndelete);
    }
}
