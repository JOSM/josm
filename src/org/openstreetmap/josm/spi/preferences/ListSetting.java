// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Setting containing a {@link List} of {@link String} values.
 * @since 12881 (moved from package {@code org.openstreetmap.josm.data.preferences})
 */
public class ListSetting extends AbstractSetting<List<String>> {
    /**
     * Constructs a new {@code ListSetting} with the given value
     * @param value The setting value
     */
    public ListSetting(List<String> value) {
        super(value);
        consistencyTest();
    }

    /**
     * Convenience factory method.
     * @param value the value
     * @return a corresponding ListSetting object
     */
    public static ListSetting create(Collection<String> value) {
        return new ListSetting(value == null ? null : Collections.unmodifiableList(new ArrayList<>(value)));
    }

    @Override
    public ListSetting copy() {
        return ListSetting.create(value);
    }

    private void consistencyTest() {
        if (value != null && value.contains(null))
            throw new IllegalArgumentException("Error: Null as list element in preference setting");
    }

    @Override
    public void visit(SettingVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ListSetting getNullInstance() {
        return new ListSetting(null);
    }
}
