// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.BoundingBoxSelectionPanel;
import org.openstreetmap.josm.io.ChangesetQuery;

/**
 * This is the panel for selecting whether the query should be restricted to a specific bounding box.
 * @since 11326 (extracted from AdvancedChangesetQueryPanel)
 */
public class BBoxRestrictionPanel extends BoundingBoxSelectionPanel implements RestrictionPanel {

    /**
     * Constructs a new {@code BBoxRestrictionPanel}.
     */
    public BBoxRestrictionPanel() {
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                )
        ));
    }

    /**
     * Determines if the changeset query bbox is valid.
     * @return {@code true} if the changeset query bbox is defined.
     */
    @Override
    public boolean isValidChangesetQuery() {
        return getBoundingBox() != null;
    }

    /**
     * Sets the query restrictions on <code>query</code> for bbox based restrictions.
     * @param query query to fill
     */
    @Override
    public void fillInQuery(ChangesetQuery query) {
        if (!isValidChangesetQuery())
            throw new IllegalStateException(tr("Cannot restrict the changeset query to a specific bounding box. The input is invalid."));
        query.inBbox(getBoundingBox());
    }

    @Override
    public void displayMessageIfInvalid() {
        if (isValidChangesetQuery())
            return;
        HelpAwareOptionPane.showOptionDialog(
                this,
                tr(
                        "<html>Please enter valid longitude/latitude values to restrict<br>" +
                        "the changeset query to a specific bounding box.</html>"
                ),
                tr("Invalid bounding box"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Dialog/ChangesetQueryDialog#InvalidBoundingBox")
        );
    }
}
