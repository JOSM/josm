// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test class for {@link VectorNode}
 * @author Taylor Smock
 * @since xxx
 */
class VectorNodeTest {
    @RegisterExtension
    JOSMTestRules rule = new JOSMTestRules().projection();

    @Test
    void testLatLon() {
        VectorNode node = new VectorNode("test");
        assertTrue(Double.isNaN(node.lat()));
        assertTrue(Double.isNaN(node.lon()));
        LatLon testLatLon = new LatLon(50, -40);
        node.setCoor(testLatLon);
        assertEquals(50, node.lat());
        assertEquals(-40, node.lon());
        assertEquals(testLatLon, node.getCoor());
    }

    @Test
    void testSetEastNorth() {
        VectorNode node = new VectorNode("test");
        LatLon latLon = new LatLon(-1, 5);
        EastNorth eastNorth = ProjectionRegistry.getProjection().latlon2eastNorth(latLon);
        node.setEastNorth(eastNorth);
        assertEquals(-1, node.lat(), 0.0000000001);
        assertEquals(5, node.lon(), 0.0000000001);
    }

    @Test
    void testICoordinate() {
        VectorNode node = new VectorNode("test");
        assertTrue(Double.isNaN(node.lat()));
        assertTrue(Double.isNaN(node.lon()));
        ICoordinate coord = new ICoordinate() {
            @Override
            public double getLat() {
                return 5;
            }

            @Override
            public void setLat(double lat) {
                // No op
            }

            @Override
            public double getLon() {
                return -1;
            }

            @Override
            public void setLon(double lon) {
                // no op
            }
        };
        node.setCoor(coord);
        assertEquals(5, node.lat());
        assertEquals(-1, node.lon());
    }

    @Test
    void testUniqueIdGenerator() {
        VectorNode node1 = new VectorNode("test");
        VectorNode node2 = new VectorNode("test2");
        assertSame(node1.getIdGenerator(), node2.getIdGenerator());
        assertNotNull(node1.getIdGenerator());
    }

    @Test
    void testNode() {
        assertEquals(OsmPrimitiveType.NODE, new VectorNode("test").getType());
    }

    @Test
    void testBBox() {
        VectorNode node = new VectorNode("test");
        node.setCoor(new LatLon(5, -1));
        assertTrue(node.getBBox().bboxIsFunctionallyEqual(new BBox(-1, 5), 0d));
    }

    @Test
    void testVisitor() {
        List<VectorNode> visited = new ArrayList<>();
        VectorNode node = new VectorNode("test");
        node.accept(new PrimitiveVisitor() {
            @Override
            public void visit(INode n) {
                visited.add((VectorNode) n);
            }

            @Override
            public void visit(IWay<?> w) {
                fail("Way should not have been visited");
            }

            @Override
            public void visit(IRelation<?> r) {
                fail("Relation should not have been visited");
            }
        });

        assertEquals(1, visited.size());
        assertSame(node, visited.get(0));
    }

    @Test
    void testIsReferredToByWays() {
        VectorWay way = new VectorWay("test");
        VectorNode node = new VectorNode("test");
        assertFalse(node.isReferredByWays(1));
        assertTrue(node.getReferrers(true).isEmpty());
        way.setNodes(Collections.singletonList(node));
        assertEquals(1, node.getReferrers(true).size());
        assertSame(way, node.getReferrers(true).get(0));
        // No dataset yet
        assertFalse(node.isReferredByWays(1));
        VectorDataSet dataSet = new VectorDataSet();
        dataSet.addPrimitive(way);
        dataSet.addPrimitive(node);
        assertTrue(node.isReferredByWays(1));
        assertFalse(node.isReferredByWays(2));
    }
}
