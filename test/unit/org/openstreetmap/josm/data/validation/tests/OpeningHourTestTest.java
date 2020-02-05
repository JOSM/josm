// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.CustomMatchers.hasSize;
import static org.CustomMatchers.isEmpty;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of "Opening hours" validation test.
 * @see OpeningHourTest
 */
public class OpeningHourTestTest {
    /**
     * We need prefs for this. We check strings so we need i18n.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().i18n();

    private OpeningHourTest openingHourTest;

    /**
     * Setup test.
     * @throws Exception if test cannot be initialized
     */
    @Before
    public void setUp() throws Exception {
        openingHourTest = new OpeningHourTest();
        openingHourTest.initialize();
    }

    /**
     * Test #1 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax1() {
        final String key = "opening_hours";
        // frequently used tags according to https://taginfo.openstreetmap.org/keys/opening_hours#values
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "24/7"), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Mo-Fr 08:30-20:00"), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Mo-Fr sunrise-sunset"), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "09:00-21:00"), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Su-Th sunset-24:00,04:00-sunrise; Fr-Sa sunset-sunrise"), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Su-Th sunset-24:00, 04:00-sunrise; Fr-Sa sunset-sunrise"), hasSize(1));
        assertEquals(Severity.OTHER, openingHourTest.checkOpeningHourSyntax(
                key, "Su-Th sunset-24:00, 04:00-sunrise; Fr-Sa sunset-sunrise").get(0).getSeverity());
        assertEquals("Su-Th sunset-24:00,04:00-sunrise; Fr-Sa sunset-sunrise", openingHourTest.checkOpeningHourSyntax(
                key, "Su-Th sunset-24:00, 04:00-sunrise; Fr-Sa sunset-sunrise").get(0).getPrettifiedValue());
    }

    /**
     * Test translated messages.
     */
    @Test
    public void testI18n() {
        assertTrue(openingHourTest.checkOpeningHourSyntax("opening_hours", ".", null, false, "de")
                .get(0).toString().contains("Unerwartetes Zeichen"));
        assertFalse(openingHourTest.checkOpeningHourSyntax("opening_hours", ".", null, false, "en")
                .get(0).toString().contains("Unerwartetes Zeichen"));
    }

    /**
     * Test #2 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax2() {
        final String key = "opening_hours";
        final List<OpeningHourTest.OpeningHoursTestError> errors = openingHourTest.checkOpeningHourSyntax(key, "Mo-Tue");
        assertThat(errors, hasSize(2));
        assertEquals(key + " - Mo-Tue <--- (Please use the English abbreviation \"Tu\" for \"tue\".)", errors.get(0).getMessage());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
        assertEquals(key +
                " - Mo-Tue <--- (This rule is not very explicit because there is no time selector being used."+
                " A time selector is the part specifying hours when the object is opened, for example \"10:00-19:00\"."+
                " Please add a time selector to this rule or use a comment to make it more explicit.)", errors.get(1).getMessage());
        assertEquals(Severity.WARNING, errors.get(1).getSeverity());
    }

    /**
     * Test #3 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax3() {
        final String key = "opening_hours";
        final List<OpeningHourTest.OpeningHoursTestError> errors = openingHourTest.checkOpeningHourSyntax(key, "Sa-Su 10.00-20.00");
        assertThat(errors, hasSize(2));
        assertEquals(key + " - Sa-Su 10. <--- (Please use \":\" as hour/minute-separator)", errors.get(0).getMessage());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
        assertEquals("Sa-Su 10:00-20:00", errors.get(0).getPrettifiedValue());
        assertEquals(key + " - Sa-Su 10.00-20. <--- (Please use \":\" as hour/minute-separator)", errors.get(1).getMessage());
        assertEquals(Severity.WARNING, errors.get(1).getSeverity());
    }

    /**
     * Test #4 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax4() {
        assertThat(openingHourTest.checkOpeningHourSyntax(null, null), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(null, ""), isEmpty());
        assertEquals("opening_hours - The value contains nothing meaningful which can be parsed.",
                openingHourTest.checkOpeningHourSyntax("opening_hours", " ").get(0).getMessage());
        assertEquals("null - The optional_conf_parm[\"tag_key\"] parameter is of unknown type. Given object, expected string.",
                openingHourTest.checkOpeningHourSyntax(null, " ").get(0).getMessage());
    }

    /**
     * Test #5 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax5() {
        final String key = "opening_hours";
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "badtext"), hasSize(1));
        assertEquals(key + " - ba <--- (Unexpected token: \"b\" Invalid/unsupported syntax.)",
                openingHourTest.checkOpeningHourSyntax(key, "badtext").get(0).getMessage());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "5.00 p.m-11.00 p.m"), hasSize(1));
        assertEquals(key + " - 5.00 p <--- (hyphen (-) or open end (+) in time range expected. "
                + "For working with points in time, the mode for opening_hours.js has to be altered. Maybe wrong tag?)",
                openingHourTest.checkOpeningHourSyntax(key, "5.00 p.m-11.00 p.m").get(0).getMessage());
    }

    /**
     * Test #6 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax6() {
        final String key = "opening_hours";
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "PH open \"always open on public holidays\""), isEmpty());
    }

    /**
     * Test #7 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax7() {
        final String key = "opening_hours";
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "9:00-18:00"), hasSize(1));
        assertEquals(Severity.OTHER, openingHourTest.checkOpeningHourSyntax(key, "9:00-18:00").get(0).getSeverity());
        assertEquals("09:00-18:00", openingHourTest.checkOpeningHourSyntax(key, "9:00-18:00").get(0).getPrettifiedValue());
    }

    /**
     * Non-regression Test of opening_hours syntax for bug #9367.
     */
    @Test
    public void testCheckOpeningHourSyntaxTicket9367() {
        final String key = "opening_hours";
        assertEquals(Severity.WARNING, openingHourTest.checkOpeningHourSyntax(key, "Mo,Tu 04-17").get(0).getSeverity());
        assertEquals(key + " - Mo,Tu 04-17 <--- (Time range without minutes specified. "
                + "Not very explicit! Please use this syntax instead \"04:00-17:00\".)",
                openingHourTest.checkOpeningHourSyntax(key, "Mo,Tu 04-17").get(0).getMessage());
        assertEquals("Mo,Tu 04:00-17:00", openingHourTest.checkOpeningHourSyntax(key, "Mo,Tu 04-17").get(0).getPrettifiedValue());
    }

