// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

public class ChangesetCacheSortSpecification {
    static public enum SortCriterium {
        ID,
        COMMENT,
        OPEN,
        USER,
        CREATED_AT,
        CLOSED_AT
    }

    static public enum SortDirection {
        ASCENDING,
        DESCENDING
    }

}
