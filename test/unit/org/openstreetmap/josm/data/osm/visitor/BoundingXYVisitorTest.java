// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link BoundingXYVisitor}.
 */
@BasicPreferences
@Projection
public class BoundingXYVisitorTest {

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/24982">#24982</a>
     */
    @Test
     public void testTicket24982() {
         DataSet ds = new DataSet();
         // two nodes with coordinates as in the example data
         Node n1 = new Node(new LatLon(53.1637636, 7.8315384));
         Node n2 = new Node(new LatLon(53.1729062, 7.7530235));
         ds.addPrimitive(n1);
         ds.addPrimitive(n2);

         BoundingXYVisitor v = new BoundingXYVisitor();
         ds.setSelected(ds.getNodes());
         assertEquals(2, ds.getSelectedNodes().size());
         ds.getSelectedNodes().forEach(v::visit);
         ProjectionBounds boundsBeforeZoom = v.getBounds();
         v.enlargeBoundingBoxLogarithmically();
         ProjectionBounds boundsAfterZoom = v.getBounds();
         // check that the new bounding box isn't smaller than the old one
         assertTrue(boundsAfterZoom.contains(boundsBeforeZoom.getMin()));
         assertTrue(boundsAfterZoom.contains(boundsBeforeZoom.getMax()));
         // check that the new bounding box contains both nodes
         assertTrue(boundsAfterZoom.contains(n1.getEastNorth()));
         assertTrue(boundsAfterZoom.contains(n2.getEastNorth()));
     }

}
