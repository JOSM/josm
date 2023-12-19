// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.testutils.annotations.TaggingPresets;

/**
 * JUnit Test of {@link TagChecker}.
 */
@TaggingPresets
class TagCheckerTest {
    List<TestError> test(OsmPrimitive primitive) throws IOException {
        final TagChecker checker = new TagChecker() {
            @Override
            protected boolean includeOtherSeverityChecks() {
                return true;
            }
        };
        checker.initialize();
        checker.startTest(null);
        checker.check(TestUtils.addFakeDataSet(primitive));
        return checker.getErrors();
    }

    /**
     * Check for misspelled key.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testMisspelledKey1() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node Name=Main"));
        assertEquals(1, errors.size());
        assertEquals("Misspelled property key", errors.get(0).getMessage());
        assertEquals("Key 'Name' looks like 'name'.", errors.get(0).getDescription());
        assertTrue(errors.get(0).isFixable());
    }

    /**
     * Check for misspelled key.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testMisspelledKey2() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node landuse;=forest"));
        assertEquals(1, errors.size());
        assertEquals("Misspelled property key", errors.get(0).getMessage());
        assertEquals("Key 'landuse;' looks like 'landuse'.", errors.get(0).getDescription());
        assertTrue(errors.get(0).isFixable());
    }

    /**
     * Check for misspelled key where the suggested alternative is in use. The error should not be fixable.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testMisspelledKeyButAlternativeInUse() throws IOException {
        // ticket 12329
        final List<TestError> errors = test(OsmUtils.createPrimitive("node amenity=fuel brand=bah Brand=foo"));
        assertEquals(1, errors.size());
        assertEquals("Misspelled property key", errors.get(0).getMessage());
        assertEquals("Key 'Brand' looks like 'brand'.", errors.get(0).getDescription());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
        assertFalse(errors.get(0).isFixable());
    }

    /**
     * Check for misspelled key where the suggested alternative is given with prefix E: in ignoreTags.cfg.
     * The error should be fixable.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testUpperCaseIgnoredKey() throws IOException {
        // ticket 17468
        final List<TestError> errors = test(OsmUtils.createPrimitive("node wheelchair:Description=bla"));
        assertEquals(1, errors.size());
        assertEquals("Misspelled property key", errors.get(0).getMessage());
        assertEquals("Key 'wheelchair:Description' looks like 'wheelchair:description'.", errors.get(0).getDescription());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
        assertTrue(errors.get(0).isFixable());
    }

    /**
     * Check for misspelled key where the suggested alternative is given with prefix K: in ignoreTags.cfg.
     * The error should be fixable.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testUpperCaseInKeyIgnoredTag() throws IOException {
        // ticket 17468
        final List<TestError> errors = test(OsmUtils.createPrimitive("node land_Area=administrative"));
        assertEquals(1, errors.size());
        assertEquals("Misspelled property key", errors.get(0).getMessage());
        assertEquals("Key 'land_Area' looks like 'land_area'.", errors.get(0).getDescription());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
        assertTrue(errors.get(0).isFixable());
    }

    /**
     * Check for unknown key.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testTranslatedNameKey() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node namez=Baz"));
        assertEquals(1, errors.size());
        assertEquals("Presets do not contain property key", errors.get(0).getMessage());
        assertEquals("Key 'namez' not in presets.", errors.get(0).getDescription());
        assertEquals(Severity.OTHER, errors.get(0).getSeverity());
        assertFalse(errors.get(0).isFixable());
    }

    /**
     * Check for misspelled value.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testMisspelledTag() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node landuse=forrest"));
        assertEquals(1, errors.size());
        assertEquals("Unknown property value", errors.get(0).getMessage());
        assertEquals("Value 'forrest' for key 'landuse' is unknown, maybe 'forest' is meant?", errors.get(0).getDescription());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
        assertFalse(errors.get(0).isFixable());
    }

    /**
     * Check for misspelled value with multiple alternatives in presets.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testMisspelledTag2() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node highway=servics"));
        assertEquals(1, errors.size());
        assertEquals("Unknown property value", errors.get(0).getMessage());
        assertEquals(
                "Value 'servics' for key 'highway' is unknown, maybe one of [service, services] is meant?",
                errors.get(0).getDescription());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
        assertFalse(errors.get(0).isFixable());
    }

    /**
     * Check for misspelled value.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testMisspelledTag3() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node highway=residentail"));
        assertEquals(1, errors.size());
        assertEquals("Unknown property value", errors.get(0).getMessage());
        assertEquals("Value 'residentail' for key 'highway' is unknown, maybe 'residential' is meant?",
                errors.get(0).getDescription());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
        assertFalse(errors.get(0).isFixable());
    }

    /**
     * Check for misspelled value.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testShortValNotInPreset2() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node shop=abs"));
        assertEquals(1, errors.size());
        assertEquals("Presets do not contain property value", errors.get(0).getMessage());
        assertEquals("Value 'abs' for key 'shop' not in presets.", errors.get(0).getDescription());
        assertEquals(Severity.OTHER, errors.get(0).getSeverity());
        assertFalse(errors.get(0).isFixable());
    }

    /**
     * Checks that tags specifically ignored are effectively not in internal presets.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testIgnoredTagsNotInPresets() throws IOException {
        new TagChecker().initialize();
        List<String> errors = TagChecker.getIgnoredTags().stream()
                .filter(tag -> TagChecker.isTagInPresets(tag.getKey(), tag.getValue()))
                .map(Tag::toString)
                .collect(Collectors.toList());
        assertTrue(errors.isEmpty(), errors::toString);
    }

    /**
     * Check regression: Don't fix surface=u -> surface=mud.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testTooShortToFix() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node surface=u"));
        assertEquals(1, errors.size());
        assertEquals("Presets do not contain property value", errors.get(0).getMessage());
        assertEquals("Value 'u' for key 'surface' not in presets.", errors.get(0).getDescription());
        assertEquals(Severity.OTHER, errors.get(0).getSeverity());
        assertFalse(errors.get(0).isFixable());
    }

    /**
     * Check value with upper case
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testValueDifferentCase() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node highway=Residential"));
        assertEquals(1, errors.size());
        assertEquals("Unknown property value", errors.get(0).getMessage());
        assertEquals("Value 'Residential' for key 'highway' is unknown, maybe 'residential' is meant?",
                errors.get(0).getDescription());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
        assertFalse(errors.get(0).isFixable());
    }

    /**
     * Key in presets but not in ignored.cfg. Caused a NPE with r14727.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testRegression17246() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node access=privat"));
        assertEquals(1, errors.size());
        assertEquals("Unknown property value", errors.get(0).getMessage());
        assertEquals("Value 'privat' for key 'access' is unknown, maybe 'private' is meant?",
                errors.get(0).getDescription());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
        assertFalse(errors.get(0).isFixable());
    }

    /**
     * Checks for unwanted non printing control characters
     * @param s String to test
     * @param assertionC assertion on the result (true/false)
     * @param expected expected fixed value
     */
    private static void doTestUnwantedNonprintingControlCharacters(String s, Consumer<Boolean> assertionC, String expected) {
        assertionC.accept(TagChecker.containsUnwantedNonPrintingControlCharacter(s));
        assertEquals(expected, TagChecker.removeUnwantedNonPrintingControlCharacters(s));
    }

