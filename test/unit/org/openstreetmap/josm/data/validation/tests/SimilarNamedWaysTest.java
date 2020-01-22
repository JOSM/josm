// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit test of {@link SimilarNamedWays}
 */
public class SimilarNamedWaysTest {

    private final SimilarNamedWays test = new SimilarNamedWays();

    /**
     * Setup test
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private void checkSimilarity(String message, String name1, String name2, boolean expected) {
        boolean actual = test.similaryName(name1, name2);
        assertEquals(message, expected, actual);
    }

    /**
     * Test similar names.
     */
    @Test
    public void testSimilarNames() {
        checkSimilarity("same string", "Testname", "Testname", false);
        checkSimilarity("different case", "Testname", "TestName", true);
        checkSimilarity("typo", "Testname", "Testxame", true);
        checkSimilarity("typo", "Main Street", "Maim Street", true);
        checkSimilarity("typo", "First Street", "Frist Street", true);
        checkSimilarity("missing char", "Testname", "Testame", true);
        checkSimilarity("additional char", "Testname", "Testxname", true);
        checkSimilarity("2 changes", "Testname", "Tostxname", true);
        checkSimilarity("3 changes", "Testname", "Tostxnam", false);
        checkSimilarity("many changes", "Church Street", "Water Street", false);

        // regular expression rule
        checkSimilarity("same number", "track 1", "track 1", false);
        checkSimilarity("different number", "track 1", "track 2", false);
        checkSimilarity("different number length", "track 9", "track 10", false);
        checkSimilarity("multiple numbers", "track 8 - 9", "track 10 - 11", false);
        // persian numbers, see #15869
        checkSimilarity("persian numbers", "بن‌بست نیلوفر ۵", "بن‌بست نیلوفر ۶", false);

        checkSimilarity("1st and 2nd", "1st Street", "2nd Street", false);
        checkSimilarity("1st case", "1St Street", "1st Street", true);
        checkSimilarity("1st and 2nd case", "1St Street", "2nd Street", true);
        checkSimilarity("3rd and 4th", "2rd Street", "4th Street", false);

        // synonyms
        checkSimilarity("east and west", "East Foothill Drive", "West Foothill Drive", false);
        checkSimilarity("east and west", "East Main Street", "West Main Street", false);
        checkSimilarity("east and west case", "east Foothill Drive", "West Foothill Drive", true);
        checkSimilarity("first and second", "First Street", "Second Street", false);
        checkSimilarity("first and second case", "First Street", "second Street", true);
        checkSimilarity("first and second typo", "Forst Street", "Second Street", true);
        checkSimilarity("first and second typo2", "First Street", "Socond Street", true);
        checkSimilarity("first and second 2 changes", "First Street", "Soconds Street", true);
        checkSimilarity("first and second 3 changes", "First Street", "Soconds Stret", false);
        checkSimilarity("A and B", "A Street", "B Street", false);

        // case only, see #14858
        checkSimilarity("case only", "Rua São João", "Rua Sao Joao", true);
        checkSimilarity("case only", "Rua São João", "Rua SAO JOAO", true);
        checkSimilarity("case only", "Rua Sao Joao", "Rua SAO JOAO", true);
        checkSimilarity("case only", "Rue éèçàïù", "Rue EeCAIU", true);
    }

     /**
      * Test names that previously caused a crash
      */
     @Test
     public void testSimilarNamesRegression() {
         assertFalse(test.similaryName("Unnecessary Name", "Third"));
     }
}
