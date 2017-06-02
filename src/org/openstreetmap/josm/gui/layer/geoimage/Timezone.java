// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.ParseException;
import java.util.Objects;

/**
 * Timezone in hours.<p>
 * TODO: should probably be replaced by {@link java.util.TimeZone}.
 * @since 11914 (extracted from {@link CorrelateGpxWithImages})
 */
public final class Timezone {

    static final Timezone ZERO = new Timezone(0.0);
    private final double timezone;

    Timezone(double hours) {
        this.timezone = hours;
    }

    /**
     * Returns the timezone in hours.
     * @return the timezone in hours
     */
    public double getHours() {
        return timezone;
    }

    String formatTimezone() {
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

    static Timezone parseTimezone(String timezone) throws ParseException {

        if (timezone.isEmpty())
            return ZERO;

        String error = tr("Error while parsing timezone.\nExpected format: {0}", "+H:MM");

        char sgnTimezone = '+';
        StringBuilder hTimezone = new StringBuilder();
        StringBuilder mTimezone = new StringBuilder();
        int state = 1; // 1=start/sign, 2=hours, 3=minutes.
        for (int i = 0; i < timezone.length(); i++) {
            char c = timezone.charAt(i);
            switch (c) {
                case ' ':
                    if (state != 2 || hTimezone.length() != 0)
                        throw new ParseException(error, i);
                    break;
                case '+':
                case '-':
                    if (state == 1) {
                        sgnTimezone = c;
                        state = 2;
                    } else
                        throw new ParseException(error, i);
                    break;
                case ':':
                case '.':
                    if (state == 2) {
                        state = 3;
                    } else
                        throw new ParseException(error, i);
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    switch (state) {
                        case 1:
                        case 2:
                            state = 2;
                            hTimezone.append(c);
                            break;
                        case 3:
                            mTimezone.append(c);
                            break;
                        default:
                            throw new ParseException(error, i);
                    }
                    break;
                default:
                    throw new ParseException(error, i);
            }
        }

        int h = 0;
        int m = 0;
        try {
            h = Integer.parseInt(hTimezone.toString());
            if (mTimezone.length() > 0) {
                m = Integer.parseInt(mTimezone.toString());
            }
        } catch (NumberFormatException nfe) {
            // Invalid timezone
            throw (ParseException) new ParseException(error, 0).initCause(nfe);
        }

        if (h > 12 || m > 59)
            throw new ParseException(error, 0);
        else
            return new Timezone((h + m / 60.0) * (sgnTimezone == '-' ? -1 : 1));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Timezone)) return false;
        Timezone timezone1 = (Timezone) o;
        return Double.compare(timezone1.timezone, timezone) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timezone);
    }
}
