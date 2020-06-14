// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.tools.Logging;

/**
 * Validates dates entered as text in a {@link JTextComponent}. Validates the input
 * on the fly and gives feedback about whether the date is valid or not.
 *
 * Dates can be entered in one of four standard formats defined for the current locale.
 * @since 11326 (extracted from AdvancedChangesetQueryPanel)
 */
public class DateValidator extends AbstractTextComponentValidator {

    /**
     * Constructs a new {@code DateValidator} for the given text component.
     * @param tc text component
     */
    public DateValidator(JTextComponent tc) {
        super(tc);
    }

    /**
     * Decorates the given text component.
     * @param tc text component to decorate
     * @return new date validator attached to {@code tc}
     */
    public static DateValidator decorate(JTextComponent tc) {
        return new DateValidator(tc);
    }

    @Override
    public boolean isValid() {
        return getDate() != null;
    }

    /**
     * Returns the standard tooltip text as HTML.
     * @return the standard tooltip text as HTML
     */
    public String getStandardTooltipTextAsHtml() {
        return "<html>" + getStandardTooltipText() + "</html>";
    }

    /**
     * Returns the standard tooltip text.
     * @return the standard tooltip text
     */
    public String getStandardTooltipText() {
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        return tr(
                "Please enter a date in the usual format for your locale.<br>"
                + "Example: {0}<br>"
                + "Example: {1}<br>"
                + "Example: {2}<br>"
                + "Example: {3}<br>",
                DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now),
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(now),
                DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(now),
                DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(now)
        );
    }

    @Override
    public void validate() {
        if (!isValid()) {
            String msg = "<html>The current value isn't a valid date.<br>" + getStandardTooltipText()+ "</html>";
            feedbackInvalid(msg);
        } else {
            String msg = "<html>" + getStandardTooltipText() + "</html>";
            feedbackValid(msg);
        }
    }

    /**
     * Returns the date.
     * @return the date
     */
    public LocalDate getDate() {
        for (final FormatStyle format: FormatStyle.values()) {
            DateTimeFormatter df = DateTimeFormatter.ofLocalizedDate(format);
            try {
                return LocalDate.parse(getComponent().getText(), df);
            } catch (DateTimeParseException e) {
                // Try next format
                Logging.trace(e);
            }
        }
        return null;
    }
}
