// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TextTagParser} class.
 */
public class TextTagParserTest {
    /**
     * Some of this depends on preferences.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test of {@link TextTagParser#unescape} method.
     */
    @Test
    public void testUnescape() {
        String s, s1;
        s = "\"2 3 4\"";
        s1 = "2 3 4";
        assertEquals(s1, TextTagParser.unescape(s));

        s = "\"2 \\\"3\\\" 4\"";
        s1 = "2 \"3\" 4";
        assertEquals(s1, TextTagParser.unescape(s));

        s = "\"2 3 ===4===\"";
        s1 = "2 3 ===4===";
        assertEquals(s1, TextTagParser.unescape(s));

        s = "\"2 3 \\\\\\\\===4===\"";
        s1 = "2 3 \\\\===4===";
        assertEquals(s1, TextTagParser.unescape(s));
    }

    /**
     * Test of {@link TextTagParser#readTagsFromText} method with tabs and new lines.
     */
    @Test
    public void testTNformat() {
        String txt = "   a  \t  1   \n\n\n  b\t2 \n c \t the value with \"quotes\"";
        Map<String, String> correctTags = new HashMap<String, String>() { {
            put("a", "1"); put("b", "2"); put("c", "the value with \"quotes\"");
        } };
        Map<String, String> tags = TextTagParser.readTagsFromText(txt);
        assertEquals(correctTags, tags);
    }

    /**
     * Test of {@link TextTagParser#readTagsFromText} method with quotes.
     */
    @Test
    public void testEQformat() {
        String txt = "key1=value key2=\"long value\" tag3=\"hotel \\\"Quote\\\"\"";
        Map<String, String> correctTags = new HashMap<String, String>() { {
            put("key1", "value"); put("key2", "long value");
            put("tag3", "hotel \"Quote\"");
        } };
        Map<String, String> tags = TextTagParser.readTagsFromText(txt);
        assertEquals(correctTags, tags);
    }

    /**
     * Test of {@link TextTagParser#readTagsFromText} method with JSON.
     */
    @Test
    public void testJSONformat() {
        String txt;
        Map<String, String> tags, correctTags;
        txt = "{ \"a\":\"1\", \"b\":\"2 3 4\" }";
        correctTags = new HashMap<String, String>() { { put("a", "1"); put("b", "2 3 4"); } };
        tags = TextTagParser.readTagsFromText(txt);
        assertEquals(correctTags, tags);

        txt = "\"a\"  :     \"1 1 1\", \"b2\"  :\"2 \\\"3 qwe\\\" 4\"";
        correctTags = new HashMap<String, String>() { { put("a", "1 1 1"); put("b2", "2 \"3 qwe\" 4"); } };
        tags = TextTagParser.readTagsFromText(txt);
        assertEquals(correctTags, tags);

        txt = " \"aыыы\"   :    \"val\\\"\\\"\\\"ue1\"";
        correctTags = new HashMap<String, String>() { { put("aыыы", "val\"\"\"ue1"); } };
        tags = TextTagParser.readTagsFromText(txt);
        assertEquals(correctTags, tags);
    }

    /**
     * Test of {@link TextTagParser#readTagsFromText} method with free format.
     */
    @Test
    public void testFreeformat() {
        String txt = "a 1 b=2 c=\"hello === \\\"\\\"world\"";
        Map<String, String> correctTags = new HashMap<String, String>() { {
            put("a", "1"); put("b", "2"); put("c", "hello === \"\"world");
        } };
        Map<String, String> tags = TextTagParser.readTagsFromText(txt);
        assertEquals(correctTags, tags);
    }

    /**
     * Test of {@link TextTagParser#readTagsFromText} method (error detection).
     */
    @Test
    public void testErrorDetect() {
        String txt = "a=2 b=3 4";
        Map<String, String> tags = TextTagParser.readTagsFromText(txt);
        assertEquals(Collections.EMPTY_MAP, tags);
    }

    /**
     * Test of {@link TextTagParser#readTagsFromText} method with tabs.
     */
    @Test
    public void testTab() {
        assertEquals(Collections.singletonMap("shop", "jewelry"), TextTagParser.readTagsFromText("shop\tjewelry"));
        assertEquals(Collections.singletonMap("shop", "jewelry"), TextTagParser.readTagsFromText("!shop\tjewelry"));
        assertEquals(Collections.singletonMap("shop", "jewelry"), TextTagParser.readTagsFromText("!!!shop\tjewelry"));
        assertEquals(Collections.singletonMap("shop", "jewelry"), TextTagParser.readTagsFromText("shop\t\t\tjewelry"));
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/16104">#16104</a>
     */
    @Test
    public void testTicket16104() {
        Map<String, String> expected = new HashMap<>();
        expected.put("boundary", "national_park");
        expected.put("name", "Raet nasjonalpark");
        expected.put("naturbase:iid", "VV00003273");
        expected.put("naturbase:verneform", "NP");
        expected.put("operator", "Raet Nasjonalparkstyre");
        expected.put("protect_class", "2");
        expected.put("related_law", "https://lovdata.no/forskrift/2016-12-16-1632");
        expected.put("short_name", "Raet");
        expected.put("start_date", "2016-12-16");
        expected.put("type", "boundary");
        expected.put("url", "http://faktaark.naturbase.no/?id=VV00003273");
        assertEquals(expected, TextTagParser.readTagsFromText(
                "boundary=national_park\n" +
                "name=Raet nasjonalpark\n" +
                "naturbase:iid=VV00003273\n" +
                "naturbase:verneform=NP\n" +
                "operator=Raet Nasjonalparkstyre\n" +
                "protect_class=2\n" +
                "related_law=https://lovdata.no/forskrift/2016-12-16-1632\n" +
                "short_name=Raet\n" +
                "start_date=2016-12-16\n" +
                "type=boundary\n" +
                "url=http://faktaark.naturbase.no/?id=VV00003273"));
    }
}
