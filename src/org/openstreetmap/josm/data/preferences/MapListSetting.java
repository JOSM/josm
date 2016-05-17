// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Setting containing a {@link List} of {@link Map}s of {@link String} values.
 * @since 9759
 */
public class MapListSetting extends AbstractSetting<List<Map<String, String>>> {

    /**
     * Constructs a new {@code MapListSetting} with the given value
     * @param value The setting value
     */
    public MapListSetting(List<Map<String, String>> value) {
        super(value);
        consistencyTest();
    }

    @Override
    public boolean equalVal(List<Map<String, String>> otherVal) {
        if (value == null)
            return otherVal == null;
        if (otherVal == null)
            return false;
        if (value.size() != otherVal.size())
            return false;
        Iterator<Map<String, String>> itA = value.iterator();
        Iterator<Map<String, String>> itB = otherVal.iterator();
        while (itA.hasNext()) {
            if (!equalMap(itA.next(), itB.next()))
                return false;
        }
        return true;
    }

    private static boolean equalMap(Map<String, String> a, Map<String, String> b) {
        if (a == null)
            return b == null;
        if (b == null)
            return false;
        if (a.size() != b.size())
            return false;
        for (Entry<String, String> e : a.entrySet()) {
            if (!Objects.equals(e.getValue(), b.get(e.getKey())))
                return false;
        }
        return true;
    }

    @Override
    public MapListSetting copy() {
        if (value == null)
            return new MapListSetting(null);
        List<Map<String, String>> copy = new ArrayList<>(value.size());
        for (Map<String, String> map : value) {
            Map<String, String> mapCopy = new LinkedHashMap<>(map);
            copy.add(Collections.unmodifiableMap(mapCopy));
        }
        return new MapListSetting(Collections.unmodifiableList(copy));
    }

    private void consistencyTest() {
        if (value == null)
            return;
        if (value.contains(null))
            throw new IllegalArgumentException("Error: Null as list element in preference setting");
        for (Map<String, String> map : value) {
            if (map.keySet().contains(null))
                throw new IllegalArgumentException("Error: Null as map key in preference setting");
            if (map.values().contains(null))
                throw new IllegalArgumentException("Error: Null as map value in preference setting");
        }
    }

    @Override
    public void visit(SettingVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public MapListSetting getNullInstance() {
        return new MapListSetting(null);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MapListSetting))
            return false;
        return equalVal(((MapListSetting) other).getValue());
    }
}
