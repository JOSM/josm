// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;


import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test 
import org.openstreetmap.josm.io.ChangesetQuery.ChangesetQueryUrlException;
import org.openstreetmap.josm.io.ChangesetQuery.ChangesetQueryUrlParser;


import static org.junit.Assert.*;

class ChangesetQueryUrlParserTest {
	final shouldFail = new GroovyTestCase().&shouldFail
	
	@Test
	public void test_constructor() {
	    ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
	}
	
	@Test
	public void test_parse_basic() {
		ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
		
		// OK
		parser.parse ""
		
		// should be OK
		ChangesetQuery q = parser.parse(null)
		assert q != null
		
		// should be OK
		q = parser.parse("")
		assert q != null
	}
	
	@Test 
	public void test_uid() {
		ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
		def ChangesetQuery q
		
		// OK
		q = parser.parse("uid=1234")
		assert q != null
		
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("uid=0")				
		}
		
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("uid=-1")               
		}
		
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("uid=abc")               
		}
	}
	
	@Test 
	public void test_display_name() {
		ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
		def ChangesetQuery q
		
		// OK
		q = parser.parse("display_name=abcd")
		assert q != null
		assert q.@userName == "abcd"
	}
	
	
	@Test 
	public void test_open() {
		ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
		def ChangesetQuery q
		
		// OK
		q = parser.parse("open=true")
		assert q != null
		assert q.@open == true
		
		// OK
		q = parser.parse("open=false")
		assert q != null
		assert q.@open == false
		
		// illegal value for open 
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("open=abcd")               
		}   		
	}
	
	@Test 
	public void test_closed() {
		ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
		def ChangesetQuery q
		
		// OK
		q = parser.parse("closed=true")
		assert q != null
		assert q.@closed == true
		
		// OK
		q = parser.parse("closed=false")
		assert q != null
		assert q.@closed == false
		
		// illegal value for open 
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("closed=abcd")               
		}           
	}
	
	
	@Test 
	public void test_uid_and_display_name() {
		ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
		def ChangesetQuery q
		
		// we can't have both an uid and a display name 
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("uid=1&display_name=abcd")               
		}		
	}
	
	@Test 
	public void test_time() {
		ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
		def ChangesetQuery q
		
		// OK
		q = parser.parse("time=2009-12-25T10:00:00Z")
		assert q != null
		assert q.@closedAfter != null   
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT+0"));
		cal.setTime(q.@closedAfter);
		assert cal.get(Calendar.YEAR) == 2009
		assert cal.get(Calendar.MONTH) == 11 // calendar is 0-based
		assert cal.get(Calendar.DAY_OF_MONTH) == 25
		assert cal.get(Calendar.HOUR_OF_DAY) == 10
		assert cal.get(Calendar.MINUTE) == 0
		assert cal.get(Calendar.SECOND) == 0
		
		// OK
		q = parser.parse("time=2009-12-25T10:00:00Z,2009-11-25T10:00:00Z")
		assert q!= null
		assert q.@closedAfter != null
		assert q.@createdBefore != null
		
		// should fail
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("time=asdf")               
		}   		
	}
	
	@Test 
	public void test_bbox() {
		ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
		def ChangesetQuery q
		
		// OK
		q = parser.parse("bbox=-1,-1,1,1")
		assert q != null
		assert q.@bounds != null
		
		// should fail
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("bbox=-91,-1,1,1")               
		}           
				
		// should fail
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("bbox=-1,-181,1,1")               
		}           
		
		// should fail
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("bbox=-1,-1,91,1")               
		}           
		// should fail
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("bbox=-1,-1,1,181")               
		}  
		// should fail
		shouldFail(ChangesetQueryUrlException) {
			q = parser.parse("bbox=-1,-1,1")               
		}  
	}

    @Test
    public void test_changeset_ids() {
        ChangesetQueryUrlParser parser = new ChangesetQueryUrlParser();
        def ChangesetQuery q

        // OK
        q = parser.parse("changesets=1,2,3")
        assert q != null
        assert q.@changesetIds.containsAll(Arrays.asList(1L, 2L, 3L))
        assert q.@changesetIds.size() == 3

        // OK
        q = parser.parse("changesets=1,2,3,4,1")
        assert q != null
        assert q.@changesetIds.containsAll(Arrays.asList(1L, 2L, 3L, 4L))
        assert q.@changesetIds.size() == 4

        // OK
        q = parser.parse("changesets=")
        assert q != null
        assert q.@changesetIds.size() == 0

        // should fail
        shouldFail(ChangesetQueryUrlException) {
            q = parser.parse("changesets=foo")
        }
    }
}
