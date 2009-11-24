// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.text.ParseException;
import java.util.Date;

/**
 * Tries to parse a date as good as it can.
 *
 * @author Immanuel.Scholz
 */
public class DateParser {
    public static Date parse(String d) throws ParseException {
        return new PrimaryDateParser().parse(d);
    }
}
