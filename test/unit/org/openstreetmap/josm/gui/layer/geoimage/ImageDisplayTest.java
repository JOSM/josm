// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.awt.Rectangle;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.layer.geoimage.ImageDisplay.VisRect;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ImageDisplay} class.
 */
public class ImageDisplayTest {
    /**
     * We need prefs for this.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link ImageDisplay#calculateDrawImageRectangle}.
     */
    @Test
    public void testCalculateDrawImageRectangle() {
        assertEquals(new Rectangle(),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(), new Dimension()));
        assertEquals(new Rectangle(0, 0, 10, 5),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(10, 5)));
        assertEquals(new Rectangle(0, 0, 10, 5),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 20, 10), new Dimension(10, 5)));
        assertEquals(new Rectangle(0, 0, 20, 10),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(20, 10)));
        assertEquals(new Rectangle(5, 0, 24, 12),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(35, 12)));
        assertEquals(new Rectangle(0, 1, 8, 4),
                ImageDisplay.calculateDrawImageRectangle(new VisRect(0, 0, 10, 5), new Dimension(8, 6)));
    }
}
