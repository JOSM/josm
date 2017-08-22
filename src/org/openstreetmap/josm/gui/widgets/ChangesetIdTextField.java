// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.tools.Logging;

/**
 * A text field designed to enter a single OSM changeset ID.
 * @since 5765
 */
public class ChangesetIdTextField extends AbstractIdTextField<ChangesetIdTextField.ChangesetIdValidator> {

    /**
     * Constructs a new {@link ChangesetIdTextField}
     */
    public ChangesetIdTextField() {
        super(ChangesetIdValidator.class, 10);
    }

    /**
     * Gets the entered changeset id.
     * @return The entered changeset id
     */
    public final int getChangesetId() {
        return validator.id;
    }

    /**
     * Reads the changeset id.
     * @return true if a valid changeset id has been successfully read, false otherwise
     * @see ChangesetIdValidator#readChangesetId
     */
    @Override
    public boolean readIds() {
        return validator.readChangesetId();
    }

    /**
     * Validator for a changeset ID entered in a {@link JTextComponent}.
     */
    public static class ChangesetIdValidator extends AbstractTextComponentValidator {

        private int id;

        /**
         * Constructs a new {@link ChangesetIdValidator}
         * @param tc The text component to validate
         */
        public ChangesetIdValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public boolean isValid() {
            return readChangesetId();
        }

        @Override
        public void validate() {
            if (!isValid()) {
                feedbackInvalid(tr("The current value is not a valid changeset ID. Please enter an integer value > 0"));
            } else {
                feedbackValid(tr("Please enter an integer value > 0"));
            }
        }

        /**
         * Reads the changeset id.
         * @return true if a valid changeset id has been successfully read, false otherwise
         */
        public boolean readChangesetId() {
            String value = getComponent().getText();
            if (value != null && !value.trim().isEmpty()) {
                id = 0;
                try {
                    int changesetId = Integer.parseInt(value.trim());
                    if (changesetId > 0) {
                        id = changesetId;
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // Ignored
                    Logging.trace(e);
                }
            }
            return false;
        }
    }
}
