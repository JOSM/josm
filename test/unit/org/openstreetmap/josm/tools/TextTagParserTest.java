// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;

public class TextTagParserTest {
    @BeforeClass
    public static void before() {
        Main.initApplicationPreferences();
    }

    @Test
    public void testUnescape() {
        String s, s1;
        s = "\"2 3 4\"";       s1 = "2 3 4";
        Assert.assertEquals(s1, TextTagParser.unescape(s));

        s = "\"2 \\\"3\\\" 4\"";       s1 = "2 \"3\" 4";
        Assert.assertEquals(s1, TextTagParser.unescape(s));
        
        s = "\"2 3 ===4===\"";       s1 = "2 3 ===4===";
        Assert.assertEquals(s1, TextTagParser.unescape(s));

        s = "\"2 3 \\\\\\\\===4===\"";       s1 = "2 3 \\\\===4===";
        Assert.assertEquals(s1, TextTagParser.unescape(s));
    }
    
    @Test
    public void testTNformat() {
        String txt = "   a  \t  1   \n\n\n  b=2 \t the value with \"quotes\"";
        Map<String, String> correctTags = new HashMap<String, String>() { {
            put("a", "1"); put("b=2", "the value with \"quotes\"");
        }};
        Map<String, String> tags = TextTagParser.readTagsFromText(txt);
        Assert.assertEquals(correctTags, tags);
    }

    @Test
    public void testEQformat() {
        String txt = "key1=value key2=\"long value\" tag3=\"hotel \\\"Quote\\\"\"";
        Map<String, String> correctTags = new HashMap<String, String>() { {
            put("key1", "value"); put("key2", "long value");
            put("tag3", "hotel \"Quote\"");
        }};
        Map<String, String> tags = TextTagParser.readTagsFromText(txt);
        Assert.assertEquals(correctTags, tags);
    }

    @Test
    public void testJSONformat() {
        String txt;
        Map<String, String> tags, correctTags;
        txt = "{ \"a\":\"1\", \"b\":\"2 3 4\" }";
        correctTags= new HashMap<String, String>() { {  put("a", "1"); put("b", "2 3 4"); }};
        tags = TextTagParser.readTagsFromText(txt);
        Assert.assertEquals(correctTags, tags);
        
        txt = "\"a\"  :     \"1 1 1\", \"b2\"  :\"2 \\\"3 qwe\\\" 4\"";
        correctTags= new HashMap<String, String>() { { put("a", "1 1 1"); put("b2", "2 \"3 qwe\" 4");}};
        tags = TextTagParser.readTagsFromText(txt);
        Assert.assertEquals(correctTags, tags);
        
        txt = " \"aыыы\"   :    \"val\\\"\\\"\\\"ue1\"";
        correctTags= new HashMap<String, String>() { { put("aыыы", "val\"\"\"ue1");} };
        tags = TextTagParser.readTagsFromText(txt);
        Assert.assertEquals(correctTags, tags);
    }
    
    @Test
    public void testFreeformat() {
        String txt = "a 1 b=2 c=\"hello === \\\"\\\"world\"";
        Map<String, String> correctTags = new HashMap<String, String>() { {
            put("a", "1"); put("b", "2"); put("c", "hello === \"\"world");
        }};
        Map<String, String> tags = TextTagParser.readTagsFromText(txt);
        Assert.assertEquals(correctTags, tags);
    }
    
    @Test
    public void errorDetect() {
        String txt = "a=2 b=3 4";
        Map<String, String> tags = TextTagParser.readTagsFromText(txt);
        Assert.assertEquals(Collections.EMPTY_MAP, tags);
        
    }
}
