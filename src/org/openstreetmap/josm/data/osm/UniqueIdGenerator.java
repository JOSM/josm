// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generator of unique identifiers.
 * @since 15820
 */
public final class UniqueIdGenerator {

    private final AtomicLong idCounter = new AtomicLong(0);

    /**
     * Generates a new primitive unique id.
     * @return new primitive unique (negative) id
     */
    long generateUniqueId() {
        return idCounter.decrementAndGet();
    }

    /**
     * Returns the current primitive unique id.
     * @return the current primitive unique (negative) id (last generated)
     */
    public long currentUniqueId() {
        return idCounter.get();
    }

    /**
     * Advances the current primitive unique id to skip a range of values.
     * @param newId new unique id
     * @throws IllegalArgumentException if newId is greater than current unique id
     */
    public void advanceUniqueId(long newId) {
        if (newId > currentUniqueId()) {
            throw new IllegalArgumentException("Cannot modify the id counter backwards");
        }
        idCounter.set(newId);
    }
}
