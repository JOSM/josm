// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.lifecycle;

/**
 * Initialization task listener.
 * @since 14125
 */
public interface InitStatusListener {

    /**
     * Called when an initialization task updates its status.
     * @param event task name
     * @return new status
     */
    Object updateStatus(String event);

    /**
     * Called when an initialization task completes.
     * @param status final status
     */
    void finish(Object status);
}
