// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Setting containing a {@link List} of {@code List}s of {@link String} values.
 * @since xxx (moved from package {@code org.openstreetmap.josm.data.preferences})
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
            List<List<String>> valueList = new ArrayList<>(value.size());
            for (Collection<String> lst : value) {
                valueList.add(new ArrayList<>(lst));
            }
            return new ListListSetting(valueList);
        }
        return new ListListSetting(null);
    }

    @Override
    public ListListSetting copy() {
        if (value == null)
            return new ListListSetting(null);

        List<List<String>> copy = new ArrayList<>(value.size());
        for (Collection<String> lst : value) {
            List<String> lstCopy = new ArrayList<>(lst);
            copy.add(Collections.unmodifiableList(lstCopy));
        }
        return new ListListSetting(Collections.unmodifiableList(copy));
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
