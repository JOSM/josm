// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Setting containing a {@link List} of {@link Map}s of {@link String} values.
 * @since 12881 (moved from package {@code org.openstreetmap.josm.data.preferences})
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
            if (!(map instanceof SortedMap) && map.containsKey(null))
                throw new IllegalArgumentException("Error: Null as map key in preference setting");
            if (map.containsValue(null))
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
}
