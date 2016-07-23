// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import org.openstreetmap.josm.data.osm.Changeset;

/**
 * Super interface of changeset-aware components.
 * @since 9493
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface ChangesetAware {

    /**
     * Returns the current changeset.
     * @return the current changeset
     */
    Changeset getCurrentChangeset();
}
