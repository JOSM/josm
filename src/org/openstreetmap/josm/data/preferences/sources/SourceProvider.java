// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences.sources;

import java.util.Collection;

/**
 * Interface for a class that offers a list of {@link SourceEntry}s.
 *
 * Used by plugins to offer additional SourceEntrys to the user.
 * @since 12649 (moved from gui.preferences package)
 */
@FunctionalInterface
public interface SourceProvider {

    /**
     * Get the collection of {@link SourceEntry}s.
     * @return the collection of {@link SourceEntry}s
     */
    Collection<SourceEntry> getSources();
}
