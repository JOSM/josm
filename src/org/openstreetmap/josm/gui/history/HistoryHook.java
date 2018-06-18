// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.util.List;

import org.openstreetmap.josm.data.osm.PrimitiveId;

/**
 * Change, or block, history requests.
 *
 * The HistoryHook may modify the requested primitive ids silently, it may display a
 * warning message to the user or prevent the request altogether.
 * @since 13947
 */
public interface HistoryHook {

    /**
     * Modify the requested primitive ids before history request.
     * The request is cancelled if the collection is cleared.
     * Default implementation is to do no changes.
     * @param ids The current ids to change
     * @since 13948
     */
    default void modifyRequestedIds(List<PrimitiveId> ids) {
    }
}
