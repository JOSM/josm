// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;

/**
 * Validator for OSM username.
 */
public class UserNameValidator extends AbstractTextComponentValidator {

    /**
     * Constructs a new {@code UserNameValidator}.
     * @param tc the text component used to enter username
     */
    public UserNameValidator(JTextComponent tc) {
        super(tc);
    }

    @Override
    public boolean isValid() {
        return getComponent().getText().trim().length() > 0;
    }

    @Override
    public void validate() {
        if (isValid()) {
            feedbackValid(tr("Please enter your OSM user name"));
        } else {
            feedbackInvalid(tr("The user name cannot be empty. Please enter your OSM user name"));
        }
    }
}
