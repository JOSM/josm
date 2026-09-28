// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.I18n;
import org.openstreetmap.josm.testutils.annotations.TaggingPresets;

/**
 * JUnit Test of {@link TagChecker}.
 */
@I18n
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
        if (primitive.getDataSet() == null) {
            TestUtils.addFakeDataSet(primitive);
        }
        checker.check(primitive);
        return checker.getErrors();
    }

    static Stream<Arguments> testTags() {
        final String misspelled = "Misspelled property key";
        final String unknown = "Unknown property value";
        final String noPropertyValue = "Presets do not contain property value";
        final String looksLike = "Key ''{0}'' looks like ''{1}''.";
        final String invalidPreset = "Key from a preset is invalid in this region";
        return Stream.of(
                // Check for misspelled key.
                Arguments.of("testMisspelledKey1", "node Name=Main", misspelled,
                        MessageFormat.format(looksLike, "Name", "name"), Severity.WARNING, true),
                // Check for misspelled key.
                Arguments.of("testMisspelledKey2", "node landuse;=forest", misspelled,
                        MessageFormat.format(looksLike, "landuse;", "landuse"), Severity.WARNING, true),
                // Check for misspelled key where the suggested alternative is in use. The error should not be fixable.
                Arguments.of("testMisspelledKeyButAlternativeInUse", "node amenity=fuel brand=bah Brand=foo", misspelled,
                        MessageFormat.format(looksLike, "Brand", "brand"), Severity.WARNING, false),
                // Check for misspelled key where the suggested alternative is given with prefix E: in ignoreTags.cfg.
                // The error should be fixable.
                // ticket 17468
                Arguments.of("testUpperCaseIgnoredKey", "node wheelchair:Description=bla", misspelled,
                        MessageFormat.format(looksLike, "wheelchair:Description", "wheelchair:description"), Severity.WARNING, true),
                // Check for misspelled key where the suggested alternative is given with prefix K: in ignoreTags.cfg.
                // The error should be fixable.
                // ticket 17468
                Arguments.of("testUpperCaseInKeyIgnoredTag", "node land_Area=administrative", misspelled,
                        MessageFormat.format(looksLike, "land_Area", "land_area"), Severity.WARNING, true),
                // Check for unknown key
                Arguments.of("testTranslatedNameKey", "node namez=Baz",
                        "Presets do not contain property key", "Key 'namez' not in presets.", Severity.OTHER, false),
                // Check for misspelled value
                Arguments.of("testMisspelledTag", "node landuse=forrest", unknown,
                        "Value 'forrest' for key 'landuse' is unknown, maybe 'forest' is meant?", Severity.WARNING, false),
                // Check for misspelled value with multiple alternatives in presets.
                Arguments.of("testMisspelledTag2", "node highway=servics", unknown,
                        "Value 'servics' for key 'highway' is unknown, maybe one of [service, services] is meant?", Severity.WARNING, false),
                // Check for misspelled value.
                Arguments.of("testMisspelledTag3", "node highway=residentail", unknown,
                        "Value 'residentail' for key 'highway' is unknown, maybe 'residential' is meant?", Severity.WARNING, false),
                // Check for misspelled value.
                Arguments.of("testShortValNotInPreset2", "node shop=abs", noPropertyValue,
                        "Value 'abs' for key 'shop' not in presets.", Severity.OTHER, false),
                // Check regression: Don't fix surface=u -> surface=mud.
                Arguments.of("testTooShortToFix", "node surface=u", noPropertyValue,
                        "Value 'u' for key 'surface' not in presets.", Severity.OTHER, false),
                // Check value with upper case
                Arguments.of("testValueDifferentCase", "node highway=Residential", unknown,
                        "Value 'Residential' for key 'highway' is unknown, maybe 'residential' is meant?", Severity.WARNING, false),
                Arguments.of("testRegionKey", "node payment:ep_minipay=yes", invalidPreset,
                        "Preset Payment Methods should not have the key payment:ep_minipay", Severity.WARNING, false),
                Arguments.of("testRegionTag", "relation type=waterway gnis:feature_id=123456", invalidPreset,
                        "Preset Waterway should not have the key gnis:feature_id", Severity.WARNING, false),
                // Key in presets but not in ignored.cfg. Caused a NPE with r14727.
                Arguments.of("testRegression17246", "node access=privat", unknown,
                        "Value 'privat' for key 'access' is unknown, maybe 'private' is meant?", Severity.WARNING, false)
        );
    }

    /**
     * Test tags
     * @param name The name of the test
     * @param primitive The primitive definition to use
     * @param expectedMessage The expected error message
     * @param expectedDescription The expected error description
     * @param severity The expected severity
     * @param isFixable {@code true} if the error should be fixable
     * @throws IOException See {@link #test(OsmPrimitive)}
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("testTags")
    void testTags(String name, String primitive, String expectedMessage, String expectedDescription, Severity severity, boolean isFixable)
            throws IOException {
        final List<TestError> errors = test(OsmUtils.createPrimitive(primitive));
        assertEquals(1, errors.size());
        assertEquals(expectedMessage, errors.get(0).getMessage());
        assertEquals(expectedDescription, errors.get(0).getDescription());
        assertEquals(severity, errors.get(0).getSeverity());
        assertEquals(isFixable, errors.get(0).isFixable());
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

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/23290">Bug #23290</a>
     */
    @Test
    void testTicket23290() throws IOException {
        final Node france1 = new Node(new LatLon(48.465638, 7.3677049));
        final Node france2 = new Node(new LatLon(48.4191768, 7.7275072));
        final Node german1 = new Node(new LatLon(48.3398223, 8.1683322));
        final Node german2 = new Node(new LatLon(48.4137076, 7.754287));
        final Way frenchRiver = TestUtils.newWay("waterway=river", france1, france2);
        final Way germanRiver = TestUtils.newWay("waterway=river", german1, german2);
        final Way incompleteWay = new Way(123, 0);
        final Relation riverRelation = TestUtils.newRelation("type=waterway waterway=river ref:sandre=A---0000 ref:fgkz=2",
                new RelationMember("", germanRiver));
        // Ensure they have the same dataset
        new DataSet(france1, france2, frenchRiver, german1, german2, germanRiver, incompleteWay, riverRelation);
        assertEquals(1, test(riverRelation).size());
        riverRelation.addMember(new RelationMember("", frenchRiver));
        assertEquals(0, test(riverRelation).size());
        riverRelation.removeMembersFor(frenchRiver);
        assertEquals(1, test(riverRelation).size());
        riverRelation.addMember(new RelationMember("", incompleteWay));
        assertEquals(0, test(riverRelation).size());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/23860">Bug #23860</a>.
     * Duplicate key
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testTicket23860Equal() throws IOException {
        ValidatorPrefHelper.PREF_OTHER.put(true);
        Config.getPref().putBoolean(TagChecker.PREF_CHECK_PRESETS_TYPES, true);
        final TaggingPreset originalBusStop = org.openstreetmap.josm.gui.tagging.presets.TaggingPresets.getMatchingPresets(
                Collections.singleton(TaggingPresetType.NODE), Collections.singletonMap("highway", "bus_stop"), false)
                .iterator().next();
        final Key duplicateKey = new Key();
        duplicateKey.key = "highway";
        duplicateKey.value = "bus_stop";
        try {
            originalBusStop.data.add(duplicateKey);
            final List<TestError> errors = test(OsmUtils.createPrimitive("way highway=bus_stop"));
            assertEquals(1, errors.size());
        } finally {
            originalBusStop.data.remove(duplicateKey);
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/23860">Bug #23860</a>.
     * Duplicate key
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testTicket23860NonEqual() throws IOException {
        ValidatorPrefHelper.PREF_OTHER.put(true);
        Config.getPref().putBoolean(TagChecker.PREF_CHECK_PRESETS_TYPES, true);
        final TaggingPreset originalBusStop = org.openstreetmap.josm.gui.tagging.presets.TaggingPresets.getMatchingPresets(
                        Collections.singleton(TaggingPresetType.NODE), Collections.singletonMap("highway", "bus_stop"), false)
                .iterator().next();
        final Key duplicateKey = new Key();
        duplicateKey.key = "highway";
        duplicateKey.value = "bus_stop2";
        try {
            originalBusStop.data.add(duplicateKey);
            final List<TestError> errors = test(OsmUtils.createPrimitive("way highway=bus_stop"));
            assertEquals(0, errors.size());
        } finally {
            originalBusStop.data.remove(duplicateKey);
        }
    }
}
