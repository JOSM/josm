// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.text.ParseException;
import java.util.Date;

/**
 * Tries to parse a date as good as it can.
 *
 * @author Immanuel.Scholz
 */
public final class DateParser {
    
    private DateParser() {
        // Hide default constructor for utils classes
    }
    
    public static Date parse(String d) throws ParseException {
        return new PrimaryDateParser().parse(d);
    }
}
