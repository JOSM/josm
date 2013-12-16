// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Checks for ways with identical consecutive nodes.
 * @since 3669
 */
public class DuplicatedWayNodes extends Test {
    protected static final int DUPLICATE_WAY_NODE = 501;

    /**
     * Constructs a new {@code DuplicatedWayNodes} test.
     */
    public DuplicatedWayNodes() {
        super(tr("Duplicated way nodes"),
                tr("Checks for ways with identical consecutive nodes."));
    }

    @Override
    public void visit(Way w) {
        if (!w.isUsable()) return;

        Node lastN = null;
        for (Node n : w.getNodes()) {
            if (lastN == null) {
                lastN = n;
                continue;
            }
            if (lastN == n) {
                errors.add(new TestError(this, Severity.ERROR, tr("Duplicated way nodes"), DUPLICATE_WAY_NODE,
                        Arrays.asList(w), Arrays.asList(n)));
                break;
            }
            lastN = n;
        }
    }

    @Override
    public Command fixError(TestError testError) {
        // primitives list can be empty if all primitives have been purged
        Iterator<? extends OsmPrimitive> it = testError.getPrimitives().iterator();
        if (it.hasNext()) {
            Way w = (Way) it.next();
            Way wnew = new Way(w);
            wnew.setNodes(null);
            Node lastN = null;
            for (Node n : w.getNodes()) {
                if (lastN == null) {
                    wnew.addNode(n);
                } else if (n == lastN) {
                    // Skip this node
                } else {
                    wnew.addNode(n);
                }
                lastN = n;
            }
            if (wnew.getNodesCount() < 2)
                // Empty way, delete
                return deletePrimitivesIfNeeded(Collections.singleton(w));
            else
                return new ChangeCommand(w, wnew);
        }
        return null;
    }

    @Override
    public boolean isFixable(TestError testError) {
        return testError.getTester() instanceof DuplicatedWayNodes;
    }
}
