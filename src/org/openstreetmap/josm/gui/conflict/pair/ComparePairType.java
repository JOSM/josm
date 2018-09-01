// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;
import static org.openstreetmap.josm.gui.conflict.pair.ListRole.MERGED_ENTRIES;
import static org.openstreetmap.josm.gui.conflict.pair.ListRole.MY_ENTRIES;
import static org.openstreetmap.josm.gui.conflict.pair.ListRole.THEIR_ENTRIES;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.tools.Utils;

/**
 * Enumeration of the possible comparison pairs
 * @since 1650
 */
public enum ComparePairType {

    /**
     * compare my version of an {@link org.openstreetmap.josm.data.osm.OsmPrimitive} with their version
     */
    MY_WITH_THEIR(tr("My with Their"), MY_ENTRIES, THEIR_ENTRIES),

    /**
     * compare my version of an {@link org.openstreetmap.josm.data.osm.OsmPrimitive} with the merged version
     */
    MY_WITH_MERGED(tr("My with Merged"), MY_ENTRIES, MERGED_ENTRIES),

    /**
     * compare their version of an {@link org.openstreetmap.josm.data.osm.OsmPrimitive} with the merged veresion
     */
    THEIR_WITH_MERGED(tr("Their with Merged"), THEIR_ENTRIES, MERGED_ENTRIES);

    /** the localized display name */
    private final String displayName;
    private final ListRole[] participatingRoles;

    ComparePairType(String displayName, ListRole... participatingRoles) {
        this.displayName = displayName;
        this.participatingRoles = Utils.copyArray(participatingRoles);
    }

    /**
     * replies the display name
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * replies true, if <code>role</code> is participating in this comparison pair
     *
     * @param role  the list role
     * @return true, if <code>role</code> is participating in this comparison pair; false, otherwise
     */
    public boolean isParticipatingIn(ListRole role) {
        for (ListRole r: participatingRoles) {
            if (r == role) return true;
        }
        return false;
    }

    /**
     * replies the pair of {@link ListRole}s participating in this comparison pair
     *
     * @return  the pair of list roles
     */
    public ListRole[] getParticipatingRoles() {
        return Utils.copyArray(participatingRoles);
    }

    /**
     * replies the opposite role of <code>role</code> participating in this comparison pair
     *
     * @param role one of the two roles in this pair
     * @return the opposite role
     * @throws IllegalStateException  if role is not participating in this pair
     */
    public ListRole getOppositeRole(ListRole role) {
        if (!isParticipatingIn(role))
            throw new IllegalStateException(tr("Role {0} is not participating in compare pair {1}.", role.toString(), this.toString()));
        if (participatingRoles[0] == role)
            return participatingRoles[1];
        else
            return participatingRoles[0];
    }
}
