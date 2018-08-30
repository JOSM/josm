// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.ParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.tools.JosmDecimalFormatSymbolsProvider;
import org.openstreetmap.josm.tools.Pair;

/**
 * Time offset of GPX correlation.
 * @since 14205 (extracted from {@code CorrelateGpxWithImages})
 */
public final class GpxTimeOffset {

    /**
     * The time offset 0.
     */
    public static final GpxTimeOffset ZERO = new GpxTimeOffset(0);
    private final long milliseconds;

    private GpxTimeOffset(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    /**
     * Constructs a new {@code GpxTimeOffset} from milliseconds.
     * @param milliseconds time offset in milliseconds.
     * @return new {@code GpxTimeOffset}
     */
    public static GpxTimeOffset milliseconds(long milliseconds) {
        return new GpxTimeOffset(milliseconds);
    }

    /**
     * Constructs a new {@code GpxTimeOffset} from seconds.
     * @param seconds time offset in seconds.
     * @return new {@code GpxTimeOffset}
     */
    public static GpxTimeOffset seconds(long seconds) {
        return new GpxTimeOffset(1000 * seconds);
    }

    /**
     * Get time offset in milliseconds.
     * @return time offset in milliseconds
     */
    public long getMilliseconds() {
        return milliseconds;
    }

    /**
     * Get time offset in seconds.
     * @return time offset in seconds
     */
    public long getSeconds() {
        return milliseconds / 1000;
    }

    /**
     * Formats time offset.
     * @return formatted time offset. Format: decimal number
     */
    public String formatOffset() {
        if (milliseconds % 1000 == 0) {
            return Long.toString(milliseconds / 1000);
        } else if (milliseconds % 100 == 0) {
            return String.format(Locale.ENGLISH, "%.1f", milliseconds / 1000.);
        } else {
            return String.format(Locale.ENGLISH, "%.3f", milliseconds / 1000.);
        }
    }

    /**
     * Parses time offset.
     * @param offset time offset. Format: decimal number
     * @return time offset
     * @throws ParseException if time offset can't be parsed
     */
    public static GpxTimeOffset parseOffset(String offset) throws ParseException {
        String error = tr("Error while parsing offset.\nExpected format: {0}", "number");

        if (!offset.isEmpty()) {
            try {
                if (offset.startsWith("+")) {
                    offset = offset.substring(1);
                }
                return GpxTimeOffset.milliseconds(Math.round(JosmDecimalFormatSymbolsProvider.parseDouble(offset) * 1000));
            } catch (NumberFormatException nfe) {
                throw (ParseException) new ParseException(error, 0).initCause(nfe);
            }
        } else {
            return GpxTimeOffset.ZERO;
        }
    }

    /**
     * Returns the day difference.
     * @return the day difference
     */
    public int getDayOffset() {
        // Find day difference
        return (int) Math.round(((double) getMilliseconds()) / TimeUnit.DAYS.toMillis(1));
    }

    /**
     * Returns offset without day difference.
     * @return offset without day difference
     */
    public GpxTimeOffset withoutDayOffset() {
        return milliseconds(getMilliseconds() - TimeUnit.DAYS.toMillis(getDayOffset()));
    }

    /**
     * Split out timezone and offset.
     * @return pair of timezone and offset
     */
    public Pair<GpxTimezone, GpxTimeOffset> splitOutTimezone() {
        // In hours
        final double tz = ((double) withoutDayOffset().getSeconds()) / TimeUnit.HOURS.toSeconds(1);

        // Due to imprecise clocks we might get a "+3:28" timezone, which should obviously be 3:30 with
        // -2 minutes offset. This determines the real timezone and finds offset.
        final double timezone = (double) Math.round(tz * 2) / 2; // hours, rounded to one decimal place
        final long delta = Math.round(getMilliseconds() - timezone * TimeUnit.HOURS.toMillis(1));
        return Pair.create(new GpxTimezone(timezone), GpxTimeOffset.milliseconds(delta));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GpxTimeOffset)) return false;
        GpxTimeOffset offset = (GpxTimeOffset) o;
        return milliseconds == offset.milliseconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(milliseconds);
    }
}