    /**
     * Test #1 of service_times syntax.
     */
    @Test
    public void testCheckServiceTimeSyntax1() {
        final String key = "service_times";
        // frequently used tags according to https://taginfo.openstreetmap.org/keys/service_times#values
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Su 10:00"), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "automatic"), not(isEmpty()));
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Mo-Sa 09:00-18:00"), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Su 09:30; We 19:30"), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Mo-Fr 00:00-00:30,04:00-00:30; Sa,Su,PH 00:00-24:00"), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Mo-Fr 0:00-0:30,4:00-00:30; Sa,Su,PH 0:00-24:00"), hasSize(1));
        assertEquals("Mo-Fr 00:00-00:30,04:00-00:30; Sa,Su,PH 00:00-24:00",
                openingHourTest.checkOpeningHourSyntax(key, "Mo-Fr 0:00-0:30,4:00-00:30; Sa,Su,PH 0:00-24:00").get(0).getPrettifiedValue());
        assertEquals("Mo-Fr 00:00-00:30,04:00-00:30; Sa,Su,PH 00:00-24:00",
                openingHourTest.checkOpeningHourSyntax(key, "Mo-Fr 0:00-0:30,4:00-00:30; Sa,Su,PH 0:00-24:00").get(0).getPrettifiedValue());
    }

    /**
     * Test #1 of collection_times syntax.
     */
    @Test
    public void testCheckCollectionTimeSyntax1() {
        final String key = "collection_times";
        // frequently used tags according to https://taginfo.openstreetmap.org/keys/collection_times#values
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Mo-Sa 09:00"), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "fixme"), not(isEmpty()));
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "daily"), not(isEmpty()));
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Mo-Fr 13:30,17:45,19:00; Sa 15:00; Su 11:00"), isEmpty());
        assertThat(openingHourTest.checkOpeningHourSyntax(key, "Mo-Fr 13:30, 17:45, 19:00; Sa 15:00; Su 11:00"), hasSize(1));
        assertEquals(Severity.OTHER,
                openingHourTest.checkOpeningHourSyntax(key, "Mo-Fr 13:30, 17:45, 19:00; Sa 15:00; Su 11:00").get(0).getSeverity());
        assertEquals("Mo-Fr 13:30,17:45,19:00; Sa 15:00; Su 11:00",
                openingHourTest.checkOpeningHourSyntax(key, "Mo-Fr 13:30, 17:45, 19:00; Sa 15:00; Su 11:00").get(0).getPrettifiedValue());
    }

    /**
     * Tests that predefined values in presets are correct.
     */
    @Test
    public void testPresetValues() {
        final Collection<TaggingPreset> presets = TaggingPresetReader.readFromPreferences(false, false);
        final Set<Tag> values = new LinkedHashSet<>();
        for (final TaggingPreset p : presets) {
            for (final TaggingPresetItem i : p.data) {
                if (i instanceof KeyedItem &&
                        Arrays.asList("opening_hours", "service_times", "collection_times").contains(((KeyedItem) i).key)) {
                    for (final String v : ((KeyedItem) i).getValues()) {
                        values.add(new Tag(((KeyedItem) i).key, v));
                    }
                }
            }
        }
        for (final Tag t : values) {
            final List<OpeningHourTest.OpeningHoursTestError> errors = openingHourTest.checkOpeningHourSyntax(t.getKey(), t.getValue());
            assertThat(t + " is valid", errors, isEmpty());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17932">Bug #17932</a>.
     */
    @Test
    public void testTicket17932() {
        Logging.clearLastErrorAndWarnings();
        assertTrue(openingHourTest.checkOpeningHourSyntax("opening_hours", "SH off").isEmpty());
    }
}
