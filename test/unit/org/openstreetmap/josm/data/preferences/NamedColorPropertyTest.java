// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test {@link NamedColorProperty}
 * @author Michael Zangl
 */
public class NamedColorPropertyTest {
    /**
     * This is a preference test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();
    private NamedColorProperty base;

    /**
     * Set up test case
     */
    @Before
    public void createTestProperty() {
        base = new NamedColorProperty("test", Color.RED);
    }

    /**
     * Test {@link NamedColorProperty#get()}
     */
    @Test
    public void testGet() {
        assertEquals(Color.RED, base.get());
    }

    /**
     * Test {@link NamedColorProperty#put}
     */
    @Test
    public void testPut() {
        assertEquals(Color.RED, base.get());

        base.put(new Color(0xff00af00));
        assertEquals(new Color(0xff00af00), base.get());
        assertEquals("#00af00", Config.getPref().getList("clr.general.test").get(0).toLowerCase());

        base.put(null);
        assertEquals(Color.RED, base.get());
    }

    /**
     * Test color alpha.
     */
    @Test
    public void testColorAlpha() {
        assertEquals(0x12, new NamedColorProperty("foo", new Color(0x12345678, true)).get().getAlpha());
        assertTrue(Preferences.main().putList("clr.general.bar", Arrays.asList("#34567812", "general", "", "bar")));
        assertEquals(0x12, new NamedColorProperty("bar", Color.RED).get().getAlpha());
    }

    /**
     * Test color name and alpha.
     */
    @Test
    public void testColorNameAlpha() {
        assertEquals(0x12, new NamedColorProperty("foo", new Color(0x12345678, true)).get().getAlpha());
    }

    /**
     * Test {@link NamedColorProperty#getChildColor(String)}
     */
    @Test
    public void testGetChildColor() {
        AbstractProperty<Color> child = base.getChildColor("test2");

        assertEquals(Color.RED, child.get());

        base.put(Color.GREEN);
        assertEquals(Color.GREEN, child.get());

        child.put(Color.YELLOW);
        assertEquals(Color.YELLOW, child.get());
        assertEquals(Color.GREEN, base.get());

        child.put(null);
        assertEquals(Color.GREEN, child.get());
    }
}
