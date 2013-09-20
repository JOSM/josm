// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Checks for self-intersecting ways.
 */
public class SelfIntersectingWay extends Test {

    protected static final int SELF_INTERSECT = 401;

    /**
     * Constructs a new {@code SelfIntersectingWay} test.
     */
    public SelfIntersectingWay() {
        super(tr("Self-intersecting ways"),
                tr("This test checks for ways " +
                        "that contain some of their nodes more than once."));
    }

    @Override public void visit(Way w) {
        HashSet<Node> nodes = new HashSet<Node>();

        for (int i = 1; i < w.getNodesCount() - 1; i++) {
            Node n = w.getNode(i);
            if (nodes.contains(n)) {
                errors.add(new TestError(this,
                        Severity.WARNING, tr("Self-intersecting ways"), SELF_INTERSECT,
                        Arrays.asList(w), Arrays.asList(n)));
                break;
            } else {
                nodes.add(n);
            }
        }
    }
}
