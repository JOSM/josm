// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.properties;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.junit.Ignore;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.projection.Projections;

@Ignore
public class PropertiesMergerTest extends JFrame{

    private PropertiesMerger merger;

    protected void build() {
        Main.setProjection(Projections.getProjectionByCode("EPSG:4326")); // WGS 84

        setLayout(new BorderLayout());
        add(merger = new PropertiesMerger(), BorderLayout.CENTER);
    }

    protected void populate() {
        Node my = new Node(1);
        my.setCoor(new LatLon(1, 1));
        my.setDeleted(true);

        Node their = new Node(2);
        their.setCoor(new LatLon(10, 10));

        merger.getModel().populate(new Conflict<OsmPrimitive>(my, their));
    }

    /**
     * Constructs a new {@code PropertiesMergerTest}.
     */
    public PropertiesMergerTest() {
        build();
        populate();
    }

    public static void main(String[] args) {
        PropertiesMergerTest app = new PropertiesMergerTest();
        app.setSize(600, 400);
        app.setVisible(true);
    }
}
