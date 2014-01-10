// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.junit.Assert;
import org.junit.Test;
import org.openstreetmap.josm.Main;

import java.io.BufferedReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests of {@link Utils} class.
 */
public class UtilsTest {

    /**
     * Test of {@link Utils#strip} method.
     */
    @Test
    public void testStrip() {
        final String someWhite = 
            "\u00A0"+ // SPACE_SEPARATOR
            "\u2007"+ // LINE_SEPARATOR
            "\u202F"+ // PARAGRAPH_SEPARATOR
            "\u0009"+ // HORIZONTAL TABULATION
            "\n"    + // LINE FEED (U+000A, cannot be put as it in Java)
            "\u000B"+ // VERTICAL TABULATION
            "\u000C"+ // FORM FEED
            "\r"    + // CARRIAGE RETURN (U+000D, cannot be put as it in Java)
            "\u001C"+ // FILE SEPARATOR
            "\u001D"+ // GROUP SEPARATOR
            "\u001E"+ // RECORD SEPARATOR
            "\u001F"+ // UNIT SEPARATOR
            "\u2003"+ // EM SPACE
            "\u2007"+ // FIGURE SPACE
            "\u200B"+ // ZERO WIDTH SPACE
            "\uFEFF"+ // ZERO WIDTH NO-BREAK SPACE
            "\u3000"; // IDEOGRAPHIC SPACE
        Assert.assertNull(Utils.strip(null));
        Assert.assertEquals("", Utils.strip(""));
        Assert.assertEquals("", Utils.strip(" "));
        Assert.assertEquals("", Utils.strip("  "));
        Assert.assertEquals("", Utils.strip("   "));
        Assert.assertEquals("", Utils.strip(someWhite));
        Assert.assertEquals("a", Utils.strip("a"));
        Assert.assertEquals("ab", Utils.strip("ab"));
        Assert.assertEquals("abc", Utils.strip("abc"));
        Assert.assertEquals("a", Utils.strip(" a"));
        Assert.assertEquals("ab", Utils.strip(" ab"));
        Assert.assertEquals("abc", Utils.strip(" abc"));
        Assert.assertEquals("a", Utils.strip("a "));
        Assert.assertEquals("ab", Utils.strip("ab "));
        Assert.assertEquals("abc", Utils.strip("abc "));
        Assert.assertEquals("a", Utils.strip(someWhite+"a"+someWhite));
        Assert.assertEquals("ab", Utils.strip(someWhite+"ab"+someWhite));
        Assert.assertEquals("abc", Utils.strip(someWhite+"abc"+someWhite));
    }

    /**
     * Test of {@link Utils#toHexString} method.
     */
    @Test
    public void testToHexString(){
        Assert.assertEquals("", Utils.toHexString(null));
        Assert.assertEquals("", Utils.toHexString(new byte[0]));
        Assert.assertEquals("01", Utils.toHexString(new byte[]{0x1}));
        Assert.assertEquals("0102", Utils.toHexString(new byte[]{0x1,0x2}));
        Assert.assertEquals("12", Utils.toHexString(new byte[]{0x12}));
        Assert.assertEquals("127f", Utils.toHexString(new byte[]{0x12, 0x7f}));
        Assert.assertEquals("fedc", Utils.toHexString(new byte[]{(byte) 0xfe, (byte) 0xdc}));
    }

    @Test
    public void testOpenUrlGzip() throws Exception {
        Main.initApplicationPreferences();
        final BufferedReader x = Utils.openURLReaderAndDecompress(new URL("http://www.openstreetmap.org/trace/1613906/data"), true);
        Assert.assertTrue(x.readLine().startsWith("<?xml version="));
        x.close();
    }

    @Test
    public void testOpenUrlBzip() throws Exception {
        Main.initApplicationPreferences();
        final BufferedReader x = Utils.openURLReaderAndDecompress(new URL("http://www.openstreetmap.org/trace/785544/data"), true);
        Assert.assertTrue(x.readLine().startsWith("<?xml version="));
        x.close();
    }

    @Test
    public void testPositionListString() throws Exception {
        assertThat(Utils.getPositionListString(Arrays.asList(1)), is("1"));
        assertThat(Utils.getPositionListString(Arrays.asList(1, 2, 3)), is("1-3"));
        assertThat(Utils.getPositionListString(Arrays.asList(3, 1, 2)), is("1-3"));
        assertThat(Utils.getPositionListString(Arrays.asList(1, 2, 3, 6, 7, 8)), is("1-3,6-8"));
        assertThat(Utils.getPositionListString(Arrays.asList(1, 5, 2, 6, 7)), is("1-2,5-7"));
    }

    @Test
    public void testDurationString() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        assertThat(Utils.getDurationString(123), is("123 ms"));
        assertThat(Utils.getDurationString(1234), is("1.2 s"));
        assertThat(Utils.getDurationString(57 * 1000), is("57.0 s"));
        assertThat(Utils.getDurationString(507 * 1000), is("8 min 27 s"));
        assertThat(Utils.getDurationString((long) (8.4 * 60 * 60 * 1000)), is("8 h 24 min"));
        assertThat(Utils.getDurationString((long) (1.5 * 24 * 60 * 60 * 1000)), is("1 day 12 h"));
        assertThat(Utils.getDurationString((long) (8.5 * 24 * 60 * 60 * 1000)), is("8 days 12 h"));
    }
}
