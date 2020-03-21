// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.fail;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link FontsManager} class.
 */
public class FontsManagerTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test method for {@code FontsManager#initialize}
     */
    @Test
    public void testFontsManager() {
        FontsManager.initialize();
        boolean found = false;
        for (Font f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
            if (f.getName().contains("Droid")) {
                System.out.println(f);
                found = true;
            }
        }
        if (!found) {
            fail("DroidSans font not found");
        }
    }

    /**
     * Tests that {@code FontsManager} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(FontsManager.class);
    }
}
