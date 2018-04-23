// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Geometry} class.
 */
public class GeometryTest {
    /**
     * Primitives need preferences and projection.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().projection();

    /**
     * Test of {@link Geometry#getLineLineIntersection} method.
     */
    @Test
    public void testLineLineIntersection() {
        EastNorth p1 = new EastNorth(-9477809.106349014, 1.5392960539974203E7);
        EastNorth p2 = new EastNorth(-9477813.789091509, 1.5392954297092048E7);
        EastNorth p3 = new EastNorth(-9477804.974058038, 1.539295490030348E7);
        EastNorth p4 = new EastNorth(-9477814.628697459, 1.5392962142181376E7);

        EastNorth intersectionPoint = Geometry.getLineLineIntersection(p1, p2, p3, p4);

        EastNorth d1 = p3.subtract(intersectionPoint);
        EastNorth d2 = p1.subtract(p2);
        Double crossProduct = d1.east()*d2.north() - d1.north()*d2.east();
        Double scalarProduct = d1.east()*d2.east() + d1.north()*d2.north();
        Double len1 = d1.length();
        Double len2 = d2.length();

        Double angle1 = Geometry.getCornerAngle(p1, p2, intersectionPoint);
        Double angle2 = Geometry.getCornerAngle(p3, p4, intersectionPoint);
        Assert.assertTrue("intersection point not on line, angle: " + angle1,
                Math.abs(angle1) < 1e-10);
        Assert.assertTrue("intersection point not on line, angle: " + angle2,
                Math.abs(angle1) < 1e-10);

        Assert.assertTrue("cross product != 1 : " + Math.abs(crossProduct/len1/len2),
                Math.abs(Math.abs(crossProduct/len1/len2) - 1) < 1e-10);
        Assert.assertTrue("scalar product != 0 : " + scalarProduct/len1/len2,
                Math.abs(scalarProduct/len1/len2) < 1e-10);
    }

    /**
     * Test of {@link Geometry#closedWayArea(org.openstreetmap.josm.data.osm.Way)} method.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testClosedWayArea() throws Exception {
        try (FileInputStream in = new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm")) {
            DataSet ds = OsmReader.parseDataSet(in, null);
            Way closedWay = (Way) SubclassFilteredCollection.filter(ds.allPrimitives(),
                    SearchCompiler.compile("landuse=forest")).iterator().next();
            Assert.assertEquals(5760015.7353515625, Geometry.closedWayArea(closedWay), 1e-3);
            Assert.assertEquals(5760015.7353515625, Geometry.computeArea(closedWay), 1e-3);
        }
    }

    /**
     * Test of {@link Geometry#multipolygonArea(Relation)}} method.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testMultipolygonArea() throws Exception {
        try (FileInputStream in = new FileInputStream(TestUtils.getTestDataRoot() + "multipolygon.osm")) {
            DataSet ds = OsmReader.parseDataSet(in, null);
            final Relation r = ds.getRelations().iterator().next();
            Assert.assertEquals(4401735.20703125, Geometry.multipolygonArea(r), 1e-3);
            Assert.assertEquals(4401735.20703125, Geometry.computeArea(r), 1e-3);
        }
    }

    /**
     * Test of {@link Geometry#getAreaAndPerimeter(List)} method.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testAreaAndPerimeter() throws Exception {
        try (FileInputStream in = new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm")) {
            DataSet ds = OsmReader.parseDataSet(in, null);
            Way closedWay = (Way) SubclassFilteredCollection.filter(ds.allPrimitives(),
                    SearchCompiler.compile("landuse=forest")).iterator().next();
            Geometry.AreaAndPerimeter areaAndPerimeter = Geometry.getAreaAndPerimeter(closedWay.getNodes());
            Assert.assertEquals(12495000., areaAndPerimeter.getArea(), 1e-3);
            Assert.assertEquals(15093.201209424187, areaAndPerimeter.getPerimeter(), 1e-3);
        }
    }

    /**
     * Test of {@link Geometry#getNormalizedAngleInDegrees(double)} method.
     */
    @Test
    public void testRightAngle() {
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        n1.setCoor(new LatLon(10.22873540462851, 6.169719398316592));
        n2.setCoor(new LatLon(10.229332494162811, 6.16978130985785));
        n3.setCoor(new LatLon(10.22924937004949, 6.17060908367496));

        double angle = Geometry.getNormalizedAngleInDegrees(Geometry.getCornerAngle(n1.getEastNorth(),
                n2.getEastNorth(), n3.getEastNorth()));
        assertEquals(90, angle, 1e-8);
        angle = Geometry.getNormalizedAngleInDegrees(Geometry.getCornerAngle(n1.getEastNorth(),
                n2.getEastNorth(), n1.getEastNorth()));
        assertEquals(0, angle, 1e-8);

        n1.setCoor(new LatLon(10.2295011, 6.1693106));
        n2.setCoor(new LatLon(10.2294958, 6.16930635));
        n3.setCoor(new LatLon(10.2294895, 6.1693039));

        angle = Geometry.getNormalizedAngleInDegrees(Geometry.getCornerAngle(n1.getEastNorth(),
                n2.getEastNorth(), n3.getEastNorth()));
        assertEquals(162.66381817961337, angle, 1e-5);

        angle = Geometry.getNormalizedAngleInDegrees(Geometry.getCornerAngle(n3.getEastNorth(),
                n2.getEastNorth(), n1.getEastNorth()));
        assertEquals(162.66381817961337, angle, 1e-5);
    }
}
