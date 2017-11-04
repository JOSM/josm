// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Filter.FilterPreferenceEntry;
import org.openstreetmap.josm.data.osm.search.SearchMode;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link Filter}.
 */
public class FilterTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    @Test
    public void testBasic() throws SearchParseError {
        DataSet ds = new DataSet();
        Node n1 = new Node(LatLon.ZERO);
        n1.put("amenity", "parking");
        Node n2 = new Node(LatLon.ZERO);
        n2.put("fixme", "continue");
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);

        Collection<OsmPrimitive> all = new HashSet<>();
        all.addAll(Arrays.asList(new OsmPrimitive[] {n1, n2}));

        List<Filter> filters = new LinkedList<>();
        Filter f1 = new Filter();
        f1.text = "fixme";
        f1.hiding = true;
        filters.addAll(Arrays.asList(new Filter[] {f1}));

        FilterMatcher filterMatcher = new FilterMatcher();
        filterMatcher.update(filters);

        FilterWorker.executeFilters(all, filterMatcher);

        assertTrue(n2.isDisabledAndHidden());
        assertFalse(n1.isDisabled());
    }

    @Test
    public void testFilter() throws Exception {
        for (int i : new int[] {1, 2, 3, 11, 12, 13, 14, 15}) {
            DataSet ds;
            try (InputStream is = new FileInputStream("data_nodist/filterTests.osm")) {
                ds = OsmReader.parseDataSet(is, NullProgressMonitor.INSTANCE);
            }

            List<Filter> filters = new LinkedList<>();
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
            case 11: {
                Filter f1 = new Filter();
                f1.text = "highway";
                f1.inverted = true;
                f1.hiding = true;
                filters.add(f1);
                break;
            }
            case 12: {
                Filter f1 = new Filter();
                f1.text = "highway";
                f1.inverted = true;
                f1.hiding = true;
                Filter f2 = new Filter();
                f2.text = "water";
                f2.mode = SearchMode.remove;
                filters.addAll(Arrays.asList(new Filter[] {f1, f2}));
                break;
            }
            case 13: {
                Filter f1 = new Filter();
                f1.text = "highway";
                f1.inverted = true;
                f1.hiding = true;
                Filter f2 = new Filter();
                f2.text = "water";
                f2.mode = SearchMode.remove;
                Filter f3 = new Filter();
                f3.text = "natural";
                filters.addAll(Arrays.asList(new Filter[] {f1, f2, f3}));
                break;
            }
            case 14: {
                /* show all highways and all water features, but not lakes
                 * except those that have a name */
                Filter f1 = new Filter();
                f1.text = "highway";
                f1.inverted = true;
                f1.hiding = true;
                Filter f2 = new Filter();
                f2.text = "water";
                f2.mode = SearchMode.remove;
                Filter f3 = new Filter();
                f3.text = "natural";
                Filter f4 = new Filter();
                f4.text = "name";
                f4.mode = SearchMode.remove;
                filters.addAll(Arrays.asList(new Filter[] {f1, f2, f3, f4}));
                break;
            }
            case 15: {
                Filter f1 = new Filter();
                f1.text = "highway";
                f1.inverted = true;
                f1.hiding = true;
                Filter f2 = new Filter();
                f2.text = "water";
                f2.mode = SearchMode.remove;
                f2.hiding = true; // Remove only hide flag so water should stay disabled
                filters.addAll(Arrays.asList(new Filter[] {f1, f2}));
                break;
            }
            default: throw new AssertionError();
            }

            FilterMatcher filterMatcher = new FilterMatcher();
            filterMatcher.update(filters);

            FilterWorker.executeFilters(ds.allPrimitives(), filterMatcher);

            boolean foundAtLeastOne = false;
            System.err.println("Run #"+i);
            StringBuilder failedPrimitives = new StringBuilder();
            for (OsmPrimitive osm : ds.allPrimitives()) {
                String key = "source:RESULT"+i; // use key that counts as untagged
                if (osm.hasKey(key)) {
                    foundAtLeastOne = true;
                    if (!osm.get(key).equals(filterCode(osm))) {
                        failedPrimitives.append(String.format(
                                "Object %s. Expected [%s] but was [%s]%n", osm.toString(), osm.get(key), filterCode(osm)));
                    }
                }
            }
            assertTrue(foundAtLeastOne);
            if (failedPrimitives.length() != 0)
                throw new AssertionError(String.format("Run #%d%n%s", i, failedPrimitives.toString()));
        }
    }

    /**
     * Unit tests of {@link Filter.FilterPreferenceEntry} class.
     */
    @Test
    public void testFilterPreferenceEntry() {
        Filter f = new Filter();
        FilterPreferenceEntry fpe = f.getPreferenceEntry();

        assertTrue(fpe.enable);

        assertFalse(fpe.case_sensitive);
        assertFalse(fpe.hiding);
        assertFalse(fpe.inverted);
        assertFalse(fpe.mapCSS_search);
        assertFalse(fpe.regex_search);

        assertEquals("add", fpe.mode);
        assertEquals("1", fpe.version);
        assertEquals("", fpe.text);

        f.allElements = !f.allElements;
        f.caseSensitive = !f.caseSensitive;
        f.enable = !f.enable;
        f.hiding = !f.hiding;
        f.inverted = !f.inverted;
        f.mapCSSSearch = !f.mapCSSSearch;
        f.mode = SearchMode.remove;
        f.regexSearch = !f.regexSearch;
        f.text = "foo";
        fpe = f.getPreferenceEntry();

        assertFalse(fpe.enable);

        assertTrue(fpe.case_sensitive);
        assertTrue(fpe.hiding);
        assertTrue(fpe.inverted);
        assertTrue(fpe.mapCSS_search);
        assertTrue(fpe.regex_search);

        assertEquals("remove", fpe.mode);
        assertEquals("1", fpe.version);
        assertEquals("foo", fpe.text);

        assertEquals(fpe, new Filter(fpe).getPreferenceEntry());
    }

    /**
     * Unit test of methods {@link FilterPreferenceEntry#equals} and {@link FilterPreferenceEntry#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(FilterPreferenceEntry.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }

    private static String filterCode(OsmPrimitive osm) {
        if (!osm.isDisabled())
            return "v";
        if (!osm.isDisabledAndHidden())
            return "d";
        return "h";
    }
}
