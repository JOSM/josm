// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
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
        Assert.assertEquals(s1, TextTagParser.unescape(s));

        s = "\"2 \\\"3\\\" 4\"";
        s1 = "2 \"3\" 4";
        Assert.assertEquals(s1, TextTagParser.unescape(s));

        s = "\"2 3 ===4===\"";
        s1 = "2 3 ===4===";
        Assert.assertEquals(s1, TextTagParser.unescape(s));

        s = "\"2 3 \\\\\\\\===4===\"";
        s1 = "2 3 \\\\===4===";
        Assert.assertEquals(s1, TextTagParser.unescape(s));
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
        Assert.assertEquals(correctTags, tags);
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
        Assert.assertEquals(correctTags, tags);
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
        Assert.assertEquals(correctTags, tags);

        txt = "\"a\"  :     \"1 1 1\", \"b2\"  :\"2 \\\"3 qwe\\\" 4\"";
        correctTags = new HashMap<String, String>() { { put("a", "1 1 1"); put("b2", "2 \"3 qwe\" 4"); } };
        tags = TextTagParser.readTagsFromText(txt);
        Assert.assertEquals(correctTags, tags);

        txt = " \"aыыы\"   :    \"val\\\"\\\"\\\"ue1\"";
        correctTags = new HashMap<String, String>() { { put("aыыы", "val\"\"\"ue1"); } };
        tags = TextTagParser.readTagsFromText(txt);
        Assert.assertEquals(correctTags, tags);
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
        Assert.assertEquals(correctTags, tags);
    }

    /**
     * Test of {@link TextTagParser#readTagsFromText} method (error detection).
     */
    @Test
    public void errorDetect() {
        String txt = "a=2 b=3 4";
        Map<String, String> tags = TextTagParser.readTagsFromText(txt);
        Assert.assertEquals(Collections.EMPTY_MAP, tags);
    }

    /**
     * Test of {@link TextTagParser#readTagsFromText} method with tabs.
     */
    @Test
    public void testTab() {
        Assert.assertEquals(TextTagParser.readTagsFromText("shop\tjewelry"), Collections.singletonMap("shop", "jewelry"));
        Assert.assertEquals(TextTagParser.readTagsFromText("!shop\tjewelry"), Collections.singletonMap("shop", "jewelry"));
        Assert.assertEquals(TextTagParser.readTagsFromText("!!!shop\tjewelry"), Collections.singletonMap("shop", "jewelry"));
        Assert.assertEquals(TextTagParser.readTagsFromText("shop\t\t\tjewelry"), Collections.singletonMap("shop", "jewelry"));
    }
}
