// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * JUnit Test of "Name mismatch" validation test.
 */
class NameMismatchTest {
    List<TestError> test(String primitive) {
        final NameMismatch test = new NameMismatch();
        test.check(OsmUtils.createPrimitive(primitive));
        return test.getErrors();
    }

    /**
     * Test "A name is missing, even though name:* exists."
     */
    @Test
    void testCase0() {
        final List<TestError> errors = test("node name:de=Europa");
        assertEquals(1, errors.size());
        assertEquals("A name is missing, even though name:* exists.", errors.get(0).getMessage());
    }

    /**
     * Test "Missing name:*={0}. Add tag with correct language key."
     */
    @Test
    void testCase1() {
        final List<TestError> errors = test("node name=Europe name:de=Europa");
        assertEquals(1, errors.size());
        assertEquals("Missing name:*=Europe. Add tag with correct language key.", errors.get(0).getDescription());
    }

    /**
     * Test no error
     */
    @Test
    void testCase2() {
        final List<TestError> errors = test("node name=Europe name:de=Europa name:en=Europe");
        assertEquals(0, errors.size());
    }

    /**
     * Various other tests
     */
    @Test
    void testCase3() {
        List<TestError> errors;
        errors = test("node \"name\"=\"Italia - Italien - Italy\"");
        assertEquals(0, errors.size());
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia");
        assertEquals(2, errors.size());
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia name:de=Italien");
        assertEquals(1, errors.size());
        assertEquals("Missing name:*=Italy. Add tag with correct language key.", errors.get(0).getDescription());
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia name:de=Italien name:en=Italy");
        assertEquals(0, errors.size());
    }

    /**
     * Test that {@code name:etymology:wikidata} does not count.
     */
    @Test
    void testEtymologyWikidata() {
        final List<TestError> errors = test("node name=Foo name:etymology:wikidata=Bar");
        assertEquals(0, errors.size());
    }
}
