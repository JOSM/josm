// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.search;

import java.util.Arrays;

/**
 * Search mode.
 * @since 12659 (extracted from {@code SearchAction})
 */
public enum SearchMode {
    /** replace selection */
    replace('R'),
    /** add to selection */
    add('A'),
    /** remove from selection */
    remove('D'),
    /** find in selection */
    in_selection('S');

    private final char code;

    SearchMode(char code) {
        this.code = code;
    }

    /**
     * Returns the unique character code of this mode.
     * @return the unique character code of this mode
     */
    public char getCode() {
        return code;
    }

    /**
     * Returns the search mode matching the given character code.
     * @param code character code
     * @return search mode matching the given character code
     */
    public static SearchMode fromCode(char code) {
        return Arrays.stream(values())
                .filter(mode -> mode.getCode() == code)
                .findFirst().orElse(null);
    }
}
