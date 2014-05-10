// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPresetReader;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;

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
     */
    @Test
    public void testTicket9632() throws IllegalDataException, IOException {
        Collection<String> sources = new ArrayList<>();
        sources.add("resource://data/defaultpresets.xml");
        sources.add("http://josm.openstreetmap.de/josmfile?page=Presets/BicycleJunction&amp;preset");
        TaggingPresetPreference.taggingPresets = TaggingPresetReader.readAll(sources, true);

        Comparator<Relation> comparator = DefaultNameFormatter.getInstance().getRelationComparator();

        try (InputStream is = new FileInputStream(TestUtils.getTestDataRoot() + "regress/9632/data.osm.zip")) {
            DataSet ds = OsmReader.parseDataSet(Compression.ZIP.getUncompressedInputStream(is), null);
            List<Relation> relations = new ArrayList<>(ds.getRelations());
            System.out.println(Arrays.toString(relations.toArray()));
            // Check each compare possibility
            for (int i=0; i<relations.size(); i++) {
                long start = System.currentTimeMillis();
                Relation r1 = relations.get(i);
                String r1s = r1.toString();
                for (int j=i; j<relations.size(); j++) {
                    Relation r2 = relations.get(j);
                    String r2s = r2.toString();
                    int a = comparator.compare(r1, r2);
                    int b = comparator.compare(r2, r1);
                    String msg = "Compared\nr1: "+r1s+"\nr2: "+r2s+"gave: "+a+"/"+b;
                    if (i==j || a==b) {
                        assertTrue(msg, a == 0 && b == 0);
                    } else {
                        assertTrue(msg, a == -b);
                    }
                    for (int k=j; k<relations.size(); k++) {
                        Relation r3 = relations.get(k);
                        String r3s = r2.toString();
                        int c = comparator.compare(r1, r3);
                        int d = comparator.compare(r2, r3);
                        String msg2 = msg + "\nCompared\nr1: "+r1s+"\nr3: "+r3s+"gave: "+c+"Compared\nr2: "+r2s+"\nr3: "+r3s+"gave: "+d;
                        if (a > 0 && d > 0) {
                            assertTrue(msg2, c > 0);
                        } else if (a == 0 && d == 0) {
                            assertTrue(msg2, c == 0);
                        } else if (a < 0 && d < 0) {
                            assertTrue(msg2, c < 0);
                        }
                    }
                }
                long end = System.currentTimeMillis();
                System.out.println(i+"-> "+(end-start)+" ms");
            }
            // Sort relation list
            Collections.sort(relations, comparator);
        }
    }
}
