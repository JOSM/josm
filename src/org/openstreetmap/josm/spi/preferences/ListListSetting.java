// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.tools.StreamUtils;
import org.openstreetmap.josm.tools.Utils;

/**
 * Setting containing a {@link List} of {@code List}s of {@link String} values.
 * @since 12881 (moved from package {@code org.openstreetmap.josm.data.preferences})
 */
public class ListListSetting extends AbstractSetting<List<List<String>>> {

    /**
     * Constructs a new {@code ListListSetting} with the given value
     * @param value The setting value
     */
    public ListListSetting(List<List<String>> value) {
        super(value);
        consistencyTest();
    }

    /**
     * Convenience factory method.
     * @param value the value
     * @return a corresponding ListListSetting object
     */
    public static ListListSetting create(Collection<Collection<String>> value) {
        if (value != null) {
            List<List<String>> valueList = value.stream()
                    .map(ArrayList::new)
                    .collect(Collectors.toList());
            return new ListListSetting(valueList);
        }
        return new ListListSetting(null);
    }

    @Override
    public ListListSetting copy() {
        if (value == null)
            return new ListListSetting(null);

        List<List<String>> copy = value.stream()
                .map(Utils::toUnmodifiableList)
                .collect(StreamUtils.toUnmodifiableList());
        return new ListListSetting(copy);
    }

    private void consistencyTest() {
        if (value != null) {
            if (value.contains(null))
                throw new IllegalArgumentException("Error: Null as list element in preference setting");
            for (Collection<String> lst : value) {
                if (lst.contains(null)) {
                    throw new IllegalArgumentException("Error: Null as inner list element in preference setting");
                }
            }
        }
    }

    @Override
    public void visit(SettingVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ListListSetting getNullInstance() {
        return new ListListSetting(null);
    }
}
