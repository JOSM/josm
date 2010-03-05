// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * @author Matthias Julius
 */
public class OsmIdTextField extends JTextField {

    private OsmIdValidator validator;

    public OsmIdTextField() {
        validator = OsmIdValidator.decorate(this);
    }

    public int getOsmId() {
        return validator.getOsmId();
    }

    /**
     * Validator for a changeset ID entered in a {@see JTextComponent}.
     *
     */
    static private class OsmIdValidator extends AbstractTextComponentValidator {

        static public OsmIdValidator decorate(JTextComponent tc) {
            return new OsmIdValidator(tc);
        }

        public OsmIdValidator(JTextComponent tc) {
            super(tc, false);
        }

        @Override
        public boolean isValid() {
            return getOsmId() > 0;
        }

        @Override
        public void validate() {
            if (!isValid()) {
                feedbackInvalid(tr("The current value is not a valid OSM ID. Please enter an integer value > 0"));
            } else {
                feedbackValid(tr("Please enter an integer value > 0"));
            }
        }

        public int getOsmId() {
            String value  = getComponent().getText();
            if (value == null || value.trim().length() == 0) return 0;
            try {
                int osmId = Integer.parseInt(value.trim());
                if (osmId > 0) return osmId;
                return 0;
            } catch(NumberFormatException e) {
                return 0;
            }
        }
    }
}
