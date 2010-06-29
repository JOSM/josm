// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;


public class FilterTest {
    @BeforeClass
    public static void setUp() {
        Main.proj = new Mercator();
    }
    
    @Test
    public void basic_test() throws ParseError {
        DataSet ds = new DataSet();
        Node n1 = new Node(1);
        n1.put("amenity", "parking");
        Node n2 = new Node(2);
        n2.put("fixme", "continue");
        ds.addPrimitive(n1);
        OsmPrimitive p = ds.getPrimitiveById(1,OsmPrimitiveType.NODE);
        assertNotNull(p);

        Collection<OsmPrimitive> all = new HashSet<OsmPrimitive>();
        all.addAll(Arrays.asList(new OsmPrimitive[] {n1, n2}));

        List<Filter> filters = new LinkedList<Filter>();
        Filter f1 = new Filter();
        f1.text = "fixme";
        f1.hiding = true;
        filters.addAll(Arrays.asList(new Filter[] {f1}));

        FilterMatcher filterMatcher = new FilterMatcher();
        filterMatcher.update(filters);

        FilterWorker.executeFilters(all, filterMatcher);

        assertTrue(n2.isDisabledAndHidden());
        assertTrue(!n1.isDisabled());
    }

    @Test
    public void filter_test() throws ParseError, IllegalDataException, FileNotFoundException {
        for (int i = 1; i<=3; ++i) {
            DataSet ds = OsmReader.parseDataSet(new FileInputStream("data_nodist/filterTests.osm"), NullProgressMonitor.INSTANCE);

            List<Filter> filters = new LinkedList<Filter>();
            switch (i) {
                case 1: {
                    Filter f1 = new Filter();
                    f1.text = "power";
                    f1.hiding = true;
                    filters.add(f1);
                    break;
                }
                case 2: {
                    Filter f1 = new Filter();
                    f1.text = "highway";
                    f1.inverted = true;
                    filters.add(f1);
                    break;
                }
                case 3: {
                    Filter f1 = new Filter();
                    f1.text = "power";
                    f1.inverted = true;
                    f1.hiding = true;
                    Filter f2 = new Filter();
                    f2.text = "highway";
                    filters.addAll(Arrays.asList(new Filter[] {f1, f2}));
                    break;
                }
            }

            FilterMatcher filterMatcher = new FilterMatcher();
            filterMatcher.update(filters);

            FilterWorker.executeFilters(ds.allPrimitives(), filterMatcher);

            boolean foundAtLeastOne = false;
            System.err.println("Run #"+i);
            for (OsmPrimitive osm : ds.allPrimitives()) {
                String key = "source:RESULT"+i; // use key that counts as untagged
                if (osm.hasKey(key)) {
                    foundAtLeastOne = true;
//                    System.err.println("osm "+osm.getId()+" "+filterCode(osm)+" "+osm.get(key));
                    assertEquals(String.format("Run #%d Object %s", i,osm.toString()), filterCode(osm), osm.get(key));
                }
            }
            assertTrue(foundAtLeastOne);
        }
    }

    private String filterCode(OsmPrimitive osm) {
        if (!osm.isDisabled())
            return "v";
        if (!osm.isDisabledAndHidden())
            return "d";
        return "h";
    }
}
