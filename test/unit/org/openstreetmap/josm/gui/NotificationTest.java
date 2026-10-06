// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

/**
 * Unit tests of {@link Notification}.
 */
@BasicPreferences
class NotificationTest {

    /**
     * A plugin may hand over its logo in full size, which must not blow up the notification.
     */
    @Test
    void testLargeIconIsScaledDown() {
        int maxSize = ImageSizes.NOTIFICATION.getAdjustedWidth();
        Icon icon = new Notification().setIcon(new ImageIcon(new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB))).getIcon();
        assertEquals(maxSize, icon.getIconWidth(), "the icon does not fit");
        assertEquals(maxSize / 2, icon.getIconHeight(), "the aspect ratio of the icon is not kept");
    }
}
