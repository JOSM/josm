// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.tools.Utils;

/**
 * Setting containing a {@link List} of {@link String} values.
 * @since 9759
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
    public boolean equalVal(List<String> otherVal) {
        return Utils.equalCollection(value, otherVal);
    }

    @Override
    public ListSetting copy() {
        return ListSetting.create(value);
    }

    private void consistencyTest() {
        if (value != null && value.contains(null))
            throw new RuntimeException("Error: Null as list element in preference setting");
    }

    @Override
    public void visit(SettingVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ListSetting getNullInstance() {
        return new ListSetting(null);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ListSetting))
            return false;
        return equalVal(((ListSetting) other).getValue());
    }
}
