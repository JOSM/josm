// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This test tests the {@link Predicate}s created by the {@link Predicates} class.
 *
 * @author Michael Zangl
 */
public class PredicatesTest {
    /**
     * Some of this depends on preferences.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test {@link Predicates#alwaysTrue()}
     */
    @Test
    public void testAlwaysTrue() {
        Predicate<Object> alwaysTrue = Predicates.alwaysTrue();
        assertTrue(alwaysTrue.evaluate(new Object()));
        assertTrue(alwaysTrue.evaluate(Boolean.TRUE));
    }

    /**
     * Test {@link Predicates#alwaysFalse()}
     */
    @Test
    public void testAlwaysFalse() {
        Predicate<Object> alwaysFalse = Predicates.alwaysFalse();
        assertFalse(alwaysFalse.evaluate(new Object()));
        assertFalse(alwaysFalse.evaluate(Boolean.TRUE));
    }

    /**
     * Test {@link Predicates#not(Predicate)}
     */
    @Test
    public void testNot() {
        Predicate<Boolean> not = Predicates.not(new Predicate<Boolean>() {
            @Override
            public boolean evaluate(Boolean object) {
                return object;
            }
        });
        assertFalse(not.evaluate(Boolean.TRUE));
        assertTrue(not.evaluate(Boolean.FALSE));
    }

    /**
     * Test {@link Predicates#equalTo(Object)}
     */
    @Test
    public void testEqualTo() {
        Integer testObject = Integer.valueOf(1);
        Predicate<Integer> equalTo = Predicates.equalTo(testObject);
        assertTrue(equalTo.evaluate(testObject));
        assertTrue(equalTo.evaluate(Integer.valueOf(1)));

        assertFalse(equalTo.evaluate(Integer.valueOf(2)));
        assertFalse(equalTo.evaluate(null));
    }

    /**
     * Test {@link Predicates#isOfClass(Class)}
     */
    @Test
    public void testIsOfClass() {
        Predicate<Object> isOfClass = Predicates.<Object>isOfClass(Hashtable.class);
        assertFalse(isOfClass.evaluate(null));
        assertFalse(isOfClass.evaluate(new Object()));
        assertFalse(isOfClass.evaluate(new Properties()));
        assertTrue(isOfClass.evaluate(new Hashtable<>()));
    }

    /**
     * Test {@link Predicates#isOfClass(Class)}
     */
    @Test
    public void testIsInstanceOf() {
        Predicate<Object> isInstanceOf = Predicates.<Object>isInstanceOf(Hashtable.class);
        assertFalse(isInstanceOf.evaluate(null));
        assertFalse(isInstanceOf.evaluate(new Object()));
        assertTrue(isInstanceOf.evaluate(new Properties()));
        assertTrue(isInstanceOf.evaluate(new Hashtable<>()));
    }

    /**
     * Test {@link Predicates#stringMatchesPattern(java.util.regex.Pattern)}
     */
    @Test
    public void testStringMatchesPattern() {
        Pattern p = Pattern.compile("ab?c");
        Predicate<String> stringMatchesPattern = Predicates.stringMatchesPattern(p);
        assertFalse(stringMatchesPattern.evaluate(""));
        assertFalse(stringMatchesPattern.evaluate("a"));
        assertFalse(stringMatchesPattern.evaluate("xabcx"));
        assertTrue(stringMatchesPattern.evaluate("ac"));
        assertTrue(stringMatchesPattern.evaluate("abc"));
    }

    /**
     * Test {@link Predicates#stringContainsPattern(java.util.regex.Pattern)}
     */
    @Test
    public void testStringContainsPattern() {
        Pattern p = Pattern.compile("ab?c");
        Predicate<String> stringContainsPattern = Predicates.stringContainsPattern(p);
        assertFalse(stringContainsPattern.evaluate(""));
        assertFalse(stringContainsPattern.evaluate("a"));
        assertTrue(stringContainsPattern.evaluate("xabcx"));
        assertTrue(stringContainsPattern.evaluate("ac"));
        assertTrue(stringContainsPattern.evaluate("abc"));
        assertTrue(stringContainsPattern.evaluate("xx\nabc\nx"));
    }

    /**
     * Test {@link Predicates#stringContains(String)}
     */
    @Test
    public void testStringContains() {
        Predicate<String> stringContains = Predicates.stringContains("abc");
        assertFalse(stringContains.evaluate(""));
        assertFalse(stringContains.evaluate("a"));
        assertTrue(stringContains.evaluate("xabcx"));
        assertFalse(stringContains.evaluate("ac"));
        assertTrue(stringContains.evaluate("abc"));
    }

    /**
     * Test {@link Predicates#hasTag(String, String...)}
     */
    @Test
    public void testHasTag() {
        Predicate<OsmPrimitive> hasTag = Predicates.hasTag("key", "value");
        Node n1 = new Node();
        assertFalse(hasTag.evaluate(n1));
        n1.put("Key", "x");
        assertFalse(hasTag.evaluate(n1));
        n1.put("key", "x");
        assertFalse(hasTag.evaluate(n1));
        n1.put("key", "value");
        assertTrue(hasTag.evaluate(n1));
    }

    /**
     * Test {@link Predicates#hasKey(String)}
     */
    @Test
    public void testHasKey() {
        Predicate<OsmPrimitive> hasKey = Predicates.hasKey("key");
        Node n1 = new Node();
        assertFalse(hasKey.evaluate(n1));
        n1.put("Key", "x");
        assertFalse(hasKey.evaluate(n1));
        n1.put("key", "x");
        assertTrue(hasKey.evaluate(n1));
    }

    /**
     * Test {@link Predicates#inCollection(java.util.Collection)}
     */
    @Test
    public void testInCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        Predicate<String> inCollection = Predicates.inCollection(list);
        assertTrue(inCollection.evaluate("a"));
        assertTrue(inCollection.evaluate("c"));
        assertFalse(inCollection.evaluate("d"));
        assertFalse(inCollection.evaluate(null));

        List<String> list2 = Arrays.asList("a", "b", "c", null);
        Predicate<String> inCollection2 = Predicates.inCollection(list2);
        assertTrue(inCollection2.evaluate("a"));
        assertTrue(inCollection2.evaluate("c"));
        assertFalse(inCollection2.evaluate("d"));
        assertTrue(inCollection2.evaluate(null));
    }

    /**
     * Test {@link Predicates#isNull()}
     */
    @Test
    public void testIsNull() {
        Predicate<Object> isNull = Predicates.isNull();
        assertTrue(isNull.evaluate(null));
        assertFalse(isNull.evaluate(Integer.valueOf(2)));
    }
}
