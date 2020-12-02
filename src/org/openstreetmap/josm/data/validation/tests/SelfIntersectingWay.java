// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.tests.CrossingWays.SelfCrossing;

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

    @Override
    public void visit(Way w) {
        int last = w.getNodesCount();
        if (last < 2)
            return;
        Set<Node> nodes = new HashSet<>();
        nodes.add(w.firstNode());
        int countFirst = 0;
        int countLast = 0;
        for (int i = 1; i < last; i++) {
            Node n = w.getNode(i);
            if (nodes.contains(n)) {
                boolean ok = false;
                if (n == w.firstNode()) {
                    if (countFirst++ == 0)
                        ok = true;
                } else if (i + 1 == last) {
                    if (countLast++ == 0)
                        ok = true;
                }
                if (!ok || countFirst + countLast > 1) {
                    errors.add(TestError.builder(this, Severity.WARNING, SELF_INTERSECT)
                            .message(tr("Self-intersecting ways"))
                            .primitives(w)
                            .highlight(n)
                            .build());
                    break;
                }
            } else {
                nodes.add(n);
            }
        }
    }

    /**
     * Check if the given way is self-intersecting
     * @param way the way to check
     * @return {@code true} if way contains some nodes more than once
     * @see SelfCrossing
     * @since 17386
     */
    public static boolean isSelfIntersecting(Way way) {
        SelfIntersectingWay test = new SelfIntersectingWay();
        test.visit(way);
        return !test.errors.isEmpty();
    }
}
