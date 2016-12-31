package org.apache.commons.jcs.utils.struct;

/**
 *
 * @author Wiktor Niesiobędzki
 *
 *         Simple LRUMap implementation that keeps the number of the objects below or equal maxObjects
 *
 * @param <K>
 * @param <V>
 */
public class LRUMap<K, V> extends AbstractLRUMap<K, V>
{
    /** if the max is less than 0, there is no limit! */
    private int maxObjects = -1;

    public LRUMap()
    {
        super();
    }

    /**
     *
     * @param maxObjects
     *            maximum number to keep in the map
     */
    public LRUMap(int maxObjects)
    {
        this();
        this.maxObjects = maxObjects;
    }

    @Override
    public boolean shouldRemove()
    {
        return maxObjects > 0 && this.size() > maxObjects;
    }
}
