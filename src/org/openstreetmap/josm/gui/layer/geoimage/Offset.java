// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.ParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.tools.JosmDecimalFormatSymbolsProvider;
import org.openstreetmap.josm.tools.Pair;

/**
 * Time offset of GPX correlation.
 * @since 11914 (extracted from {@link CorrelateGpxWithImages})
 */
public final class Offset {

    static final Offset ZERO = new Offset(0);
    private final long milliseconds;

    private Offset(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    static Offset milliseconds(long milliseconds) {
        return new Offset(milliseconds);
    }

    static Offset seconds(long seconds) {
        return new Offset(1000 * seconds);
    }

    long getMilliseconds() {
        return milliseconds;
    }

    long getSeconds() {
        return milliseconds / 1000;
    }

    String formatOffset() {
        if (milliseconds % 1000 == 0) {
            return Long.toString(milliseconds / 1000);
        } else if (milliseconds % 100 == 0) {
            return String.format(Locale.ENGLISH, "%.1f", milliseconds / 1000.);
        } else {
            return String.format(Locale.ENGLISH, "%.3f", milliseconds / 1000.);
        }
    }

    static Offset parseOffset(String offset) throws ParseException {
        String error = tr("Error while parsing offset.\nExpected format: {0}", "number");

        if (!offset.isEmpty()) {
            try {
                if (offset.startsWith("+")) {
                    offset = offset.substring(1);
                }
                return Offset.milliseconds(Math.round(JosmDecimalFormatSymbolsProvider.parseDouble(offset) * 1000));
            } catch (NumberFormatException nfe) {
                throw (ParseException) new ParseException(error, 0).initCause(nfe);
            }
        } else {
            return Offset.ZERO;
        }
    }

    int getDayOffset() {
        // Find day difference
        return (int) Math.round(((double) getMilliseconds()) / TimeUnit.DAYS.toMillis(1));
    }

    Offset withoutDayOffset() {
        return milliseconds(getMilliseconds() - TimeUnit.DAYS.toMillis(getDayOffset()));
    }

    Pair<Timezone, Offset> splitOutTimezone() {
        // In hours
        final double tz = ((double) withoutDayOffset().getSeconds()) / TimeUnit.HOURS.toSeconds(1);

        // Due to imprecise clocks we might get a "+3:28" timezone, which should obviously be 3:30 with
        // -2 minutes offset. This determines the real timezone and finds offset.
        final double timezone = (double) Math.round(tz * 2) / 2; // hours, rounded to one decimal place
        final long delta = Math.round(getMilliseconds() - timezone * TimeUnit.HOURS.toMillis(1));
        return Pair.create(new Timezone(timezone), Offset.milliseconds(delta));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Offset)) return false;
        Offset offset = (Offset) o;
        return milliseconds == offset.milliseconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(milliseconds);
    }
}
