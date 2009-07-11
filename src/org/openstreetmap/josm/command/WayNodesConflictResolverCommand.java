// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represent a command for resolving conflicts in the node list of two
 * {@see Way}s.
 *
 */
public class WayNodesConflictResolverCommand extends ConflictResolveCommand {

    static private final Logger logger = Logger.getLogger(WayNodesConflictResolverCommand.class.getName());

    /** the conflict to resolve */
    private Conflict<Way> conflict;

    /** the list of merged nodes. This becomes the list of news of my way after the
     *  command is executed
     */
    private final List<Node> mergedNodeList;


    /**
     * 
     * @param my my may
     * @param their their way
     * @param mergedNodeList  the list of merged nodes
     */
    public WayNodesConflictResolverCommand(Way my, Way their, List<Node> mergedNodeList) {
        conflict = new Conflict<Way>(my,their);
        this.mergedNodeList = mergedNodeList;
    }


    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                        tr("Resolve conflicts in node list of of way {0}", conflict.getMy().id),
                        ImageProvider.get("data", "object"),
                        JLabel.HORIZONTAL
                )
        );
    }

    @Override
    public boolean executeCommand() {
        // remember the current state of 'my' way
        //
        super.executeCommand();

        // replace the list of nodes of 'my' way by the list of merged
        // nodes
        //
        conflict.getMy().nodes.clear();
        for (int i=0; i<mergedNodeList.size();i++) {
            Node n = mergedNodeList.get(i);
            conflict.getMy().nodes.add(n);
            if (! getLayer().data.nodes.contains(n)) {
                logger.warning(tr("Main dataset does not include node {0}", n.toString()));
            }
        }
        rememberConflict(conflict);
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(conflict.getMy());
    }
}
