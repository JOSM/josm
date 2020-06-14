// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;
import static org.openstreetmap.josm.gui.conflict.pair.ListRole.MERGED_ENTRIES;
import static org.openstreetmap.josm.gui.conflict.pair.ListRole.MY_ENTRIES;
import static org.openstreetmap.josm.gui.conflict.pair.ListRole.THEIR_ENTRIES;
import static org.openstreetmap.josm.tools.I18n.tr;

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
    private final ListRole participatingRole1;
    private final ListRole participatingRole2;

    ComparePairType(String displayName, ListRole participatingRole1, ListRole participatingRole2) {
        this.displayName = displayName;
        this.participatingRole1 = participatingRole1;
        this.participatingRole2 = participatingRole2;
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
        return participatingRole1 == role || participatingRole2 == role;
    }

    /**
     * replies the pair of {@link ListRole}s participating in this comparison pair
     *
     * @return  the pair of list roles
     */
    public ListRole[] getParticipatingRoles() {
        return new ListRole[]{participatingRole1, participatingRole2};
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
        return participatingRole1 == role ? participatingRole2 : participatingRole1;
    }
}
