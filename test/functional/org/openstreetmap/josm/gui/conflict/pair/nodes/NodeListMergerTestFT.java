// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.nodes;

import java.awt.BorderLayout;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JFrame;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

public class NodeListMergerTestFT extends JFrame {

    private NodeListMerger nodeListMerger;

    protected void populate() {
        Way w1 = new Way();
        Node n1;
        w1.addNode(n1 = new Node(1));
        for (int i = 0; i < 20; i++) {
            n1.put("key" + i, "value" + i);
        }
        // Java 11: use String.repeat
        String note = IntStream.range(0, 50).mapToObj(i -> " A very long text ").collect(Collectors.joining());
        n1.put("note", note);
        w1.addNode(new Node(2));
        w1.addNode(new Node(3));

        Way w2 = new Way();
        w2.addNode(new Node(4));
        w2.addNode(new Node(5));
        w2.addNode(new Node(6));

        nodeListMerger.populate(new Conflict<OsmPrimitive>(w1, w2));

    }

    protected void populateLong() {
        Way w1 = new Way();
        for (int i = 0; i < 100; i++) {
            w1.addNode(new Node(i));
        }

        Way w2 = new Way();
        for (int i = 1; i < 200; i += 2) {
            w2.addNode(new Node(i));
        }
        nodeListMerger.populate(new Conflict<OsmPrimitive>(w1, w2));

    }

    protected void build() {
        nodeListMerger = new NodeListMerger();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(nodeListMerger, BorderLayout.CENTER);
    }

    /**
     * Constructs a new {@code NodeListMergerTest}.
     */
    public NodeListMergerTestFT() {
        build();
        populate();
    }

    public static void main(String[] args) {
        NodeListMergerTestFT test = new NodeListMergerTestFT();
        test.setSize(600, 600);
        test.setVisible(true);
    }
}
