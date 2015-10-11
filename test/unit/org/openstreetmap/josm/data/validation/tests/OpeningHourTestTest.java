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
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems;
import org.openstreetmap.josm.gui.tagging.TaggingPresetReader;

/**
 * JUnit Test of "Opening hours" validation test.
 */
public class OpeningHourTestTest {

    private static final OpeningHourTest OPENING_HOUR_TEST = new OpeningHourTest();

    /**
     * Setup test.
     * @throws Exception if test cannot be initialized
     */
    @Before
    public void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
        OPENING_HOUR_TEST.initialize();
    }

    /**
     * Test #1 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax1() {
        final String key = "opening_hours";
        // frequently used tags according to https://taginfo.openstreetmap.org/keys/opening_hours#values
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "24/7"), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 08:30-20:00"), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr sunrise-sunset"), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "09:00-21:00"), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Su-Th sunset-24:00,04:00-sunrise; Fr-Sa sunset-sunrise"), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Su-Th sunset-24:00, 04:00-sunrise; Fr-Sa sunset-sunrise"), hasSize(1));
        assertEquals(Severity.OTHER, OPENING_HOUR_TEST.checkOpeningHourSyntax(
                key, "Su-Th sunset-24:00, 04:00-sunrise; Fr-Sa sunset-sunrise").get(0).getSeverity());
        assertEquals("Su-Th sunset-24:00,04:00-sunrise; Fr-Sa sunset-sunrise", OPENING_HOUR_TEST.checkOpeningHourSyntax(
                key, "Su-Th sunset-24:00, 04:00-sunrise; Fr-Sa sunset-sunrise").get(0).getPrettifiedValue());
    }

    @Test
    public void testI18n() {
        assertTrue(OPENING_HOUR_TEST.checkOpeningHourSyntax("opening_hours", ".", OpeningHourTest.CheckMode.POINTS_IN_TIME, false, "de")
                .get(0).toString().contains("Unerwartetes Zeichen"));
        assertFalse(OPENING_HOUR_TEST.checkOpeningHourSyntax("opening_hours", ".", OpeningHourTest.CheckMode.POINTS_IN_TIME, false, "en")
                .get(0).toString().contains("Unerwartetes Zeichen"));
    }

    /**
     * Test #2 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax2() {
        final String key = "opening_hours";
        final List<OpeningHourTest.OpeningHoursTestError> errors = OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Tue");
        assertThat(errors, hasSize(2));
        assertEquals(key + " - Mo-Tue <--- (Please use the abbreviation \"Tu\" for \"tue\".)", errors.get(0).getMessage());
        assertEquals(Severity.WARNING, errors.get(0).getSeverity());
        assertEquals(key +
                " - Mo-Tue <--- (This rule is not very explicit because there is no time selector being used."+
                " Please add a time selector to this rule or use a comment to make it more explicit.)", errors.get(1).getMessage());
        assertEquals(Severity.WARNING, errors.get(1).getSeverity());
    }

    /**
     * Test #3 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax3() {
        final String key = "opening_hours";
        final List<OpeningHourTest.OpeningHoursTestError> errors = OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Sa-Su 10.00-20.00");
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
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(null, null), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(null, ""), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(null, " "), isEmpty());
    }

    /**
     * Test #5 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax5() {
        final String key = "opening_hours";
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "badtext"), hasSize(1));
        assertEquals(key + " - ba <--- (Unexpected token: \"b\" Invalid/unsupported syntax.)",
                OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "badtext").get(0).getMessage());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "5.00 p.m-11.00 p.m"), hasSize(1));
        assertEquals(key + " - 5.00 p <--- (hyphen (-) or open end (+) in time range expected. "
                + "For working with points in time, the mode for opening_hours.js has to be altered. Maybe wrong tag?)",
                OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "5.00 p.m-11.00 p.m").get(0).getMessage());
    }

    /**
     * Test #6 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax6() {
        final String key = "opening_hours";
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "PH open \"always open on public holidays\""), isEmpty());
    }

    /**
     * Test #7 of opening_hours syntax.
     */
    @Test
    public void testCheckOpeningHourSyntax7() {
        final String key = "opening_hours";
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "9:00-18:00"), hasSize(1));
        assertEquals(Severity.OTHER, OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "9:00-18:00").get(0).getSeverity());
        assertEquals("09:00-18:00", OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "9:00-18:00").get(0).getPrettifiedValue());
    }

    /**
     * Non-regression Test of opening_hours syntax for bug #9367.
     */
    @Test
    public void testCheckOpeningHourSyntaxTicket9367() {
        final String key = "opening_hours";
        assertEquals(Severity.WARNING, OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo,Tu 04-17").get(0).getSeverity());
        assertEquals(key + " - Mo,Tu 04-17 <--- (Time range without minutes specified. "
                + "Not very explicit! Please use this syntax instead \"04:00-17:00\".)",
                OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo,Tu 04-17").get(0).getMessage());
        assertEquals("Mo,Tu 04:00-17:00", OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo,Tu 04-17").get(0).getPrettifiedValue());
    }

    /**
     * Test #1 of service_times syntax.
     */
    @Test
    public void testCheckServiceTimeSyntax1() {
        final String key = "service_times";
        // frequently used tags according to https://taginfo.openstreetmap.org/keys/service_times#values
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Su 10:00", OpeningHourTest.CheckMode.BOTH), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "automatic", OpeningHourTest.CheckMode.BOTH), not(isEmpty()));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Sa 09:00-18:00", OpeningHourTest.CheckMode.BOTH), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Su 09:30; We 19:30", OpeningHourTest.CheckMode.BOTH), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 00:00-00:30,04:00-00:30; Sa,Su,PH 00:00-24:00",
                OpeningHourTest.CheckMode.BOTH), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 0:00-0:30,4:00-00:30; Sa,Su,PH 0:00-24:00",
                OpeningHourTest.CheckMode.BOTH), hasSize(1));
        assertEquals("Mo-Fr 00:00-00:30,04:00-00:30; Sa,Su,PH 00:00-24:00",
                OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 0:00-0:30,4:00-00:30; Sa,Su,PH 0:00-24:00",
                OpeningHourTest.CheckMode.BOTH).get(0).getPrettifiedValue());
        assertEquals("Mo-Fr 00:00-00:30,04:00-00:30; Sa,Su,PH 00:00-24:00",
                OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 0:00-0:30,4:00-00:30; Sa,Su,PH 0:00-24:00",
                OpeningHourTest.CheckMode.BOTH).get(0).getPrettifiedValue());
    }

    /**
     * Test #1 of collection_times syntax.
     */
    @Test
    public void testCheckCollectionTimeSyntax1() {
        final String key = "collection_times";
        // frequently used tags according to https://taginfo.openstreetmap.org/keys/collection_times#values
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Sa 09:00", OpeningHourTest.CheckMode.BOTH), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "fixme", OpeningHourTest.CheckMode.BOTH), not(isEmpty()));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "daily", OpeningHourTest.CheckMode.BOTH), not(isEmpty()));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 13:30,17:45,19:00; Sa 15:00; Su 11:00",
                OpeningHourTest.CheckMode.BOTH), isEmpty());
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 13:30, 17:45, 19:00; Sa 15:00; Su 11:00",
                OpeningHourTest.CheckMode.BOTH), hasSize(1));
        assertEquals(Severity.OTHER,
                OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 13:30, 17:45, 19:00; Sa 15:00; Su 11:00",
                OpeningHourTest.CheckMode.BOTH).get(0).getSeverity());
        assertEquals("Mo-Fr 13:30,17:45,19:00; Sa 15:00; Su 11:00",
                OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 13:30, 17:45, 19:00; Sa 15:00; Su 11:00",
                OpeningHourTest.CheckMode.BOTH).get(0).getPrettifiedValue());
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
                if (i instanceof TaggingPresetItems.KeyedItem &&
                        Arrays.asList("opening_hours", "service_times", "collection_times").contains(((TaggingPresetItems.KeyedItem) i).key)) {
                    for (final String v : ((TaggingPresetItems.KeyedItem) i).getValues()) {
                        values.add(new Tag(((TaggingPresetItems.KeyedItem) i).key, v));
                    }
                }
            }
        }
        for (final Tag t : values) {
            final List<OpeningHourTest.OpeningHoursTestError> errors = OPENING_HOUR_TEST.checkOpeningHourSyntax(t.getKey(), t.getValue());
            assertThat(t + " is valid", errors, isEmpty());
        }
    }
}
