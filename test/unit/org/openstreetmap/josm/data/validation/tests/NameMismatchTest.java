// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.TestError;

public class NameMismatchTest {

    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    List<TestError> test(String primitive) {
        final NameMismatch test = new NameMismatch();
        test.check(OsmUtils.createPrimitive(primitive));
        return test.getErrors();
    }

    @Test
    public void test0() {
        final List<TestError> errors = test("node name:de=Europa");
        assertSame(errors.size(), 1);
        assertEquals(errors.get(0).getDescription(), "A name is missing, even though name:* exists.");
    }

    @Test
    public void test1() {
        final List<TestError> errors = test("node name=Europe name:de=Europa");
        assertSame(errors.size(), 1);
        assertEquals(errors.get(0).getDescription(), "Missing name:*=Europe. Add tag with correct language key.");
    }

    @Test
    public void test2() {
        final List<TestError> errors = test("node name=Europe name:de=Europa name:en=Europe");
        assertSame(errors.size(), 0);
    }

    @Test
    public void test3() {
        List<TestError> errors;
        errors = test("node \"name\"=\"Italia - Italien - Italy\"");
        assertSame(errors.size(), 0);
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia");
        assertSame(errors.size(), 2);
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia name:de=Italien");
        assertSame(errors.size(), 1);
        assertEquals(errors.get(0).getDescription(), "Missing name:*=Italy. Add tag with correct language key.");
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia name:de=Italien name:en=Italy");
        assertSame(errors.size(), 0);
    }
}
