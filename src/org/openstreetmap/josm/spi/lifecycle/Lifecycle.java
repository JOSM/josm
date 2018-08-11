// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.lifecycle;

import java.util.Objects;

/**
 * JOSM lifecycle.
 * @since 14125
 */
public final class Lifecycle {

    private static volatile InitStatusListener initStatusListener;

    private Lifecycle() {
        // Hide constructor
    }

    /**
     * Gets initialization task listener.
     * @return initialization task listener
     */
    public static InitStatusListener getInitStatusListener() {
        return initStatusListener;
    }

    /**
     * Sets initialization task listener.
     * @param listener initialization task listener. Must not be null
     */
    public static void setInitStatusListener(InitStatusListener listener) {
        initStatusListener = Objects.requireNonNull(listener);
    }
}
