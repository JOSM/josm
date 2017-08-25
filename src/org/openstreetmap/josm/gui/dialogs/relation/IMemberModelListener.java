// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

/**
 * A listener that is called whenever a member should be made visible
 */
@FunctionalInterface
public interface IMemberModelListener {
    /**
     * Requests the given member to become visible
     * @param index The index of the member in the table.
     */
    void makeMemberVisible(int index);
}
