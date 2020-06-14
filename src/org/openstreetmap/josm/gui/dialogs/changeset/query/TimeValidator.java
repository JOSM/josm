// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.tools.Logging;

/**
 * Validates time values entered as text in a {@link JTextComponent}. Validates the input
 * on the fly and gives feedback about whether the time value is valid or not.
 *
 * Time values can be entered in one of four standard formats defined for the current locale.
 * @since 11326 (extracted from AdvancedChangesetQueryPanel)
 */
public class TimeValidator extends AbstractTextComponentValidator {

    /**
     * Constructs a new {@code TimeValidator} for the given text component.
     * @param tc text component
     */
    public TimeValidator(JTextComponent tc) {
        super(tc);
    }

    /**
     * Decorates the given text component.
     * @param tc text component to decorate
     * @return new time validator attached to {@code tc}
     */
    public static TimeValidator decorate(JTextComponent tc) {
        return new TimeValidator(tc);
    }

    @Override
    public boolean isValid() {
        if (getComponent().getText().trim().isEmpty())
            return true;
        return getTime() != null;
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
                "Please enter a valid time in the usual format for your locale.<br>"
                + "Example: {0}<br>"
                + "Example: {1}<br>"
                + "Example: {2}<br>"
                + "Example: {3}<br>",
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(now),
                DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format(now),
                DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG).format(now),
                DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL).format(now)
        );
    }

    @Override
    public void validate() {
        if (!isValid()) {
            String msg = "<html>The current value isn't a valid time.<br>" + getStandardTooltipText() + "</html>";
            feedbackInvalid(msg);
        } else {
            String msg = "<html>" + getStandardTooltipText() + "</html>";
            feedbackValid(msg);
        }
    }

    /**
     * Returns the time.
     * @return the time
     */
    public LocalTime getTime() {
        if (getComponent().getText().trim().isEmpty())
            return LocalTime.MIDNIGHT;

        for (final FormatStyle format: FormatStyle.values()) {
            DateTimeFormatter df = DateTimeFormatter.ofLocalizedTime(format);
            try {
                return LocalTime.parse(getComponent().getText(), df);
            } catch (DateTimeParseException e) {
                // Try next format
                Logging.trace(e);
            }
        }
        return LocalTime.MIDNIGHT;
    }
}
