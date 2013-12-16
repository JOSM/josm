// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Check coastlines for errors
 *
 * @author frsantos
 * @author Teemu Koskinen
 */
public class Coastlines extends Test {

    protected static final int UNORDERED_COASTLINE = 901;
    protected static final int REVERSED_COASTLINE = 902;
    protected static final int UNCONNECTED_COASTLINE = 903;

    private List<Way> coastlines;

    private Area downloadedArea = null;

    /**
     * Constructor
     */
    public Coastlines() {
        super(tr("Coastlines"),
                tr("This test checks that coastlines are correct."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {

        super.startTest(monitor);

        OsmDataLayer layer = Main.main.getEditLayer();

        if (layer != null) {
            downloadedArea = layer.data.getDataSourceArea();
        }

        coastlines = new LinkedList<Way>();
    }

    @Override
    public void endTest() {
        for (Way c1 : coastlines) {
            Node head = c1.firstNode();
            Node tail = c1.lastNode();

            if (c1.getNodesCount() == 0 || head.equals(tail)) {
                continue;
            }

            int headWays = 0;
            int tailWays = 0;
            boolean headReversed = false;
            boolean tailReversed = false;
            boolean headUnordered = false;
            boolean tailUnordered = false;
            Way next = null;
            Way prev = null;

            for (Way c2 : coastlines) {
                if (c1 == c2) {
                    continue;
                }

                if (c2.containsNode(head)) {
                    headWays++;
                    next = c2;

                    if (head.equals(c2.firstNode())) {
                        headReversed = true;
                    } else if (!head.equals(c2.lastNode())) {
                        headUnordered = true;
                    }
                }

                if (c2.containsNode(tail)) {
                    tailWays++;
                    prev = c2;

                    if (tail.equals(c2.lastNode())) {
                        tailReversed = true;
                    } else if (!tail.equals(c2.firstNode())) {
                        tailUnordered = true;
                    }
                }
            }

            // To avoid false positives on upload (only modified primitives
            // are visited), we have to check possible connection to ways
            // that are not in the set of validated primitives.
            if (headWays == 0) {
                Collection<OsmPrimitive> refs = head.getReferrers();
                for (OsmPrimitive ref : refs) {
                    if (ref != c1 && isCoastline(ref)) {
                        // ref cannot be in <code>coastlines</code>, otherwise we would
                        // have picked it up already
                        headWays++;
                        next = (Way) ref;

                        if (head.equals(next.firstNode())) {
                            headReversed = true;
                        } else if (!head.equals(next.lastNode())) {
                            headUnordered = true;
                        }
                    }
                }
            }
            if (tailWays == 0) {
                Collection<OsmPrimitive> refs = tail.getReferrers();
                for (OsmPrimitive ref : refs) {
                    if (ref != c1 && isCoastline(ref)) {
                        tailWays++;
                        prev = (Way) ref;

                        if (tail.equals(prev.lastNode())) {
                            tailReversed = true;
                        } else if (!tail.equals(prev.firstNode())) {
                            tailUnordered = true;
                        }
                    }
                }
            }

            List<OsmPrimitive> primitives = new ArrayList<OsmPrimitive>();
            primitives.add(c1);

            if (headWays == 0 || tailWays == 0) {
                List<OsmPrimitive> highlight = new ArrayList<OsmPrimitive>();

                if (headWays == 0 && head.getCoor().isIn(downloadedArea)) {
                    highlight.add(head);
                }
                if (tailWays == 0 && tail.getCoor().isIn(downloadedArea)) {
                    highlight.add(tail);
                }

                if (!highlight.isEmpty()) {
                    errors.add(new TestError(this, Severity.ERROR, tr("Unconnected coastline"),
                            UNCONNECTED_COASTLINE, primitives, highlight));
                }
            }

            boolean unordered = false;
            boolean reversed = headWays == 1 && headReversed && tailWays == 1 && tailReversed;

            if (headWays > 1 || tailWays > 1) {
                unordered = true;
            } else if (headUnordered || tailUnordered) {
                unordered = true;
            } else if (reversed && next == prev) {
                unordered = true;
            } else if ((headReversed || tailReversed) && headReversed != tailReversed) {
                unordered = true;
            }

            if (unordered) {
                List<OsmPrimitive> highlight = new ArrayList<OsmPrimitive>();

                if (headWays > 1 || headUnordered || headReversed || reversed) {
                    highlight.add(head);
                }
                if (tailWays > 1 || tailUnordered || tailReversed || reversed) {
                    highlight.add(tail);
                }

                errors.add(new TestError(this, Severity.ERROR, tr("Unordered coastline"),
                        UNORDERED_COASTLINE, primitives, highlight));
            }
            else if (reversed) {
                errors.add(new TestError(this, Severity.ERROR, tr("Reversed coastline"),
                        REVERSED_COASTLINE, primitives));
            }
        }

        coastlines = null;
        downloadedArea = null;

        super.endTest();
    }

    @Override
    public void visit(Way way) {
        if (!way.isUsable())
            return;

        if (isCoastline(way)) {
            coastlines.add(way);
        }
    }

    private static boolean isCoastline(OsmPrimitive osm) {
        return osm instanceof Way && "coastline".equals(osm.get("natural"));
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
            return (testError.getCode() == REVERSED_COASTLINE);

        return false;
    }
}
