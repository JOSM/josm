// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
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
 * @deprecated to remove when {@code Predicates} class is removed
 */
@Deprecated
public class PredicatesTest {
    /**
     * Some of this depends on preferences.
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Deprecated
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test {@link Predicates#alwaysTrue()}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testAlwaysTrue() {
        Predicate<Object> alwaysTrue = Predicates.alwaysTrue();
        assertTrue(alwaysTrue.test(new Object()));
        assertTrue(alwaysTrue.test(Boolean.TRUE));
    }

    /**
     * Test {@link Predicates#alwaysFalse()}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testAlwaysFalse() {
        Predicate<Object> alwaysFalse = Predicates.alwaysFalse();
        assertFalse(alwaysFalse.test(new Object()));
        assertFalse(alwaysFalse.test(Boolean.TRUE));
    }

    /**
     * Test {@link Predicates#equalTo(Object)}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testEqualTo() {
        Integer testObject = Integer.valueOf(1);
        Predicate<Integer> equalTo = Predicates.equalTo(testObject);
        assertTrue(equalTo.test(testObject));
        assertTrue(equalTo.test(Integer.valueOf(1)));

        assertFalse(equalTo.test(Integer.valueOf(2)));
        assertFalse(equalTo.test(null));
    }

    /**
     * Test {@link Predicates#isOfClass(Class)}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testIsOfClass() {
        Predicate<Object> isOfClass = Predicates.<Object>isOfClass(Hashtable.class);
        assertFalse(isOfClass.test(null));
        assertFalse(isOfClass.test(new Object()));
        assertFalse(isOfClass.test(new Properties()));
        assertTrue(isOfClass.test(new Hashtable<>()));
    }

    /**
     * Test {@link Predicates#isOfClass(Class)}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testIsInstanceOf() {
        Predicate<Object> isInstanceOf = Predicates.<Object>isInstanceOf(Hashtable.class);
        assertFalse(isInstanceOf.test(null));
        assertFalse(isInstanceOf.test(new Object()));
        assertTrue(isInstanceOf.test(new Properties()));
        assertTrue(isInstanceOf.test(new Hashtable<>()));
    }

    /**
     * Test {@link Predicates#stringMatchesPattern(java.util.regex.Pattern)}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testStringMatchesPattern() {
        Pattern p = Pattern.compile("ab?c");
        Predicate<String> stringMatchesPattern = Predicates.stringMatchesPattern(p);
        assertFalse(stringMatchesPattern.test(""));
        assertFalse(stringMatchesPattern.test("a"));
        assertFalse(stringMatchesPattern.test("xabcx"));
        assertTrue(stringMatchesPattern.test("ac"));
        assertTrue(stringMatchesPattern.test("abc"));
    }

    /**
     * Test {@link Predicates#stringContainsPattern(java.util.regex.Pattern)}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testStringContainsPattern() {
        Pattern p = Pattern.compile("ab?c");
        Predicate<String> stringContainsPattern = Predicates.stringContainsPattern(p);
        assertFalse(stringContainsPattern.test(""));
        assertFalse(stringContainsPattern.test("a"));
        assertTrue(stringContainsPattern.test("xabcx"));
        assertTrue(stringContainsPattern.test("ac"));
        assertTrue(stringContainsPattern.test("abc"));
        assertTrue(stringContainsPattern.test("xx\nabc\nx"));
    }

    /**
     * Test {@link Predicates#stringContains(String)}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testStringContains() {
        Predicate<String> stringContains = Predicates.stringContains("abc");
        assertFalse(stringContains.test(""));
        assertFalse(stringContains.test("a"));
        assertTrue(stringContains.test("xabcx"));
        assertFalse(stringContains.test("ac"));
        assertTrue(stringContains.test("abc"));
    }

    /**
     * Test {@link Predicates#hasTag(String, String...)}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testHasTag() {
        Predicate<OsmPrimitive> hasTag = Predicates.hasTag("key", "value");
        Node n1 = new Node();
        assertFalse(hasTag.test(n1));
        n1.put("Key", "x");
        assertFalse(hasTag.test(n1));
        n1.put("key", "x");
        assertFalse(hasTag.test(n1));
        n1.put("key", "value");
        assertTrue(hasTag.test(n1));
    }

    /**
     * Test {@link Predicates#hasKey(String)}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testHasKey() {
        Predicate<OsmPrimitive> hasKey = Predicates.hasKey("key");
        Node n1 = new Node();
        assertFalse(hasKey.test(n1));
        n1.put("Key", "x");
        assertFalse(hasKey.test(n1));
        n1.put("key", "x");
        assertTrue(hasKey.test(n1));
    }

    /**
     * Test {@link Predicates#inCollection(java.util.Collection)}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testInCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        Predicate<String> inCollection = Predicates.inCollection(list);
        assertTrue(inCollection.test("a"));
        assertTrue(inCollection.test("c"));
        assertFalse(inCollection.test("d"));
        assertFalse(inCollection.test(null));

        List<String> list2 = Arrays.asList("a", "b", "c", null);
        Predicate<String> inCollection2 = Predicates.inCollection(list2);
        assertTrue(inCollection2.test("a"));
        assertTrue(inCollection2.test("c"));
        assertFalse(inCollection2.test("d"));
        assertTrue(inCollection2.test(null));
    }

    /**
     * Test {@link Predicates#isNull()}
     * @deprecated to remove when {@code Predicates} class is removed
     */
    @Test
    @Deprecated
    public void testIsNull() {
        Predicate<Object> isNull = Predicates.isNull();
        assertTrue(isNull.test(null));
        assertFalse(isNull.test(Integer.valueOf(2)));
    }
}
