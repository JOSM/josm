// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;


import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class NodesWithSameName extends Test {
    protected static int SAME_NAME = 801;

    private Map<String, List<Node>> namesToNodes;

    public NodesWithSameName() {
        super(tr("Nodes with same name"),
            tr("This test finds nodes that have the same name (might be duplicates)."));
    }

    @Override public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        namesToNodes = new HashMap<String, List<Node>>();
    }

    @Override public void visit(Node n) {
        if (!n.isUsable()) return;

        String name = n.get("name");
        String sign = n.get("traffic_sign");
        String highway = n.get("highway");
        if (name == null
            || (sign != null && sign.equals("city_limit"))
            || (highway != null && highway.equals("bus_stop"))) {
            return;
        }

        List<Node> nodes = namesToNodes.get(name);
        if (nodes == null)
            namesToNodes.put(name, nodes = new ArrayList<Node>());

        nodes.add(n);
    }

    @Override public void endTest() {
        for (List<Node> nodes : namesToNodes.values()) {
            if (nodes.size() > 1) {
                // Report the same-name nodes, unless each has a unique ref=*.
                HashSet<String> refs = new HashSet<String>();

                for (Node n : nodes) {
                    String ref = n.get("ref");
                    if (ref == null || !refs.add(ref)) {
                        errors.add(new TestError(this, Severity.OTHER,
                            tr("Nodes with same name"), SAME_NAME, nodes));
                        break;
                    }
                }
            }
        }
        super.endTest();
        namesToNodes = null;
    }
}
