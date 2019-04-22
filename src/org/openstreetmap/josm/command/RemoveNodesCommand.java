// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Command that removes a set of nodes from a way.
 * The same can be done with ChangeNodesCommand, but this is more
 * efficient. (Needed for the tool to disconnect nodes from ways.)
 *
 * @author Giuseppe Bilotta
 */
public class RemoveNodesCommand extends AbstractNodesCommand<Set<Node>> {

    /**
     * Constructs a new {@code RemoveNodesCommand}.
     * @param way The way to modify. Must not be null, and belong to a data set
     * @param rmNodes The list of nodes to remove
     * @deprecated Use {@link #RemoveNodesCommand(Way, Set)}
     */
    @Deprecated
    public RemoveNodesCommand(Way way, List<Node> rmNodes) {
        super(way.getDataSet(), way, new HashSet<>(rmNodes));
    }

    /**
     * Constructs a new {@code RemoveNodesCommand}.
     * @param way The way to modify. Must not be null, and belong to a data set
     * @param rmNodes The set of nodes to remove
     * @since xxx
     */
    public RemoveNodesCommand(Way way, Set<Node> rmNodes) {
        super(way.getDataSet(), way, rmNodes);
    }

    /**
     * Constructs a new {@code RemoveNodesCommand}.
     * @param ds The target data set. Must not be {@code null}
     * @param way The way to modify. Must not be null, and belong to a data set
     * @param rmNodes The list of nodes to remove
     * @since 15013
     */
    public RemoveNodesCommand(DataSet ds, Way way, Set<Node> rmNodes) {
        super(ds, way, rmNodes);
    }

    @Override
    protected void modifyWay() {
        way.removeNodes(cmdNodes);
    }

    @Override
    public String getDescriptionText() {
        return tr("Removed nodes from {0}", way.getDisplayName(DefaultNameFormatter.getInstance()));
    }
}
