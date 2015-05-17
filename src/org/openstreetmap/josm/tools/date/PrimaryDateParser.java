// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.date;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

/**
 * Handles a number of different date formats encountered in OSM. This is built
 * based on similar code in JOSM. This class is not threadsafe, a separate
 * instance must be created per thread.
 *
 * @author Brett Henderson
 */
public class PrimaryDateParser {
    private DatatypeFactory datatypeFactory;
    private FallbackDateParser fallbackDateParser;
    private Calendar calendar;

    /**
     * Creates a new instance.
     */
    public PrimaryDateParser() {
        // Build an xml data type factory.
        try {
            datatypeFactory = DatatypeFactory.newInstance();

        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("Unable to instantiate xml datatype factory.", e);
        }

        fallbackDateParser = new FallbackDateParser();

        calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    }

    private boolean isDateInShortStandardFormat(String date) {
        char[] dateChars;
        // We can only parse the date if it is in a very specific format.
        // eg. 2007-09-23T08:25:43Z

        if (date.length() != 20) {
            return false;
        }

        dateChars = date.toCharArray();

        // Make sure any fixed characters are in the correct place.
        if (dateChars[4] != '-') {
            return false;
        }
        if (dateChars[7] != '-') {
            return false;
        }
        if (dateChars[10] != 'T') {
            return false;
        }
        if (dateChars[13] != ':') {
            return false;
        }
        if (dateChars[16] != ':') {
            return false;
        }
        if (dateChars[19] != 'Z') {
            return false;
        }

        // Ensure all remaining characters are numbers.
        for (int i = 0; i < 4; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }
        for (int i = 5; i < 7; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }
        for (int i = 8; i < 10; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }
        for (int i = 11; i < 13; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }
        for (int i = 14; i < 16; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }
        for (int i = 17; i < 19; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }

        // No problems found so it is in the special case format.
        return true;
    }

    private boolean isDateInLongStandardFormat(String date) {
        char[] dateChars;
        // We can only parse the date if it is in a very specific format.
        // eg. 2007-09-23T08:25:43.000Z

        if (date.length() != 24) {
            return false;
        }

        dateChars = date.toCharArray();

        // Make sure any fixed characters are in the correct place.
        if (dateChars[4] != '-') {
            return false;
        }
        if (dateChars[7] != '-') {
            return false;
        }
        if (dateChars[10] != 'T') {
            return false;
        }
        if (dateChars[13] != ':') {
            return false;
        }
        if (dateChars[16] != ':') {
            return false;
        }
        if (dateChars[19] != '.') {
            return false;
        }
        if (dateChars[23] != 'Z') {
            return false;
        }

        // Ensure all remaining characters are numbers.
        for (int i = 0; i < 4; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }
        for (int i = 5; i < 7; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }
        for (int i = 8; i < 10; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }
        for (int i = 11; i < 13; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }
        for (int i = 14; i < 16; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }
        for (int i = 17; i < 19; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }
        for (int i = 20; i < 23; i++) {
            if (dateChars[i] < '0' || dateChars[i] > '9') {
                return false;
            }
        }

        // No problems found so it is in the special case format.
        return true;
    }

    private Date parseShortStandardDate(String date) {
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));
        int hour = Integer.parseInt(date.substring(11, 13));
        int minute = Integer.parseInt(date.substring(14, 16));
        int second = Integer.parseInt(date.substring(17, 19));

        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);

        return calendar.getTime();
    }

    private Date parseLongStandardDate(String date) {
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(5, 7));
        int day = Integer.parseInt(date.substring(8, 10));
        int hour = Integer.parseInt(date.substring(11, 13));
        int minute = Integer.parseInt(date.substring(14, 16));
        int second = Integer.parseInt(date.substring(17, 19));
        int millisecond = Integer.parseInt(date.substring(20, 23));

        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, millisecond);

        return calendar.getTime();
    }

    /**
     * Attempts to parse the specified date.
     *
     * @param date
     *            The date to parse.
     * @return The date.
     * @throws ParseException
     *             Occurs if the date does not match any of the supported date
     *             formats.
     */
    public Date parse(String date) throws ParseException {
        try {
            if (isDateInShortStandardFormat(date)) {
                return parseShortStandardDate(date);
            } else if (isDateInLongStandardFormat(date)) {
                return parseLongStandardDate(date);
            } else {
                return datatypeFactory.newXMLGregorianCalendar(date).toGregorianCalendar().getTime();
            }

        } catch (IllegalArgumentException e) {
            return fallbackDateParser.parse(date);
        }
    }
}
