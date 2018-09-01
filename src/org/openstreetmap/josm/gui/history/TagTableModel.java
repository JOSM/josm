// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;

/**
 * The table model for the tags of the version
 * at {@link PointInTimeType#REFERENCE_POINT_IN_TIME}
 * or {@link PointInTimeType#CURRENT_POINT_IN_TIME}
 * @since 11647 (extracted from HistoryBrowserModel)
 */
public final class TagTableModel extends AbstractTableModel {

    private List<String> keys;
    private final PointInTimeType pointInTimeType;
    private final HistoryBrowserModel model;

    /**
     * Constructs a new {@code TagTableModel}.
     * @param historyModel parent {@code HistoryBrowserModel}
     * @param type type of point in time
     */
    public TagTableModel(HistoryBrowserModel historyModel, PointInTimeType type) {
        model = historyModel;
        pointInTimeType = type;
        initKeyList();
    }

    void initKeyList() {
        keys = new ArrayList<>(model.getKeySet());
        Collections.sort(keys);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        if (keys == null)
            return 0;
        return keys.size();
    }

    @Override
    public Object getValueAt(int row, int column) {
        return getKeyAt(row);
    }

    /**
     * Get the key for the given row.
     * @param row The row
     * @return The key in that row.
     * @since 10637
     */
    public String getKeyAt(int row) {
        return keys.get(row);
    }

    /**
     * Determines if a tag exists for the given key.
     * @param key tag key
     * @return {@code true} if a tag exists for the given key
     */
    public boolean hasTag(String key) {
        HistoryOsmPrimitive primitive = model.getPointInTime(pointInTimeType);
        return primitive != null && primitive.hasKey(key);
    }

    /**
     * Returns the tag value for the given key.
     * @param key tag key
     * @return tag value, or null
     */
    public String getValue(String key) {
        HistoryOsmPrimitive primitive = model.getPointInTime(pointInTimeType);
        if (primitive == null)
            return null;
        return primitive.get(key);
    }

    /**
     * Determines if a tag exists in the opposite point in time for the given key.
     * @param key tag key
     * @return {@code true} if a tag exists for the given key
     */
    public boolean oppositeHasTag(String key) {
        HistoryOsmPrimitive primitive = model.getPointInTime(pointInTimeType.opposite());
        return primitive != null && primitive.hasKey(key);
    }

    /**
     * Returns the tag value in the opposite point in time for the given key.
     * @param key tag key
     * @return tag value, or null
     */
    public String getOppositeValue(String key) {
        HistoryOsmPrimitive primitive = model.getPointInTime(pointInTimeType.opposite());
        if (primitive == null)
            return null;
        return primitive.get(key);
    }

    /**
     * Determines if the tag value is the same in the opposite point in time for the given key.
     * @param key tag key
     * @return {@code true} if the tag value is the same in the opposite point in time for the given key
     */
    public boolean hasSameValueAsOpposite(String key) {
        String value = getValue(key);
        String oppositeValue = getOppositeValue(key);
        return value != null && value.equals(oppositeValue);
    }

    /**
     * Returns the type of point in time.
     * @return the type of point in time
     */
    public PointInTimeType getPointInTimeType() {
        return pointInTimeType;
    }

    /**
     * Determines if this is the current point in time.
     * @return {@code true} if this is the current point in time
     */
    public boolean isCurrentPointInTime() {
        return pointInTimeType == PointInTimeType.CURRENT_POINT_IN_TIME;
    }

    /**
     * Determines if this is the reference point in time.
     * @return {@code true} if this is the reference point in time
     */
    public boolean isReferencePointInTime() {
        return pointInTimeType == PointInTimeType.REFERENCE_POINT_IN_TIME;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }
}
