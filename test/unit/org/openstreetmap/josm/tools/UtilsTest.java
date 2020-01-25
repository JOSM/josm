// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link Utils} class.
 */
public class UtilsTest {
    /**
     * Use default, basic test rules.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules();

    /**
     * Tests that {@code Utils} satisfies utility class criterias.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(Utils.class);
    }

    /**
     * Test of {@link Utils#strip} method.
     */
    @Test
    public void testStrip() {
        // CHECKSTYLE.OFF: SingleSpaceSeparator
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
        // CHECKSTYLE.ON: SingleSpaceSeparator
        assertNull(Utils.strip(null));
        assertEquals("", Utils.strip(""));
        assertEquals("", Utils.strip(" "));
        assertEquals("", Utils.strip("  "));
        assertEquals("", Utils.strip("   "));
        assertEquals("", Utils.strip(someWhite));
        assertEquals("a", Utils.strip("a"));
        assertEquals("ab", Utils.strip("ab"));
        assertEquals("abc", Utils.strip("abc"));
        assertEquals("a", Utils.strip(" a"));
        assertEquals("ab", Utils.strip(" ab"));
        assertEquals("abc", Utils.strip(" abc"));
        assertEquals("a", Utils.strip("a "));
        assertEquals("ab", Utils.strip("ab "));
        assertEquals("abc", Utils.strip("abc "));
        assertEquals("a", Utils.strip(someWhite+"a"+someWhite));
        assertEquals("ab", Utils.strip(someWhite+"ab"+someWhite));
        assertEquals("abc", Utils.strip(someWhite+"abc"+someWhite));

        // extended skip
        assertEquals("a", Utils.strip("a", "b"));
        assertEquals("b", Utils.strip("acbcac", "ac"));
    }

    /**
     * Test of {@link Utils#isStripEmpty} method.
     */
    @Test
    public void testIsStripEmpty() {
        assertTrue(Utils.isStripEmpty(null));
        assertTrue(Utils.isStripEmpty(""));
        assertTrue(Utils.isStripEmpty(" "));
        assertTrue(Utils.isStripEmpty("  "));
        assertFalse(Utils.isStripEmpty("a"));
        assertFalse(Utils.isStripEmpty("foo"));
        assertFalse(Utils.isStripEmpty(" foo"));
        assertFalse(Utils.isStripEmpty("foo "));
        assertFalse(Utils.isStripEmpty(" foo "));
    }

    /**
     * Test of {@link Utils#toHexString} method.
     */
    @Test
    public void testToHexString() {
        assertEquals("", Utils.toHexString(null));
        assertEquals("", Utils.toHexString(new byte[0]));
        assertEquals("01", Utils.toHexString(new byte[]{0x1}));
        assertEquals("0102", Utils.toHexString(new byte[]{0x1, 0x2}));
        assertEquals("12", Utils.toHexString(new byte[]{0x12}));
        assertEquals("127f", Utils.toHexString(new byte[]{0x12, 0x7f}));
        assertEquals("fedc", Utils.toHexString(new byte[]{(byte) 0xfe, (byte) 0xdc}));
    }

    /**
     * Test of {@link Utils#getPositionListString} method.
     */
    @Test
    public void testPositionListString() {
        assertEquals("1", Utils.getPositionListString(Arrays.asList(1)));
        assertEquals("1-3", Utils.getPositionListString(Arrays.asList(1, 2, 3)));
        assertEquals("1-3", Utils.getPositionListString(Arrays.asList(3, 1, 2)));
        assertEquals("1-3,6-8", Utils.getPositionListString(Arrays.asList(1, 2, 3, 6, 7, 8)));
        assertEquals("1-2,5-7", Utils.getPositionListString(Arrays.asList(1, 5, 2, 6, 7)));
    }

    /**
     * Test of {@link Utils#getDurationString} method.
     */
    @Test
    public void testDurationString() {
        I18n.set("en");
        assertEquals("123 ms", Utils.getDurationString(123));
        assertEquals("1.2 s", Utils.getDurationString(1234));
        assertEquals("57.0 s", Utils.getDurationString(57 * 1000));
        assertEquals("8 min 27 s", Utils.getDurationString(507 * 1000));
        assertEquals("8 h 24 min", Utils.getDurationString((long) (8.4 * 60 * 60 * 1000)));
        assertEquals("1 day 12 h", Utils.getDurationString((long) (1.5 * 24 * 60 * 60 * 1000)));
        assertEquals("8 days 12 h", Utils.getDurationString((long) (8.5 * 24 * 60 * 60 * 1000)));
    }

    /**
     * Test of {@link Utils#escapeReservedCharactersHTML} method.
     */
    @Test
    public void testEscapeReservedCharactersHTML() {
        assertEquals("foo -&gt; bar -&gt; '&amp;'", Utils.escapeReservedCharactersHTML("foo -> bar -> '&'"));
    }

    /**
     * Test of {@link Utils#restrictStringLines} method.
     */
    @Test
    public void testRestrictStringLines() {
        assertEquals("1\n...", Utils.restrictStringLines("1\n2\n3", 2));
        assertEquals("1\n2\n3", Utils.restrictStringLines("1\n2\n3", 3));
        assertEquals("1\n2\n3", Utils.restrictStringLines("1\n2\n3", 4));
    }

