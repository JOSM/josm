// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Command that changes the nodes list of a way.
 * The same can be done with ChangeCommand, but this is more
 * efficient. (Needed for the duplicate node fixing
 * tool of the validator, when processing large data sets.)
 *
 * @author Imi
 */
public class ChangeNodesCommand extends AbstractNodesCommand<List<Node>> {

    /**
     * Constructs a new {@code ChangeNodesCommand}.
     * @param way The way to modify
     * @param newNodes The new list of nodes for the given way
     */
    public ChangeNodesCommand(Way way, List<Node> newNodes) {
        super(way.getDataSet(), way, newNodes);
    }

    /**
     * Constructs a new {@code ChangeNodesCommand}.
     * @param ds The target data set. Must not be {@code null}
     * @param way The way to modify
     * @param newNodes The new list of nodes for the given way
     * @since 12726
     */
    public ChangeNodesCommand(DataSet ds, Way way, List<Node> newNodes) {
        super(ds, way, newNodes);
    }

    @Override
    public void modifyWay() {
        way.setNodes(cmdNodes);
    }

    @Override
    public String getDescriptionText() {
        return tr("Change nodes of {0}", way.getDisplayName(DefaultNameFormatter.getInstance()));
    }
}
