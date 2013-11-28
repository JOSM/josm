// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.validation.Severity;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * JUnit Test of "Opening hours" validation test. 
 */
public class OpeningHourTestTest {

    private static final OpeningHourTest OPENING_HOUR_TEST = new OpeningHourTest();

    @Before
    public void setUp() throws Exception {
        Main.pref = new Preferences();
        OPENING_HOUR_TEST.initialize();
    }

    @Test
    public void testCheckOpeningHourSyntax1() throws Exception {
        final String key = "opening_hours";
        // frequently used tags according to http://taginfo.openstreetmap.org/keys/opening_hours#values
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "24/7").isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 08:30-20:00").isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "09:00-21:00").isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Su-Th sunset-24:00, 04:00-sunrise; Fr-Sa sunset-sunrise").isEmpty(), is(true));
    }

    @Test
    public void testCheckOpeningHourSyntax2() throws Exception {
        final String key = "opening_hours";
        final List<OpeningHourTest.OpeningHoursTestError> errors = OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Tue");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), is("Mo-Tue <--- (Please use the abbreviation \"Tu\" for \"tue\".)"));
        assertThat(errors.get(0).getSeverity(), is(Severity.WARNING));
    }

    @Test
    public void testCheckOpeningHourSyntax3() throws Exception {
        final String key = "opening_hours";
        final List<OpeningHourTest.OpeningHoursTestError> errors = OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Sa-Su 10.00-20.00");
        assertThat(errors.size(), is(2));
        assertThat(errors.get(0).getMessage(), is("Sa-Su 10. <--- (Please use \":\" as hour/minute-separator)"));
        assertThat(errors.get(0).getSeverity(), is(Severity.WARNING));
        assertThat(errors.get(0).getPrettifiedValue(), is("Sa-Su 10:00-20:00"));
        assertThat(errors.get(1).getMessage(), is("Sa-Su 10.00-20. <--- (Please use \":\" as hour/minute-separator)"));
        assertThat(errors.get(1).getSeverity(), is(Severity.WARNING));
    }

    @Test
    public void testCheckOpeningHourSyntax4() throws Exception {
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(null, null).isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(null, "").isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(null, " ").isEmpty(), is(true));
    }

    @Test
    public void testCheckOpeningHourSyntax5() throws Exception {
        final String key = "opening_hours";
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "badtext").size(), is(1));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "badtext").get(0).getMessage(),
                is("opening_hours - ba <--- (Unexpected token: \"b\" This means that the syntax is not valid at that point or it is currently not supported.)"));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "5.00 p.m-11.00 p.m").size(), is(1));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "5.00 p.m-11.00 p.m").get(0).getMessage(),
                is("opening_hours - 5.00 p <--- (hyphen (-) or open end (+) in time range expected. For working with points in time, the mode for opening_hours.js has to be altered. Maybe wrong tag?)"));
    }

    @Test
    public void testCheckOpeningHourSyntax6() throws Exception {
        final String key = "opening_hours";
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "PH open \"always open on public holidays\"").isEmpty(), is(true));
    }

    @Test
    public void testCheckOpeningHourSyntaxTicket9367() throws Exception {
        final String key = "opening_hours";
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo,Tu 04-17").get(0).getSeverity(), is(Severity.WARNING));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo,Tu 04-17").get(0).getMessage(), is("Mo,Tu 04-17 <--- (Time range without minutes specified. Not very explicit! Please use this syntax instead e.g. \"12:00-14:00\".)"));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo,Tu 04-17").get(0).getPrettifiedValue(), is("Mo,Tu 04:00-17:00"));
    }

    @Test
    public void testCheckServiceTimeSyntax1() throws Exception {
        final String key = "service_times";
        // frequently used tags according to http://taginfo.openstreetmap.org/keys/service_times#values
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Su 10:00", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "automatic", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(false));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Sa 09:00-18:00", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Su 09:30; We 19:30", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 0:00-0:30,4:00-00:30; Sa,Su,PH 0:00-24:00", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
    }

    @Test
    public void testCheckCollectionTimeSyntax1() throws Exception {
        final String key = "collection_times";
        // frequently used tags according to http://taginfo.openstreetmap.org/keys/collection_times#values
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Sa 09:00", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "fixme", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(false));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "daily", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(false));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(key, "Mo-Fr 13:30, 17:45, 19:00; Sa 15:00; Su 11:00", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
    }
}
