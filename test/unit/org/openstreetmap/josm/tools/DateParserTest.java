// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;

import org.openstreetmap.josm.tools.DateParser;

public class DateParserTest extends TestCase {

	public void testWrong() throws Exception {
	    try {
	    	DateParser.parse("imi");
	    	assertTrue(false);
	    } catch (ParseException pe) {
	    }
    }
	
	public void testRight() throws Exception {
		Date d = new SimpleDateFormat("dd MM yyyy HH mm ss SSS Z").parse("23 11 2001 23 05 42 123 +0100");
		Date d2 = new Date(d.getTime()-123);
		assertEquals(d2, DateParser.parse("11/23/2001 23:05:42"));
		assertEquals(d2, DateParser.parse("11/23/2001T23:05:42"));
		assertEquals(d2, DateParser.parse("11/23/2001T23:05:42+001"));
		assertEquals(d2, DateParser.parse("2001-11-23T23:05:42+01:00"));
        assertEquals(d, DateParser.parse("11/23/2001T23:05:42.123"));
		assertEquals(d, DateParser.parse("11/23/2001T23:05:42.123+001"));
    }
}