    private static void doTestUnwantedNonprintingControlCharacters(String s) {
        doTestUnwantedNonprintingControlCharacters(s, Assertions::assertTrue, "");
    }

    /**
     * Unit test of {@link TagChecker#containsUnwantedNonPrintingControlCharacter}
     *            / {@link TagChecker#removeUnwantedNonPrintingControlCharacters}
     */
    @Test
    void testContainsRemoveUnwantedNonprintingControlCharacters() {
        // Check empty string is handled
        doTestUnwantedNonprintingControlCharacters("", Assertions::assertFalse, "");
        // Check 65 ASCII control characters are removed, except new lines
        for (char c = 0x0; c < 0x20; c++) {
            if (c != '\r' && c != '\n') {
                doTestUnwantedNonprintingControlCharacters(Character.toString(c));
            } else {
                doTestUnwantedNonprintingControlCharacters(Character.toString(c), Assertions::assertFalse, Character.toString(c));
            }
        }
        doTestUnwantedNonprintingControlCharacters(Character.toString((char) 0x7F));
        // Check 7 Unicode bidi control characters are removed
        for (char c = 0x200e; c <= 0x200f; c++) {
            doTestUnwantedNonprintingControlCharacters(Character.toString(c));
        }
        for (char c = 0x202a; c <= 0x202e; c++) {
            doTestUnwantedNonprintingControlCharacters(Character.toString(c));
        }
        // Check joining characters are removed if located at the beginning or end of the string
        for (char c = 0x200c; c <= 0x200d; c++) {
            final String s = Character.toString(c);
            doTestUnwantedNonprintingControlCharacters(s);
            doTestUnwantedNonprintingControlCharacters(s + s);
            doTestUnwantedNonprintingControlCharacters(s + 'a' + s, Assertions::assertTrue, "a");
            final String ok = 'a' + s + 'b';
            doTestUnwantedNonprintingControlCharacters(ok, Assertions::assertFalse, ok);
            doTestUnwantedNonprintingControlCharacters(s + ok, Assertions::assertTrue, ok);
            doTestUnwantedNonprintingControlCharacters(ok + s, Assertions::assertTrue, ok);
            doTestUnwantedNonprintingControlCharacters(s + ok + s, Assertions::assertTrue, ok);
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17667">Bug #17667</a>.
     */
    @Test
    void testTicket17667() {
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("name", "Bus 118: Berlin, Rathaus Zehlendorf => Potsdam, Drewitz Stern-Center"));
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("name", "Καρδίτσα → Λάρισα"));
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("traffic_sign", "FI:871[← Lippuautomaatti]"));
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("traffic_sign", "FI:871[↑ Nostopaikka ↑]"));
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("name", "Cinderella II - Strandvägen ↔ Hagede"));
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("name", "Tallinn — Narva"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18322">Bug #18322</a>.
     */
    @Test
    void testTicket18322() {
        assertTrue(TagChecker.containsUnusualUnicodeCharacter("name", "D36ᴬ"));
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("ref", "D36ᴬ"));
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("old_ref", "D36ᴬ"));
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("old_ref", "D36ᵂ"));
        assertTrue(TagChecker.containsUnusualUnicodeCharacter("old_ref", "D36ᴫ"));
        assertTrue(TagChecker.containsUnusualUnicodeCharacter("old_ref", "D36ᵃ"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18449">Bug #18449</a>.
     */
    @Test
    void testTicket18449() {
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("name", "Hökumət Evi"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18740">Bug #18740</a>.
     */
    @Test
    void testTicket18740() {
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("name:ak", "Frɛnkyeman"));
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("name:bm", "Esipaɲi"));
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("name:oym", "Wɨlapaleya Ɨtu"));
    }

    /**
     * Detects objects with types not supported by their presets.
     * @throws IOException in case of I/O error
     */
    @Test
    void testObjectTypeNotSupportedByPreset() throws IOException {
        List<TestError> errors = test(OsmUtils.createPrimitive("relation waterway=river"));
        assertEquals(1, errors.size());
        assertEquals(TagChecker.INVALID_PRESETS_TYPE, errors.get(0).getCode());
        errors = test(OsmUtils.createPrimitive("relation type=waterway waterway=river"));
        assertTrue(errors.isEmpty(), errors::toString);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/19519">Bug #19519</a>.
     * @throws IOException ignored
     */
    @Test
    @Disabled("broken, see #19519")
    void testTicket19519() throws IOException {
        List<TestError> errors = test(OsmUtils.createPrimitive("node amenity=restaurant cuisine=bavarian;beef_bowl"));
        assertEquals(0, errors.size());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/20437">Bug #20437</a>.
     */
    @Test
    void testTicket20437() {
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("name:kbp", "Wasɩŋtɔŋ"));
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("name:kbp", "Kalɩfɔrnii"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/20754">Bug #20754</a>.
     */
    @Test
    void testTicket20754() {
        assertFalse(TagChecker.containsUnusualUnicodeCharacter("name", "Yuułuʔiłʔatḥ Lands"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/21348">Bug #21348</a>.
     * Key ref is in presets but without any value.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testTicket21348() throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive("node power=tower ref=12"));
        assertEquals(0, errors.size());
    }
}
