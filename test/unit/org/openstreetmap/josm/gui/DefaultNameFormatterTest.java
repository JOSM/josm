// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.tagging.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.TaggingPresets;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link DefaultNameFormatter} class.
 *
 */
public class DefaultNameFormatterTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/9632">#9632</a>.
     * @throws IllegalDataException
     * @throws IOException
     * @throws SAXException
     */
    @Test
    public void testTicket9632() throws IllegalDataException, IOException, SAXException {
        String source = "http://josm.openstreetmap.de/josmfile?page=Presets/BicycleJunction&amp;preset";
        TaggingPresets.addTaggingPresets(TaggingPresetReader.readAll(source, true));

        Comparator<Relation> comparator = DefaultNameFormatter.getInstance().getRelationComparator();

        try (InputStream is = new FileInputStream(TestUtils.getTestDataRoot() + "regress/9632/data.osm.zip")) {
            DataSet ds = OsmReader.parseDataSet(Compression.ZIP.getUncompressedInputStream(is), null);

            // Test with 3 known primitives causing the problem. Correct order is p1, p3, p2 with this preset
            Relation p1 = (Relation) ds.getPrimitiveById(2983382, OsmPrimitiveType.RELATION);
            Relation p2 = (Relation) ds.getPrimitiveById(550315, OsmPrimitiveType.RELATION);
            Relation p3 = (Relation) ds.getPrimitiveById(167042, OsmPrimitiveType.RELATION);

            System.out.println("p1: "+DefaultNameFormatter.getInstance().format(p1)+" - "+p1); // route_master ("Bus 453", 6 members)
            System.out.println("p2: "+DefaultNameFormatter.getInstance().format(p2)+" - "+p2); // TMC ("A 6 Kaiserslautern - Mannheim [negative]", 123 members)
            System.out.println("p3: "+DefaultNameFormatter.getInstance().format(p3)+" - "+p3); // route(lcn Sal  Salier-Radweg(412 members)

            assertTrue(comparator.compare(p1, p2) == -1); // p1 < p2
            assertTrue(comparator.compare(p2, p1) ==  1); // p2 > p1

            assertTrue(comparator.compare(p1, p3) == -1); // p1 < p3
            assertTrue(comparator.compare(p3, p1) ==  1); // p3 > p1
            assertTrue(comparator.compare(p2, p3) ==  1); // p2 > p3
            assertTrue(comparator.compare(p3, p2) == -1); // p3 < p2

            Relation[] relations = new ArrayList<>(ds.getRelations()).toArray(new Relation[0]);

            TestUtils.checkComparableContract(comparator, relations);
        }
    }
}
