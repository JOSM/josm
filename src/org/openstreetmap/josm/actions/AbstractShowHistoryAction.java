// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Superclass of "History" actions in various parts of JOSM.
 * @since 16495
 */
public abstract class AbstractShowHistoryAction extends AbstractAction {
    /**
     * Constructs a new {@code AbstractShowHistoryAction}.
     */
    public AbstractShowHistoryAction() {
        putValue(NAME, tr("History"));
        putValue(SHORT_DESCRIPTION, tr("Download and show the history of the selected objects"));
        new ImageProvider("dialogs", "history").getResource().attachImageIcon(this, true);
    }
}
