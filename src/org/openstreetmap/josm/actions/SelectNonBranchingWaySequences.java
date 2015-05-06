// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Auxiliary class for the {@link SelectNonBranchingWaySequencesAction}.
 *
 * @author Marko Mäkelä
 */
public class SelectNonBranchingWaySequences {
    /**
     * outer endpoints of selected ways
     */
    private Set<Node> outerNodes;
    /**
     * endpoints of selected ways
     */
    private Set<Node> nodes;

    /**
     * Creates a way selection
     *
     * @param ways selection a selection of ways
     */
    public SelectNonBranchingWaySequences(final Collection<Way> ways) {
        if (ways.isEmpty()) {
            // The selection cannot be extended.
            outerNodes = null;
            nodes = null;
        } else {
            nodes = new TreeSet<>();
            outerNodes = new TreeSet<>();

            for (Way way : ways)
                addNodes(way);
        }
    }

    /**
     * Add a way endpoint to nodes, outerNodes
     *
     * @param node a way endpoint
     */
    private void addNodes(Node node) {
        if (node == null) return;
        else if (!nodes.add(node))
            outerNodes.remove(node);
        else
            outerNodes.add(node);
    }

    /**
     * Add the endpoints of the way to nodes, outerNodes
     *
     * @param way a way whose endpoints are added
     */
    private void addNodes(Way way) {
        addNodes(way.firstNode());
        addNodes(way.lastNode());
    }

    /**
     * Find out if the selection can be extended
     *
     * @return true if the selection can be extended
     */
    public boolean canExtend() {
        return outerNodes != null && !outerNodes.isEmpty();
    }

    /**
     * Finds out if the current selection can be extended.
     *
     * @param selection current selection (ways and others)
     * @param node      perimeter node from which to extend the selection
     * @return a way by which to extend the selection, or null
     */
    private static Way findWay(Collection<OsmPrimitive> selection, Node node) {
        Way foundWay = null;

        for (Way way : OsmPrimitive.getFilteredList(node.getReferrers(),
                Way.class)) {
            if (way.getNodesCount() < 2 || !way.isFirstLastNode(node)
                    || selection.contains(way))
                continue;

            /* A previously unselected way was found that is connected
            to the node. */
            if (foundWay != null)
                /* This is not the only qualifying way. There is a
                branch at the node, and we cannot extend the selection. */
                return null;

            /* Remember the first found qualifying way. */
            foundWay = way;
        }

        /* Return the only way found, or null if none was found. */
        return foundWay;
    }

    /**
     * Finds out if the current selection can be extended.
     * <p>
     * The members outerNodes, nodes must have been initialized.
     * How to update these members when extending the selection, @see extend().
     * </p>
     * @param selection current selection
     * @return a way by which to extend the selection, or null
     */
    private Way findWay(Collection<OsmPrimitive> selection) {
        for (Node node : outerNodes) {
            Way way = findWay(selection, node);
            if (way != null)
                return way;
        }

        return null;
    }

    /**
     * Extend the current selection
     *
     * @param data the data set in which to extend the selection
     */
    public void extend(DataSet data) {
        Collection<OsmPrimitive> currentSelection;
        Collection<OsmPrimitive> selection;
        boolean selectionChanged = false;
        Way way;

        if (!canExtend())
            return;

        currentSelection = data.getSelected();

        way = findWay(currentSelection);

        if (way == null)
            return;

        selection = new LinkedList<>();
        for (OsmPrimitive primitive : currentSelection)
            selection.add(primitive);

        do {
            if (!selection.add(way))
                break;

            selectionChanged = true;
            addNodes(way);

            way = findWay(selection);
        } while (way != null);

        if (selectionChanged)
            data.setSelected(selection, true);
    }
}
