// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.conflict;

import java.util.Map;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;

/**
 * Represents a conflict between two {@link OsmPrimitive}s. It is represented as
 * a pair of {@link OsmPrimitive}s where one element of the pair has the role <em>my</em>
 * and the other has the role <em>their</em>.
 * <ul>
 *   <li><code>my</code> is the {@link OsmPrimitive} in the local dataset</li>
 *   <li><code>their</code> is the {@link OsmPrimitive} which caused the conflict when it
 *   it was tried to merge it onto <code>my</code>. <code>their</code> is usually the
 *   {@link OsmPrimitive} from the dataset in another layer or the one retrieved from the server.</li>
 * </ul>
 * @param <T> primitive type of the conflict
 * @since 1750
 */
public class Conflict<T extends OsmPrimitive> {
    private final T my;
    private final T their;
    private final boolean isMyDeleted;

    // mergedMap is only set if the conflict results from merging two layers
    private Map<PrimitiveId, PrimitiveId> mergedMap;

    public Conflict(T my, T their) {
        this(my, their, false);
    }

    public Conflict(T my, T their, boolean isMyDeleted) {
        this.my = my;
        this.their = their;
        this.isMyDeleted = isMyDeleted;
    }

    public T getMy() {
        return my;
    }

    public T getTheir() {
        return their;
    }

    public boolean isMatchingMy(OsmPrimitive my) {
        return this.my == my;
    }

    public boolean isMatchingTheir(OsmPrimitive their) {
        return this.their == their;
    }

    /**
     * Replies true if the primitive <code>primitive</code> is participating
     * in this conflict
     *
     * @param primitive the primitive
     * @return true if the primitive <code>primitive</code> is participating
     * in this conflict
     */
    public boolean isParticipating(OsmPrimitive primitive) {
        if (primitive == null) return false;
        return primitive.getPrimitiveId().equals(my.getPrimitiveId())
        || primitive.getPrimitiveId().equals(their.getPrimitiveId());
    }

    /**
     * Replies true if the primitive with id <code>id</code> is participating
     * in this conflict
     *
     * @param id the primitive id
     * @return true if the primitive <code>primitive</code> is participating
     * in this conflict
     */
    public boolean isParticipating(PrimitiveId id) {
        if (id == null) return false;
        return id.equals(my.getPrimitiveId())
        || id.equals(their.getPrimitiveId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(my, their);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Conflict<?> conflict = (Conflict<?>) obj;
        return Objects.equals(my, conflict.my) &&
                Objects.equals(their, conflict.their);
    }

    /**
     * Determines if my primitive was deleted but it has set non deleted status.
     * @return True if my primitive was deleted but it has set non deleted status because it's referred by another
     * primitive and references to deleted primitives are not allowed.
     */
    public boolean isMyDeleted() {
        return isMyDeleted;
    }

    public final Map<PrimitiveId, PrimitiveId> getMergedMap() {
        return mergedMap;
    }

    public final void setMergedMap(Map<PrimitiveId, PrimitiveId> mergedMap) {
        this.mergedMap = mergedMap;
    }

    @Override
    public String toString() {
        return "Conflict [my=" + my + ", their=" + their + ']';
    }
}
