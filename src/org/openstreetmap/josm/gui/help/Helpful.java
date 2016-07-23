// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

/**
 * Anything on which we can provide help.
 * @since 2252
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface Helpful {

    /**
     * Returns the help topic on JOSM wiki for this feature.
     * @return the help topic on JOSM wiki for this feature
     */
    String helpTopic();
}
