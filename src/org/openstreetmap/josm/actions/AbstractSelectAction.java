// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Superclass of "Select" actions in various parts of JOSM.
 * @since 7949
 */
public abstract class AbstractSelectAction extends AbstractAction {

    /**
     * Constructs a new {@code AbstractSelectAction}.
     */
    protected AbstractSelectAction() {
        putValue(NAME, tr("Select"));
        putValue(SHORT_DESCRIPTION, tr("Selects those elements on the map which are chosen on the list above."));
        new ImageProvider("dialogs", "select").getResource().attachImageIcon(this, true);
    }
}
