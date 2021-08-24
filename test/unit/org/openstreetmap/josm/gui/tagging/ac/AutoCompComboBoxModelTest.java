// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openstreetmap.josm.data.osm.search.SearchSetting;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionPriority;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.FullPreferences;

/**
 * Test class for {@link AutoCompComboBoxModel}
 */
@FullPreferences
class AutoCompComboBoxModelTest {

    class TestData {
        public String s;
        public AutoCompletionItem ac;
        public SearchSetting ss;

        TestData(String s, AutoCompletionPriority p) {
            this.s = s;
            this.ss = new SearchSetting();
            ss.text = s;
            this.ac = new AutoCompletionItem(s, p);
        }
    }

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    // CHECKSTYLE.OFF: ParenPad

    Map<String, TestData> testData = new LinkedHashMap<String, TestData>() {{
        put("a1",   new TestData("a1",   AutoCompletionPriority.UNKNOWN));
        put("a2",   new TestData("a2",   AutoCompletionPriority.IS_IN_STANDARD));
        put("a3",   new TestData("a3",   AutoCompletionPriority.IS_IN_DATASET));
        put("a4",   new TestData("a4",   AutoCompletionPriority.IS_IN_STANDARD_AND_IN_DATASET));
        put("b",    new TestData("b",    AutoCompletionPriority.UNKNOWN));
        put("bcde", new TestData("bcde", AutoCompletionPriority.UNKNOWN));
        put("bde",  new TestData("bde",  AutoCompletionPriority.UNKNOWN));
        put("bdef", new TestData("bdef", AutoCompletionPriority.IS_IN_STANDARD_AND_IN_DATASET));
    }};

    @Test
    void testAutoCompModel() {
        assertNotNull(new AutoCompComboBoxModel<String>());
        assertNotNull(new AutoCompComboBoxModel<SearchSetting>());
        assertNotNull(new AutoCompComboBoxModel<AutoCompletionItem>());
    }

    @Test
    void testAutoCompModelFindString() {
        AutoCompComboBoxModel<String> model = new AutoCompComboBoxModel<>();
        testData.forEach((k, v) -> model.addElement(v.s));

        assertNull(model.findBestCandidate("bb"));
        assertEquals("a1",   model.findBestCandidate("a" ));
        assertEquals("b",    model.findBestCandidate("b" ));
        assertEquals("bcde", model.findBestCandidate("bc"));
        assertEquals("bde",  model.findBestCandidate("bd"));
    }

    @Test
    void testAutoCompModelFindSearchSetting() {
        AutoCompComboBoxModel<SearchSetting> model = new AutoCompComboBoxModel<>();
        // Use the default Comparator (that compares on toString).
        testData.forEach((k, v) -> model.addElement(v.ss));

        assertNull(model.findBestCandidate("bb"));
        // test for sameness (aka ==).  Some objects are expensive to copy, so we want to be able to
        // round-trip an object thru the AutoCompComboBox without copying it.
        assertSame(testData.get("a1"  ).ss, model.findBestCandidate("a" ));
        assertSame(testData.get("b"   ).ss, model.findBestCandidate("b" ));
        assertSame(testData.get("bcde").ss, model.findBestCandidate("bc"));
        assertSame(testData.get("bde" ).ss, model.findBestCandidate("bd"));
    }

    @Test
    void testAutoCompModelFindAutoCompletionItem() {
        AutoCompComboBoxModel<AutoCompletionItem> model = new AutoCompComboBoxModel<>();
        // AutoCompletionItem implements Comparable. Build a Comparator from Comparable.
        model.setComparator(Comparator.naturalOrder());
        testData.forEach((k, v) -> model.addElement(v.ac));

        assertNull(model.findBestCandidate("bb"));
        assertSame(testData.get("a4"  ).ac, model.findBestCandidate("a" )); // higher prio than "a1"
        assertSame(testData.get("b"   ).ac, model.findBestCandidate("b" ));
        assertSame(testData.get("bcde").ac, model.findBestCandidate("bc"));
        assertSame(testData.get("bdef").ac, model.findBestCandidate("bd")); // higher prio than "bde"
    }
}
