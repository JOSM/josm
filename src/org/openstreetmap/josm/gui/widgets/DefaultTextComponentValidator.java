// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.text.JTextComponent;

/**
 * Default text component validator that only checks that an input field is not empty.
 * @since 10073
 */
public class DefaultTextComponentValidator extends AbstractTextComponentValidator {

    private final String validFeedback;
    private final String invalidFeedback;

    /**
     * Constructs a new {@code DefaultTextComponentValidator}.
     * @param tc the text component. Must not be null.
     * @param validFeedback text displayed for valid feedback
     * @param invalidFeedback text displayed for invalid feedback
     */
    public DefaultTextComponentValidator(JTextComponent tc, String validFeedback, String invalidFeedback) {
        super(tc);
        this.validFeedback = validFeedback;
        this.invalidFeedback = invalidFeedback;
    }

    @Override
    public boolean isValid() {
        return !getComponent().getText().trim().isEmpty();
    }

    @Override
    public void validate() {
        if (isValid()) {
            feedbackValid(validFeedback);
        } else {
            feedbackInvalid(invalidFeedback);
        }
    }
}
