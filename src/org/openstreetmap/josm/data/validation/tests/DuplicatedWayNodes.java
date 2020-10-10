// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openstreetmap.josm.command.ChangeNodesCommand;
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
                errors.add(TestError.builder(this, Severity.ERROR, DUPLICATE_WAY_NODE)
                        .message(tr("Duplicated way nodes"))
                        .primitives(w)
                        .highlight(n)
                        .build());
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
            Node lastN = null;
            List<Node> modNodes = new ArrayList<>();
            for (Node n : w.getNodes()) {
                if (n != lastN) {
                    modNodes.add(n);
                }
                lastN = n;
            }
            if (modNodes.size() < 2)
                // Empty way, delete
                return deletePrimitivesIfNeeded(Collections.singleton(w));
            else
                return new ChangeNodesCommand(w, modNodes);
        }
        return null;
    }

    @Override
    public boolean isFixable(TestError testError) {
        return testError.getTester() instanceof DuplicatedWayNodes;
    }
}
