// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.DefaultTextComponentValidator;

/**
 * Validator for OSM username.
 */
public class UserNameValidator extends DefaultTextComponentValidator {

    /**
     * Constructs a new {@code UserNameValidator}.
     * @param tc the text component used to enter username
     */
    public UserNameValidator(JTextComponent tc) {
        super(tc, tr("Please enter your OSM user name"), tr("The user name cannot be empty. Please enter your OSM user name"));
    }
}
