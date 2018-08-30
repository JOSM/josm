// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.ParseException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Timezone in hours.<p>
 * TODO: should probably be replaced by {@link java.util.TimeZone}.
 * @since 14205 (extracted from {@code CorrelateGpxWithImages})
 */
public final class GpxTimezone {

    /**
     * The timezone 0.
     */
    public static final GpxTimezone ZERO = new GpxTimezone(0.0);
    private final double timezone;

    /**
     * Construcs a new {@code GpxTimezone}.
     * @param hours timezone in hours
     */
    public GpxTimezone(double hours) {
        this.timezone = hours;
    }

    /**
     * Returns the timezone in hours.
     * @return the timezone in hours
     */
    public double getHours() {
        return timezone;
    }

    /**
     * Formats time zone.
     * @return formatted time zone. Format: ±HH:MM
     */
    public String formatTimezone() {
        StringBuilder ret = new StringBuilder();

        double timezone = this.timezone;
        if (timezone < 0) {
            ret.append('-');
            timezone = -timezone;
        } else {
            ret.append('+');
        }
        ret.append((long) timezone).append(':');
        int minutes = (int) ((timezone % 1) * 60);
        if (minutes < 10) {
            ret.append('0');
        }
        ret.append(minutes);

        return ret.toString();
    }

    /**
     * Parses timezone.
     * @param timezone timezone. Expected format: ±HH:MM
     * @return timezone
     * @throws ParseException if timezone can't be parsed
     */
    public static GpxTimezone parseTimezone(String timezone) throws ParseException {
        if (timezone.isEmpty())
            return ZERO;

        Matcher m = Pattern.compile("^([\\+\\-]?)(\\d{1,2})(?:\\:([0-5]\\d))?$").matcher(timezone);

        ParseException pe = new ParseException(tr("Error while parsing timezone.\nExpected format: {0}", "±HH:MM"), 0);
        try {
            if (m.find()) {
                int sign = "-".equals(m.group(1)) ? -1 : 1;
                int hour = Integer.parseInt(m.group(2));
                int min = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
                return new GpxTimezone(sign * (hour + min / 60.0));
            }
        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
            pe.initCause(ex);
        }
        throw pe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GpxTimezone)) return false;
        GpxTimezone timezone1 = (GpxTimezone) o;
        return Double.compare(timezone1.timezone, timezone) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timezone);
    }
}
