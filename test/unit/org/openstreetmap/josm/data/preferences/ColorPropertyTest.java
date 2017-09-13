// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import static org.junit.Assert.assertEquals;

import java.awt.Color;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test {@link ColorProperty}
 * @author Michael Zangl
 */
public class ColorPropertyTest {
    /**
     * This is a preference test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();
    private ColorProperty base;

    /**
     * Set up test case
     */
    @Before
    public void createTestProperty() {
        base = new ColorProperty("test", Color.RED);
    }

    /**
     * Test {@link ColorProperty#get()}
     */
    @Test
    public void testGet() {
        assertEquals(Color.RED, base.get());

        Config.getPref().put("color.test", "#00ff00");
        assertEquals(new Color(0xff00ff00), base.get());
    }

    /**
     * Test {@link ColorProperty#put}
     */
    @Test
    public void testPut() {
        assertEquals(Color.RED, base.get());

        base.put(new Color(0xff00ff00));
        assertEquals(new Color(0xff00ff00), base.get());
        assertEquals("#00ff00", Config.getPref().get("color.test").toLowerCase());

        base.put(null);
        assertEquals(Color.RED, base.get());
    }

    /**
     * Test {@link ColorProperty#getChildColor(String)}
     */
    @Test
    public void testGetChildColor() {
        AbstractToStringProperty<Color> child = base.getChildColor("test2");

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
