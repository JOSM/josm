// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;

/**
 * JUnit Test of {@link TagChecker}.
 */
public class TagCheckerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
        TaggingPresets.readFromPreferences();
    }

    List<TestError> test(OsmPrimitive primitive) throws IOException {
        final TagChecker checker = new TagChecker();
        checker.initialize();
        checker.startTest(null);
        checker.check(primitive);
        return checker.getErrors();
    }

    @Test
    public void testInvalidKey() throws Exception {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node Name=Main"));
        assertEquals(1, errors.size());
        assertEquals("Misspelled property key", errors.get(0).getMessage());
        assertEquals("Key 'Name' looks like 'name'.", errors.get(0).getDescription());
    }

    @Test
    public void testMisspelledKey() throws Exception {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node landuse;=forest"));
        assertEquals(1, errors.size());
        assertEquals("Misspelled property key", errors.get(0).getMessage());
        assertEquals("Key 'landuse;' looks like 'landuse'.", errors.get(0).getDescription());
    }

    @Test
    public void testTranslatedNameKey() throws Exception {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node namez=Baz"));
        assertEquals(1, errors.size());
        assertEquals("Presets do not contain property key", errors.get(0).getMessage());
        assertEquals("Key 'namez' not in presets.", errors.get(0).getDescription());
    }

    @Test
    public void testMisspelledTag() throws Exception {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node landuse=forrest"));
        assertEquals(1, errors.size());
        assertEquals("Presets do not contain property value", errors.get(0).getMessage());
        assertEquals("Value 'forrest' for key 'landuse' not in presets.", errors.get(0).getDescription());
    }
}
