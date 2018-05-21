// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.gui.mappaint.StyleCache;

/**
 * This class can be used to save properties of OsmPrimitive.
 *
 * The main difference between PrimitiveData
 * and OsmPrimitive is that PrimitiveData is not part of the dataset and changes in PrimitiveData are not
 * reported by events
 */
public abstract class PrimitiveData extends AbstractPrimitive implements Serializable {

    private static final long serialVersionUID = -1044837092478109138L;

    /**
     * Constructs a new {@code PrimitiveData}.
     */
    public PrimitiveData() {
        this(OsmPrimitive.generateUniqueId());
    }

    /**
     * Constructs a new {@code PrimitiveData} with given id.
     * @param id id
     * @since 12017
     */
    public PrimitiveData(long id) {
        this.id = id;
    }

    /**
     * Constructs a new {@code PrimitiveData} from an existing one.
     * @param data the data to copy
     */
    public PrimitiveData(PrimitiveData data) {
        cloneFrom(data);
    }

    /**
     * Sets the primitive identifier.
     * @param id primitive identifier
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Sets the primitive version.
     * @param version primitive version
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * override to make it public
     */
    @Override
    public void setIncomplete(boolean incomplete) {
        super.setIncomplete(incomplete);
    }

    /**
     * Returns a copy of this primitive data.
     * @return a copy of this primitive data
     */
    public abstract PrimitiveData makeCopy();

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(id).append(' ').append(Arrays.toString(keys)).append(' ').append(getFlagsAsString());
        return builder.toString();
    }

    /**
     * Returns a filtered list for a given primitive type.
     * @param <T> primitive type
     * @param list list to filter
     * @param type primitive type
     * @return a filtered list for given primitive type
     */
    @SuppressWarnings("unchecked")
    public static <T extends PrimitiveData> List<T> getFilteredList(Collection<T> list, OsmPrimitiveType type) {
        List<T> ret = new ArrayList<>();
        for (PrimitiveData p: list) {
            if (type.getDataClass().isInstance(p)) {
                ret.add((T) p);
            }
        }
        return ret;
    }

    @Override
    protected final void keysChangedImpl(Map<String, String> originalKeys) {
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        // since super class is not Serializable
        oos.writeLong(id);
        oos.writeLong(user == null ? -1 : user.getId());
        oos.writeInt(version);
        oos.writeInt(changesetId);
        oos.writeInt(timestamp);
        oos.writeObject(keys);
        oos.writeShort(flags);
        oos.defaultWriteObject();
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        // since super class is not Serializable
        id = ois.readLong();
        final long userId = ois.readLong();
        user = userId == -1 ? null : User.getById(userId);
        version = ois.readInt();
        changesetId = ois.readInt();
        timestamp = ois.readInt();
        keys = (String[]) ois.readObject();
        flags = ois.readShort();
        ois.defaultReadObject();
    }

    @Override
    public boolean isTagged() {
        return hasKeys();
    }

    @Override
    public boolean isAnnotated() {
        return false;
    }

    @Override
    public boolean hasDirectionKeys() {
        return false;
    }

    @Override
    public boolean reversedDirection() {
        return false;
    }

    @Override
    public void setHighlighted(boolean highlighted) {
        // Override if needed
    }

    @Override
    public boolean isHighlighted() {
        return false;
    }

    @Override
    public final List<PrimitiveData> getReferrers() {
        return Collections.emptyList();
    }

    @Override
    public void visitReferrers(PrimitiveVisitor visitor) {
        // Override if needed
    }

    @Override
    public StyleCache getCachedStyle() {
        return null;
    }

    @Override
    public void setCachedStyle(StyleCache mappaintStyle) {
        // Override if needed
    }

    @Override
    public boolean isCachedStyleUpToDate() {
        return false;
    }

    @Override
    public void declareCachedStyleUpToDate() {
        // Override if needed
    }
}
