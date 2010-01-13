// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;

public class UserNameValidator extends AbstractTextComponentValidator {

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
