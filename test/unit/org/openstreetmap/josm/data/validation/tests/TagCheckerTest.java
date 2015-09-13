// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.tagging.TaggingPresets;

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
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), is("Misspelled property key"));
        assertThat(errors.get(0).getDescription(), is("Key 'Name' looks like 'name'."));
    }

    @Test
    public void testMisspelledKey() throws Exception {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node landuse;=forest"));
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), is("Misspelled property key"));
        assertThat(errors.get(0).getDescription(), is("Key 'landuse;' looks like 'landuse'."));
    }

    @Test
    public void testTranslatedNameKey() throws Exception {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node namez=Baz"));
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), is("Presets do not contain property key"));
        assertThat(errors.get(0).getDescription(), is("Key 'namez' not in presets."));
    }

    @Test
    public void testMisspelledTag() throws Exception {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node landuse=forrest"));
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), is("Presets do not contain property value"));
        assertThat(errors.get(0).getDescription(), is("Value 'forrest' for key 'landuse' not in presets."));
    }

}
