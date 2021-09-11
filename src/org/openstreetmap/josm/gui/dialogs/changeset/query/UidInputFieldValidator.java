// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.tools.Utils;

/**
 * Validator for user ids entered in a {@link JTextComponent}.
 * @since 11326 (extracted from AdvancedChangesetQueryPanel)
 */
public class UidInputFieldValidator extends AbstractTextComponentValidator {

    /**
     * Constructs a new {@code TimeValidator} for the given text component.
     * @param tc text component
     */
    public UidInputFieldValidator(JTextComponent tc) {
        super(tc);
    }

    /**
     * Decorates the given text component.
     * @param tc text component to decorate
     * @return new uid validator attached to {@code tc}
     */
    public static UidInputFieldValidator decorate(JTextComponent tc) {
        return new UidInputFieldValidator(tc);
    }

    @Override
    public boolean isValid() {
        return getUid() > 0;
    }

    @Override
    public void validate() {
        String value = getComponent().getText();
        if (Utils.isBlank(value)) {
            feedbackInvalid("");
            return;
        }
        try {
            int uid = Integer.parseInt(value);
            if (uid <= 0) {
                feedbackInvalid(tr("The current value is not a valid user ID. Please enter an integer value > 0"));
                return;
            }
        } catch (NumberFormatException e) {
            feedbackInvalid(tr("The current value is not a valid user ID. Please enter an integer value > 0"));
            return;
        }
        feedbackValid(tr("Please enter an integer value > 0"));
    }

    /**
     * Returns the user identifier.
     * @return the user identifier
     */
    public int getUid() {
        String value = getComponent().getText();
        if (Utils.isBlank(value)) return 0;
        try {
            int uid = Integer.parseInt(value.trim());
            if (uid > 0)
                return uid;
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
