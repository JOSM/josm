/*
 *  Copyright (C) 2008 Petr Nejedly <P.Nejedly@sh.cvut.cz>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.openstreetmap.josm.tools;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * A static utility class dealing with parsing XML date quickly and formatting
 * a date to the XML UTC format regardless of current locale.
 *
 * @author nenik
 */
public final class DateUtils {
    private DateUtils() {}
    /**
     * A shared instance used for conversion between individual date fields
     * and long millis time. It is guarded against conflict by the class lock.
     * The shared instance is used because the construction, together
     * with the timezone lookup, is very expensive.
     */
    private static GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    private static final DatatypeFactory XML_DATE;

    static {
        calendar.setTimeInMillis(0);

        DatatypeFactory fact = null;
        try {
            fact = DatatypeFactory.newInstance();
        } catch(DatatypeConfigurationException ce) {
            ce.printStackTrace();
        }
        XML_DATE = fact;
    }

    public static synchronized Date fromString(String str) {
        // "2007-07-25T09:26:24{Z|{+|-}01:00}"
        if (checkLayout(str, "xxxx-xx-xxTxx:xx:xxZ") ||
                checkLayout(str, "xxxx-xx-xxTxx:xx:xx+xx:00") ||
                checkLayout(str, "xxxx-xx-xxTxx:xx:xx-xx:00")) {
            calendar.set(
                parsePart(str, 0, 4),
                parsePart(str, 5, 2)-1,
                parsePart(str, 8, 2),
                parsePart(str, 11, 2),
                parsePart(str, 14,2),
                parsePart(str, 17, 2));

            if (str.length() == 25) {
                int plusHr = parsePart(str, 20, 2);
                int mul = str.charAt(19) == '+' ? -3600000 : 3600000;
                calendar.setTimeInMillis(calendar.getTimeInMillis()+plusHr*mul);
            }

            return calendar.getTime();
        }

        try {
            return XML_DATE.newXMLGregorianCalendar(str).toGregorianCalendar().getTime();
        } catch (Exception ex) {
            return new Date();
        }
    }

    public static synchronized String fromDate(Date date) {
        calendar.setTime(date);
        XMLGregorianCalendar xgc = XML_DATE.newXMLGregorianCalendar(calendar);
        if (calendar.get(Calendar.MILLISECOND) == 0) xgc.setFractionalSecond(null);
        return xgc.toXMLFormat();
    }

    private static boolean checkLayout(String text, String pattern) {
        if (text.length() != pattern.length()) return false;
        for (int i=0; i<pattern.length(); i++) {
            if (pattern.charAt(i) == 'x') continue;
            if (pattern.charAt(i) != text.charAt(i)) return false;
        }
        return true;
    }

    private static int parsePart(String str, int off, int len) {
        return Integer.valueOf(str.substring(off, off+len));
    }

}
