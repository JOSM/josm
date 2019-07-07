// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Animation extension manager. Copied from Icedtea-Web.
 * @author Jiri Vanek (Red Hat)
 * @see <a href="http://icedtea.classpath.org/hg/icedtea-web/rev/87d3081ab573">Initial commit</a>
 * @since 14578
 */
public final class AnimationExtensionManager {

    private static volatile AnimationExtension currentExtension;

    private AnimationExtensionManager() {
        // Hide default constructor for utility classes
    }

    /**
     * Returns the current animation extension.
     * @return the current animation extension
     */
    public static AnimationExtension getExtension() {
        if (currentExtension == null) {
            currentExtension = isChristmas() ? new ChristmasExtension() : new NoExtension();
        }
        return currentExtension;
    }

    private static boolean isChristmas() {
        Calendar c = new GregorianCalendar();
        c.setTime(new Date());
        return c.get(Calendar.DAY_OF_YEAR) > 350;
    }
}
