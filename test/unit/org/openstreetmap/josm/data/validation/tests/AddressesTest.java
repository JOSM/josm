// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openstreetmap.josm.data.coor.LatLon.NORTH_POLE;
import static org.openstreetmap.josm.data.coor.LatLon.SOUTH_POLE;
import static org.openstreetmap.josm.data.coor.LatLon.ZERO;

import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;

/**
 * JUnit Test of {@link Addresses} validation test.
 */
class AddressesTest {
    private static Node createAddressNode(String nodeTags, String wayTags, String relationTags) {
        DataSet ds = new DataSet();
        Node n = TestUtils.newNode(nodeTags);
        ds.addPrimitive(n);
        if (wayTags != null) {
            ds.addPrimitive(TestUtils.newWay(wayTags, n));
        }
        if (relationTags != null) {
            ds.addPrimitive(TestUtils.newRelation(relationTags, new RelationMember(null, n)));
        }
        return n;
    }

    private static TestError doTestHouseNumberWithoutStreet(String nodeTags, String wayTags, String relationTags) {
        return new Addresses().checkHouseNumbersWithoutStreet(createAddressNode(nodeTags, wayTags, relationTags));
    }

    /**
     * Unit test of {@link Addresses#HOUSE_NUMBER_WITHOUT_STREET}
     */
    @Test
    void testHouseNumberWithoutStreet() {
        assertNull(doTestHouseNumberWithoutStreet("", null, null));
        assertNotNull(doTestHouseNumberWithoutStreet("addr:housenumber=1", null, null));
        assertNull(doTestHouseNumberWithoutStreet("addr:housenumber=1 addr:street=Foo", null, null));
        assertNull(doTestHouseNumberWithoutStreet("addr:housenumber=1 addr:place=Foo", null, null));
        assertNull(doTestHouseNumberWithoutStreet("addr:housenumber=1 addr:neighbourhood=Foo", null, null));
        assertNotNull(doTestHouseNumberWithoutStreet("addr:housenumber=1", null, "type=enforcement"));
        assertNull(doTestHouseNumberWithoutStreet("addr:housenumber=1", null, "type=associatedStreet"));
        assertNotNull(doTestHouseNumberWithoutStreet("addr:housenumber=1", "building=yes", null));
        assertNull(
                doTestHouseNumberWithoutStreet("addr:housenumber=1", "addr:interpolation=odd addr:street=Foo", null));
    }

    private static void doTestDuplicateHouseNumber(String tags1, LatLon ll1, String tags2, LatLon ll2,
            Severity expected) {
        DataSet ds = new DataSet();
        Node n1 = TestUtils.newNode(tags1);
        n1.setCoor(ll1);
        ds.addPrimitive(n1);
        Node n2 = TestUtils.newNode(tags2);
        n2.setCoor(ll2);
        ds.addPrimitive(n2);
        List<TestError> errors = new Addresses().checkForDuplicate(n2);
        assertEquals(expected != null ? 1 : 0, errors.size());
        if (expected != null) {
            assertEquals(expected, errors.get(0).getSeverity());
        }
    }

    /**
     * Unit test of {@link Addresses#DUPLICATE_HOUSE_NUMBER}
     */
    @Test
    void testDuplicateHouseNumber() {
        String num1 = "addr:housenumber=1 addr:street=Foo ";
        String num2 = "addr:housenumber=2 addr:street=Foo ";
        String city1 = "addr:city=Gotham ";
        String city2 = "addr:city=Metropolis ";
        String suburb1 = "addr:suburb=Queens ";
        String suburb2 = "addr:suburb=Bronx ";
        // Warning for same addresses at close distance
        doTestDuplicateHouseNumber(num1, ZERO, num1, ZERO, Severity.WARNING);
        // Info for same addresses at long distance
        doTestDuplicateHouseNumber(num1, SOUTH_POLE, num1, NORTH_POLE, Severity.OTHER);
        // Nothing for different addresses
        doTestDuplicateHouseNumber(num1, ZERO, num2, ZERO, null);
        // Info for same address in different cities, warning if same city
        doTestDuplicateHouseNumber(num1 + city1, ZERO, num1 + city2, ZERO, Severity.OTHER);
        doTestDuplicateHouseNumber(num1 + city1, ZERO, num1 + city1, ZERO, Severity.WARNING);
        // Info for same address in same city but different suburbs, warning if same
        // suburb
        doTestDuplicateHouseNumber(num1 + city1 + suburb1, ZERO, num1 + city1 + suburb2, ZERO, Severity.OTHER);
        doTestDuplicateHouseNumber(num1 + city1 + suburb1, ZERO, num1 + city1 + suburb1, ZERO, Severity.WARNING);
    }

