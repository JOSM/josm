// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
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
    private Map<OsmPrimitive,OsmPrimitive> resolvedConflicts;

    protected UndeletePrimitivesCommand() {
        toUndelete = new ArrayList<OsmPrimitive>();
        resolvedConflicts = new HashMap<OsmPrimitive, OsmPrimitive>();
    }
    /**
     * constructor
     * @param node  the node to undelete
     */
    public UndeletePrimitivesCommand(OsmPrimitive node) {
        this();
        toUndelete.add(node);
    }

    /**
     * constructor
     * @param node  the node to undelete
     */
    public UndeletePrimitivesCommand(OsmPrimitive ... toUndelete) {
        this();
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
            if (Main.map.conflictDialog.conflicts.containsKey(primitive)) {
                resolvedConflicts.put(primitive, Main.map.conflictDialog.conflicts.get(primitive));
                Main.map.conflictDialog.removeConflictForPrimitive(primitive);
            }
            primitive.id = 0;
        }
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.addAll(toUndelete);
    }
    @Override
    public void undoCommand() {
        super.undoCommand();

        for (OsmPrimitive my: resolvedConflicts.keySet()) {
            if (!Main.map.conflictDialog.conflicts.containsKey(my)) {
                Main.map.conflictDialog.addConflict(my, resolvedConflicts.get(my));
            }
        }
    }
}
