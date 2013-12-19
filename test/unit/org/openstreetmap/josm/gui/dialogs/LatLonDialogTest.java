package org.openstreetmap.josm.gui.dialogs;

import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LatLonDialogTest {
    @Test
    public void test1() throws Exception {
        assertThat(LatLonDialog.parseLatLon("49.29918° 19.24788°"), is(new LatLon(49.29918, 19.24788)));
    }

    @Test
    public void test2() throws Exception {
        assertThat(LatLonDialog.parseLatLon("N 49.29918 E 19.24788°"), is(new LatLon(49.29918, 19.24788)));
    }

    @Test
    public void test3() throws Exception {
        assertThat(LatLonDialog.parseLatLon("49.29918° 19.24788°"), is(new LatLon(49.29918, 19.24788)));
    }

    @Test
    public void test4() throws Exception {
        assertThat(LatLonDialog.parseLatLon("N 49.29918 E 19.24788"), is(new LatLon(49.29918, 19.24788)));
    }

    @Test
    public void test5() throws Exception {
        assertThat(LatLonDialog.parseLatLon("W 49°29.918' S 19°24.788'"), is(new LatLon(-19 - 24.788 / 60, -49 - 29.918 / 60)));
    }

    @Test
    public void test6() throws Exception {
        assertThat(LatLonDialog.parseLatLon("N 49°29'04\" E 19°24'43\""), is(new LatLon(49 + 29. / 60 + 04. / 3600, 19 + 24. / 60 + 43. / 3600)));
    }

    @Test
    public void test7() throws Exception {
        assertThat(LatLonDialog.parseLatLon("49.29918 N, 19.24788 E"), is(new LatLon(49.29918, 19.24788)));
    }

    @Test
    public void test8() throws Exception {
        assertThat(LatLonDialog.parseLatLon("49°29'21\" N 19°24'38\" E"), is(new LatLon(49 + 29. / 60 + 21. / 3600, 19 + 24. / 60 + 38. / 3600)));
    }

    @Test
    public void test9() throws Exception {
        assertThat(LatLonDialog.parseLatLon("49 29 51, 19 24 18"), is(new LatLon(49 + 29. / 60 + 51. / 3600, 19 + 24. / 60 + 18. / 3600)));
    }

    @Test
    public void test10() throws Exception {
        assertThat(LatLonDialog.parseLatLon("49 29, 19 24"), is(new LatLon(49 + 29. / 60, 19 + 24. / 60)));
    }

    @Test
    public void test11() throws Exception {
        assertThat(LatLonDialog.parseLatLon("E 49 29, N 19 24"), is(new LatLon(19 + 24. / 60, 49 + 29. / 60)));
    }

    @Test
    public void test12() throws Exception {
        assertThat(LatLonDialog.parseLatLon("49° 29; 19° 24"), is(new LatLon(49 + 29. / 60, 19 + 24. / 60)));
    }

    @Test
    public void test13() throws Exception {
        assertThat(LatLonDialog.parseLatLon("49° 29; 19° 24"), is(new LatLon(49 + 29. / 60, 19 + 24. / 60)));
    }

    @Test
    public void test14() throws Exception {
        assertThat(LatLonDialog.parseLatLon("N 49° 29, W 19° 24"), is(new LatLon(49 + 29. / 60, -19 - 24. / 60)));
    }

    @Test
    public void test15() throws Exception {
        assertThat(LatLonDialog.parseLatLon("49° 29.5 S, 19° 24.6 E"), is(new LatLon(-49 - 29.5 / 60, 19 + 24.6 / 60)));
    }

    @Test
    public void test16() throws Exception {
        assertThat(LatLonDialog.parseLatLon("N 49 29.918 E 19 15.88"), is(new LatLon(49 + 29.918 / 60, 19 + 15.88 / 60)));
    }

    @Test
    public void test17() throws Exception {
        assertThat(LatLonDialog.parseLatLon("49 29.4 19 24.5"), is(new LatLon(49 + 29.4 / 60, 19 + 24.5 / 60)));
    }

    @Test
    public void test18() throws Exception {
        assertThat(LatLonDialog.parseLatLon("-49 29.4 N -19 24.5 W"), is(new LatLon(-49 - 29.4 / 60, 19 + 24.5 / 60)));
    }
}
