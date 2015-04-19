// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.TestError;

public class NameMismatchTest {

    @Before
    public void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
    }

    List<TestError> test(String primitive) {
        final NameMismatch test = new NameMismatch();
        test.check(OsmUtils.createPrimitive(primitive));
        return test.getErrors();
    }

    @Test
    public void test0() throws Exception {
        final List<TestError> errors = test("node name:de=Europa");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), is("A name is missing, even though name:* exists."));
    }

    @Test
    public void test1() throws Exception {
        final List<TestError> errors = test("node name=Europe name:de=Europa");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), is("Missing name:*=Europe. Add tag with correct language key."));
    }

    @Test
    public void test2() throws Exception {
        final List<TestError> errors = test("node name=Europe name:de=Europa name:en=Europe");
        assertThat(errors.size(), is(0));
    }

    @Test
    public void test3() throws Exception {
        List<TestError> errors;
        errors = test("node \"name\"=\"Italia - Italien - Italy\"");
        assertThat(errors.size(), is(0));
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia");
        assertThat(errors.size(), is(2));
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia name:de=Italien");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), is("Missing name:*=Italy. Add tag with correct language key."));
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia name:de=Italien name:en=Italy");
        assertThat(errors.size(), is(0));
    }
}