    /**
     * Test of {@link Utils#getSizeString} method.
     */
    @Test
    public void testSizeString() {
        assertEquals("0 B", Utils.getSizeString(0, Locale.ENGLISH));
        assertEquals("123 B", Utils.getSizeString(123, Locale.ENGLISH));
        assertEquals("1023 B", Utils.getSizeString(1023, Locale.ENGLISH));
        assertEquals("1.00 kB", Utils.getSizeString(1024, Locale.ENGLISH));
        assertEquals("11.7 kB", Utils.getSizeString(12024, Locale.ENGLISH));
        assertEquals("8.00 EB", Utils.getSizeString(Long.MAX_VALUE, Locale.ENGLISH));
    }

    /**
     * Test of {@link Utils#getSizeString} method.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSizeStringNegative() {
        Utils.getSizeString(-1, Locale.ENGLISH);
    }

    /**
     * Test {@link Utils#joinAsHtmlUnorderedList(Iterable)}
     */
    @Test
    public void testJoinAsHtmlUnorderedList() {
        List<? extends Object> items = Arrays.asList("1", Integer.valueOf(2));
        assertEquals("<ul><li>1</li><li>2</li></ul>", Utils.joinAsHtmlUnorderedList(items));
        assertEquals("<ul></ul>", Utils.joinAsHtmlUnorderedList(Collections.emptyList()));
    }

    /**
     * Test {@link Utils#getJavaVersion}
     */
    @Test
    public void testGetJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        try {
            System.setProperty("java.version", "1.8.0_72-ea");
            assertEquals(8, Utils.getJavaVersion());

            System.setProperty("java.version", "9-ea");
            assertEquals(9, Utils.getJavaVersion());

            System.setProperty("java.version", "9");
            assertEquals(9, Utils.getJavaVersion());

            System.setProperty("java.version", "9.0.1");
            assertEquals(9, Utils.getJavaVersion());

            System.setProperty("java.version", "10");
            assertEquals(10, Utils.getJavaVersion());

            System.setProperty("java.version", "10-ea");
            assertEquals(10, Utils.getJavaVersion());

            System.setProperty("java.version", "10.0.1");
            assertEquals(10, Utils.getJavaVersion());
        } finally {
            System.setProperty("java.version", javaVersion);
        }
    }

    /**
     * Test {@link Utils#getJavaUpdate}
     */
    @Test
    public void testGetJavaUpdate() {
        String javaVersion = System.getProperty("java.version");
        try {
            System.setProperty("java.version", "1.8.0");
            assertEquals(0, Utils.getJavaUpdate());

            System.setProperty("java.version", "1.8.0_131");
            assertEquals(131, Utils.getJavaUpdate());

            System.setProperty("java.version", "1.8.0_152-ea");
            assertEquals(152, Utils.getJavaUpdate());

            System.setProperty("java.version", "9-ea");
            assertEquals(0, Utils.getJavaUpdate());

            System.setProperty("java.version", "9");
            assertEquals(0, Utils.getJavaUpdate());

            System.setProperty("java.version", "9.1.2");
            assertEquals(1, Utils.getJavaUpdate());
        } finally {
            System.setProperty("java.version", javaVersion);
        }
    }

    /**
     * Test {@link Utils#getJavaBuild}
     */
    @Test
    public void testGetJavaBuild() {
        String javaVersion = System.getProperty("java.runtime.version");
        try {
            System.setProperty("java.runtime.version", "1.8.0_131-b11");
            assertEquals(11, Utils.getJavaBuild());

            System.setProperty("java.runtime.version", "1.8.0_152-ea-b04");
            assertEquals(4, Utils.getJavaBuild());

            System.setProperty("java.runtime.version", "9-ea+170");
            assertEquals(170, Utils.getJavaBuild());

            System.setProperty("java.runtime.version", "9+200");
            assertEquals(200, Utils.getJavaBuild());

            System.setProperty("java.runtime.version", "9.1.2+62");
            assertEquals(62, Utils.getJavaBuild());

            // IBM version example
            System.setProperty("java.runtime.version", "pwa6480sr4fp7-20170627_02 (SR4 FP7)");
            assertEquals(0, Utils.getJavaBuild());

        } finally {
            System.setProperty("java.runtime.version", javaVersion);
        }
    }

    /**
     * Tests if readBytesFromStream handles null streams (might happen when there is no data on error stream)
     * @throws IOException in case of I/O error
     */
    @Test
    public void testNullStreamForReadBytesFromStream() throws IOException {
        assertEquals("Empty on null stream", 0, Utils.readBytesFromStream(null).length);
    }

    /**
     * Test of {@link Utils#getLevenshteinDistance} method.
     */
    @Test
    public void testLevenshteinDistance() {
        assertEquals(0, Utils.getLevenshteinDistance("foo", "foo"));
        assertEquals(3, Utils.getLevenshteinDistance("foo", "bar"));
        assertEquals(1, Utils.getLevenshteinDistance("bar", "baz"));
    }

    /**
     * Test of {@link Utils#isSimilar} method.
     */
    @Test
    public void testIsSimilar() {
        assertFalse(Utils.isSimilar("foo", "foo"));
        assertFalse(Utils.isSimilar("foo", "bar"));
        assertTrue(Utils.isSimilar("bar", "baz"));
        assertTrue(Utils.isSimilar("bar", "baz"));
        assertTrue(Utils.isSimilar("Rua São João", "Rua SAO Joao"));
    }

    /**
     * Test of {@link Utils#stripHtml(String)}
     */
    @Test
    public void testStripHtml() {
        assertEquals("Hoogte 55 m", Utils.stripHtml(
                "<table width=\"100%\"><tr>" +
                        "<td align=\"left\" valign=\"center\"><small><b>Hoogte</b></small></td>" +
                        "<td align=\"center\" valign=\"center\">55 m</td></tr></table>"));
    }
}
