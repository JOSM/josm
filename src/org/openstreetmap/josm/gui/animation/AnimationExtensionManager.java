// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

import org.openstreetmap.josm.data.preferences.BooleanProperty;

/**
 * Animation extension manager. Copied from Icedtea-Web.
 * @author Jiri Vanek (Red Hat)
 * @see <a href="http://icedtea.classpath.org/hg/icedtea-web/rev/87d3081ab573">Initial commit</a>
 * @since 14578
 */
public final class AnimationExtensionManager {

    private static volatile AnimationExtension currentExtension;
    private static final BooleanProperty PROP_ANIMATION = new BooleanProperty("gui.start.animation", true);

    private AnimationExtensionManager() {
        // Hide default constructor for utility classes
    }

    /**
     * Returns the current animation extension.
     * @return the current animation extension
     */
    public static AnimationExtension getExtension() {
        if (currentExtension == null) {
            final boolean isAnimated = Boolean.TRUE.equals(PROP_ANIMATION.get());
            if (isAnimated && isChristmas()) {
                currentExtension = new ChristmasExtension();
            } else if (isAnimated && isBirthday()) {
                currentExtension = new BirthdayExtension();
            } else {
                currentExtension = new NoExtension();
            }
        }
        return currentExtension;
    }

    /**
     * Determines if an extension other than {@link NoExtension} is enabled.
     * @return {@code true} if an extension other than {@code NoExtension} is enabled.
     * @since 17322
     */
    public static boolean isExtensionEnabled() {
        return !(getExtension() instanceof NoExtension);
    }

    private static boolean isChristmas() {
        return LocalDate.now(ZoneId.systemDefault()).getDayOfYear() > 350;
    }

    /**
     * The first commit of JOSM to svn (r1) was on 2005-09-27
     * @return {@code true} if today is JOSM's birthday
     */
    private static boolean isBirthday() {
        LocalDate l = LocalDate.now(ZoneId.systemDefault());
        return l.getMonth() == Month.SEPTEMBER && l.getDayOfMonth() == 27;
    }
}
