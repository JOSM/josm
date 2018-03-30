// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of {@link TagChecker}.
 */
public class TagCheckerTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rule = new JOSMTestRules().presets();

    List<TestError> test(OsmPrimitive primitive) throws IOException {
        final TagChecker checker = new TagChecker();
        checker.initialize();
        checker.startTest(null);
        checker.check(TestUtils.addFakeDataSet(primitive));
        return checker.getErrors();
    }

    /**
     * Check for mispelled key.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testMisspelledKey1() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node Name=Main"));
        assertEquals(1, errors.size());
        assertEquals("Misspelled property key", errors.get(0).getMessage());
        assertEquals("Key 'Name' looks like 'name'.", errors.get(0).getDescription());
        assertTrue(errors.get(0).isFixable());
    }

    /**
     * Check for mispelled key.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testMisspelledKey2() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node landuse;=forest"));
        assertEquals(1, errors.size());
        assertEquals("Misspelled property key", errors.get(0).getMessage());
        assertEquals("Key 'landuse;' looks like 'landuse'.", errors.get(0).getDescription());
        assertTrue(errors.get(0).isFixable());
    }

    /**
     * Check for mispelled key where the suggested alternative is in use. The error should not be fixable.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testMisspelledKeyButAlternativeInUse() throws IOException {
        // ticket 12329
        final List<TestError> errors = test(OsmUtils.createPrimitive("node amenity=fuel brand=bah Brand=foo"));
        assertEquals(1, errors.size());
        assertEquals("Misspelled property key", errors.get(0).getMessage());
        assertEquals("Key 'Brand' looks like 'brand'.", errors.get(0).getDescription());
        assertFalse(errors.get(0).isFixable());
    }

    /**
     * Check for unknown key.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testTranslatedNameKey() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node namez=Baz"));
        assertEquals(1, errors.size());
        assertEquals("Presets do not contain property key", errors.get(0).getMessage());
        assertEquals("Key 'namez' not in presets.", errors.get(0).getDescription());
    }

    /**
     * Check for mispelled value.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testMisspelledTag() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node landuse=forrest"));
        assertEquals(1, errors.size());
        assertEquals("Presets do not contain property value", errors.get(0).getMessage());
        assertEquals("Value 'forrest' for key 'landuse' not in presets.", errors.get(0).getDescription());
    }

    /**
     * Checks that tags specifically ignored are effectively not in internal presets.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testIgnoredTagsNotInPresets() throws IOException {
        List<String> errors = new ArrayList<>();
        new TagChecker().initialize();
        for (Tag tag : TagChecker.getIgnoredTags()) {
            if (TagChecker.isTagInPresets(tag.getKey(), tag.getValue())) {
                errors.add(tag.toString());
            }
        }
        assertTrue(errors.toString(), errors.isEmpty());
    }
}
