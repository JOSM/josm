// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Geometry;

/**
 * Check coastlines for errors
 *
 * @author frsantos
 * @author Teemu Koskinen
 * @author Gerd Petermann
 */
public class Coastlines extends Test {

    protected static final int UNORDERED_COASTLINE = 901;
    protected static final int REVERSED_COASTLINE = 902;
    protected static final int UNCONNECTED_COASTLINE = 903;
    protected static final int WRONG_ORDER_COASTLINE = 904;

    private List<Way> coastlineWays;

    /**
     * Constructor
     */
    public Coastlines() {
        super(tr("Coastlines"), tr("This test checks that coastlines are correct."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        coastlineWays = new LinkedList<>();
    }

    @Override
    public void endTest() {
        checkConnections();
        checkDirection();
        coastlineWays = null;
        super.endTest();
    }

    /**
     * Check connections between coastline ways.
     * The nodes of a coastline way have to fulfil these rules:
     * 1) the first node must be connected to the last node of a coastline way (which might be the same way)
     * 2) the last node must be connected to the first node of a coastline way (which might be the same way)
     * 3) all other nodes must not be connected to a coastline way
     * 4) the number of referencing coastline ways must be 1 or 2
     * Nodes outside the download area are special cases, we may not know enough about the connected ways.
     */
    private void checkConnections() {
        Area downloadedArea = null;
        for (Way w : coastlineWays) {
            if (downloadedArea == null) {
                downloadedArea = w.getDataSet().getDataSourceArea();
            }
            int numNodes = w.getNodesCount();
            for (int i = 0; i < numNodes; i++) {
                Node n = w.getNode(i);
                List<OsmPrimitive> refs = n.getReferrers();
                Set<Way> connectedWays = new HashSet<>();
                for (OsmPrimitive p : refs) {
                    if (p != w && isCoastline(p)) {
                        connectedWays.add((Way) p);
                    }
                }
                if (i == 0) {
                    if (connectedWays.isEmpty() && n != w.lastNode() && n.getCoor().isIn(downloadedArea)) {
                        addError(UNCONNECTED_COASTLINE, w, null, n);
                    }
                    if (connectedWays.size() == 1 && n != connectedWays.iterator().next().lastNode()) {
                        checkIfReversed(w, connectedWays.iterator().next(), n);
                    }
                    if (connectedWays.size() == 1 && w.isClosed() && connectedWays.iterator().next().isClosed()) {
                        addError(UNORDERED_COASTLINE, w, connectedWays, n);
                    }
                } else if (i == numNodes - 1) {
                    if (connectedWays.isEmpty() && n != w.firstNode() && n.getCoor().isIn(downloadedArea)) {
                        addError(UNCONNECTED_COASTLINE, w, null, n);
                    }
                    if (connectedWays.size() == 1 && n != connectedWays.iterator().next().firstNode()) {
                        checkIfReversed(w, connectedWays.iterator().next(), n);
                    }
                } else if (!connectedWays.isEmpty()) {
                    addError(UNORDERED_COASTLINE, w, connectedWays, n);
                }
            }
        }
    }

    /**
     * Check if two or more coastline ways form a closed clockwise way
     */
    private void checkDirection() {
        HashSet<Way> done = new HashSet<>();
        for (Way w : coastlineWays) {
            if (done.contains(w))
                continue;
            List<Way> visited = new ArrayList<>();
            done.add(w);
            visited.add(w);
            List<Node> nodes = new ArrayList<>(w.getNodes());
            Way curr = w;
            while (nodes.get(0) != nodes.get(nodes.size()-1)) {
                boolean foundContinuation = false;
                for (OsmPrimitive p : curr.lastNode().getReferrers()) {
                    if (p != curr && isCoastline(p)) {
                        Way other = (Way) p;
                        if (done.contains(other))
                            continue;
                        if (other.firstNode() == curr.lastNode()) {
                            foundContinuation = true;
                            curr = other;
                            done.add(curr);
                            visited.add(curr);
                            nodes.remove(nodes.size()-1); // remove duplicate connection node
                            nodes.addAll(curr.getNodes());
                            break;
                        }
                    }
                }
                if (!foundContinuation)
                    break;
            }
            // simple closed ways are reported by WronglyOrderedWays
            if (visited.size() > 1 && nodes.get(0) == nodes.get(nodes.size()-1) && Geometry.isClockwise(nodes)) {
                errors.add(TestError.builder(this, Severity.WARNING, WRONG_ORDER_COASTLINE)
                        .message(tr("Reversed coastline: land not on left side"))
                        .primitives(visited)
                        .build());
            }
        }
    }

    /**
     * Check if a reversed way would fit, if yes, add fixable "reversed" error, "unordered" else
     * @param w way that might be reversed
     * @param other other way that is connected to w
     * @param n1 node at which w and other are connected
     */
    private void checkIfReversed(Way w, Way other, Node n1) {
        boolean headFixedWithReverse = false;
        boolean tailFixedWithReverse = false;
        int otherCoastlineWays = 0;
        Way connectedToFirst = null;
        for (int i = 0; i < 2; i++) {
            Node n = (i == 0) ? w.firstNode() : w.lastNode();
            List<OsmPrimitive> refs = n.getReferrers();
            for (OsmPrimitive p : refs) {
                if (p != w && isCoastline(p)) {
                    Way cw = (Way) p;
                    if (i == 0 && cw.firstNode() == n) {
                        headFixedWithReverse = true;
                        connectedToFirst = cw;
                    } else if (i == 1 && cw.lastNode() == n) {
                        if (cw != connectedToFirst)
                            tailFixedWithReverse = true;
                    } else
                        otherCoastlineWays++;
                }
            }
        }
        if (otherCoastlineWays == 0 && headFixedWithReverse && tailFixedWithReverse)
            addError(REVERSED_COASTLINE, w, null, null);
        else
            addError(UNORDERED_COASTLINE, w, Collections.singletonList(other), n1);
    }

    /**
     * Add error if not already done
     * @param errCode the error code
     * @param w the way that is in error
     * @param otherWays collection of other ways in error or null
     * @param n the node to be highlighted or null
     */
    private void addError(int errCode, Way w, Collection<Way> otherWays, Node n) {
        String msg;
        switch (errCode) {
        case UNCONNECTED_COASTLINE:
            msg = tr("Unconnected coastline");
            break;
        case UNORDERED_COASTLINE:
            msg = tr("Unordered coastline");
            break;
        case REVERSED_COASTLINE:
            msg = tr("Reversed coastline");
            break;
        default:
            msg = tr("invalid coastline"); // should not happen
        }
        Set<OsmPrimitive> primitives = new HashSet<>();
        primitives.add(w);
        if (otherWays != null)
            primitives.addAll(otherWays);
        // check for repeated error
        for (TestError e : errors) {
            if (e.getCode() != errCode)
                continue;
            if (errCode != REVERSED_COASTLINE && !e.getHighlighted().contains(n))
                continue;
            if (e.getPrimitives().size() != primitives.size())
                continue;
            if (!e.getPrimitives().containsAll(primitives))
                continue;
            return; // we already know this error
        }
        if (errCode != REVERSED_COASTLINE)
            errors.add(TestError.builder(this, Severity.ERROR, errCode)
                    .message(msg)
                    .primitives(primitives)
                    .highlight(n)
                    .build());
        else
            errors.add(TestError.builder(this, Severity.ERROR, errCode)
                    .message(msg)
                    .primitives(primitives)
                    .build());
    }

    @Override
    public void visit(Way way) {
        if (!way.isUsable())
            return;

        if (isCoastline(way)) {
            coastlineWays.add(way);
        }
    }

    private static boolean isCoastline(OsmPrimitive osm) {
        return osm instanceof Way && osm.hasTag("natural", "coastline");
    }

    @Override
    public Command fixError(TestError testError) {
        if (isFixable(testError)) {
            // primitives list can be empty if all primitives have been purged
            Iterator<? extends OsmPrimitive> it = testError.getPrimitives().iterator();
            if (it.hasNext()) {
                Way way = (Way) it.next();
                Way newWay = new Way(way);

                List<Node> nodesCopy = newWay.getNodes();
                Collections.reverse(nodesCopy);
                newWay.setNodes(nodesCopy);

                return new ChangeCommand(way, newWay);
            }
        }
        return null;
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (testError.getTester() instanceof Coastlines)
            return testError.getCode() == REVERSED_COASTLINE;

        return false;
    }
}