    /**
     * Unit test of {@link Addresses#expandHouseNumber}
     */
    @Test
    void testMultiAddressDuplicates() {
        String num1 = "addr:housenumber=1,3 addr:street=Foo";
        String num2 = "addr:housenumber=1 addr:street=Foo";
        String num3 = "addr:housenumber=3 addr:street=Foo";
        String num4 = "addr:housenumber=4 addr:street=Foo";

        doTestDuplicateHouseNumber(num1, ZERO, num2, ZERO, Severity.WARNING);
        doTestDuplicateHouseNumber(num1, ZERO, num3, ZERO, Severity.WARNING);
        doTestDuplicateHouseNumber(num1, ZERO, num4, ZERO, null);

        num1 = num1.replace(",", ";");

        doTestDuplicateHouseNumber(num1, ZERO, num2, ZERO, Severity.WARNING);
        doTestDuplicateHouseNumber(num1, ZERO, num3, ZERO, Severity.WARNING);
        doTestDuplicateHouseNumber(num1, ZERO, num4, ZERO, null);
    }

    /**
     * See #23302
     */
    @Test
    void testCheckForDuplicatePOIBuildingAddresses() {
        final Addresses test = new Addresses();
        final JPanel panel = new JPanel();
        final Node poi = TestUtils.newNode("addr:housenumber=1 addr:street=Foo");
        final Way building = TestUtils.newWay("addr:housenumber=1 addr:street=Foo building=yes",
                TestUtils.newNode(""), TestUtils.newNode(""), TestUtils.newNode(""));
        final DataSet ds = new DataSet();
        // Ensure that we are checking for building-poi duplicates
        test.addGui(panel);
        JCheckBox checkboxIncludeBldgPOI = assertInstanceOf(JCheckBox.class, panel.getComponent(panel.getComponentCount() - 1));
        checkboxIncludeBldgPOI.setSelected(true);
        test.ok();
        // Set up the dataset
        ds.addPrimitive(poi);
        ds.addPrimitiveRecursive(building);
        building.addNode(building.firstNode());

        // Duplicate addresses with no additional information should always have warnings
        test.startTest(NullProgressMonitor.INSTANCE);
        test.visit(ds.allPrimitives());
        test.endTest();
        assertEquals(1, test.getErrors().size());

        // Do the first test checking for building-poi duplicates
        poi.put("name", "FooBar");
        test.startTest(NullProgressMonitor.INSTANCE);
        test.visit(ds.allPrimitives());
        test.endTest();
        assertEquals(1, test.getErrors().size());
        assertEquals(Severity.OTHER, test.getErrors().get(0).getSeverity());

        // Now check if they have the same name
        building.put("name", "FooBar");
        test.startTest(NullProgressMonitor.INSTANCE);
        test.visit(ds.allPrimitives());
        test.endTest();
        assertEquals(1, test.getErrors().size());
        assertEquals(Severity.WARNING, test.getErrors().get(0).getSeverity());

        // Now check if they have a different name
        building.put("name", "FooBar2");
        test.startTest(NullProgressMonitor.INSTANCE);
        test.visit(ds.allPrimitives());
        test.endTest();
        assertEquals(1, test.getErrors().size());
        assertEquals(Severity.OTHER, test.getErrors().get(0).getSeverity());

        // Now ensure that it doesn't get errors when disabled
        checkboxIncludeBldgPOI.setSelected(false);
        test.ok();
        test.startTest(NullProgressMonitor.INSTANCE);
        test.visit(ds.allPrimitives());
        test.endTest();
        assertTrue(test.getErrors().isEmpty());
    }
}
