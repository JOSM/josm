package org.openstreetmap.josm.data.validation.tests;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class OpeningHourTestTest {

    private static final OpeningHourTest OPENING_HOUR_TEST = new OpeningHourTest();

    @Before
    public void setUp() throws Exception {
        Main.pref = new Preferences();
        OPENING_HOUR_TEST.initialize();
    }

    @Test
    public void testCheckOpeningHourSyntax1() throws Exception {
        // frequently used tags according to http://taginfo.openstreetmap.org/keys/opening_hours#values
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("24/7").isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("Mo-Fr 08:30-20:00").isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("09:00-21:00").isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("Su-Th sunset-24:00, 04:00-sunrise; Fr-Sa sunset-sunrise").isEmpty(), is(true));
    }

    @Test
    public void testCheckOpeningHourSyntax2() throws Exception {
        final List<TestError> errors = OPENING_HOUR_TEST.checkOpeningHourSyntax("Mo-Tue");
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0).getMessage(), is("Mo-Tue <--- (Please use the abbreviation \"Tu\" for \"tue\".)"));
        assertThat(errors.get(0).getSeverity(), is(Severity.WARNING));
    }

    @Test
    public void testCheckOpeningHourSyntax3() throws Exception {
        final List<TestError> errors = OPENING_HOUR_TEST.checkOpeningHourSyntax("Sa-Su 10.00-20.00");
        assertThat(errors.size(), is(2));
        assertThat(errors.get(0).getMessage(), is("Sa-Su 10. <--- (Please use \":\" as hour/minute-separator)"));
        assertThat(errors.get(0).getSeverity(), is(Severity.WARNING));
        assertThat(errors.get(1).getMessage(), is("Sa-Su 10.00-20. <--- (Please use \":\" as hour/minute-separator)"));
        assertThat(errors.get(1).getSeverity(), is(Severity.WARNING));
    }

    @Test
    public void testCheckOpeningHourSyntax4() throws Exception {
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(null).isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("").isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax(" ").isEmpty(), is(true));
    }

    @Test
    public void testCheckOpeningHourSyntax5() throws Exception {
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("badtext").size(), is(1));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("badtext").get(0).getMessage(),
                is("opening_hours - ba <--- (Unexpected token: \"b\" This means that the syntax is not valid at that point or it is currently not supported.)"));
    }

    @Test
    public void testCheckOpeningHourSyntax6() throws Exception {
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("PH open \"always open on public holidays\"").isEmpty(), is(true));
    }

    @Test
    public void testCheckServiceTimeSyntax1() throws Exception {
        // frequently used tags according to http://taginfo.openstreetmap.org/keys/service_times#values
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("Su 10:00", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("automatic", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(false));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("Mo-Sa 09:00-18:00", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("Su 09:30; We 19:30", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("Mo-Fr 0:00-0:30,4:00-00:30; Sa,Su,PH 0:00-24:00", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
    }

    @Test
    public void testCheckCollectionTimeSyntax1() throws Exception {
        // frequently used tags according to http://taginfo.openstreetmap.org/keys/collection_times#values
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("Mo-Sa 09:00", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("fixme", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(false));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("daily", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(false));
        assertThat(OPENING_HOUR_TEST.checkOpeningHourSyntax("Mo-Fr 13:30, 17:45, 19:00; Sa 15:00; Su 11:00", OpeningHourTest.CheckMode.BOTH).isEmpty(), is(true));
    }
}
