// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        id = OsmPrimitive.generateUniqueId();
    }

    public PrimitiveData(PrimitiveData data) {
        cloneFrom(data);
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public abstract PrimitiveData makeCopy();

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(id).append(' ').append(Arrays.toString(keys)).append(' ').append(getFlagsAsString());
        return builder.toString();
    }

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
        ois.defaultReadObject();
    }
}
