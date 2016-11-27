// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import org.openstreetmap.josm.io.ChangesetQuery;

/**
 * Defines a panel to apply a restriction to the changeset query.
 * @since 11326
 */
public interface RestrictionPanel {

    /**
     * Determines if the changeset query is valid.
     * @return {@code true} if the changeset query is valid.
     */
    boolean isValidChangesetQuery();

    /**
     * Sets the query restrictions on <code>query</code>.
     * @param query query to fill
     */
    void fillInQuery(ChangesetQuery query);

    /**
     * Display error message if a field is invalid.
     */
    void displayMessageIfInvalid();
}
