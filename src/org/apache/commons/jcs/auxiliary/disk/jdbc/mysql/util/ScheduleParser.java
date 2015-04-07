package org.apache.commons.jcs.auxiliary.disk.jdbc.mysql.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * Parses the very simple schedule format.
 * <p>
 * @author Aaron Smuts
 */
public class ScheduleParser
{
    /**
     * For each date time that is separated by a comma in the
     * OptimizationSchedule, create a date and add it to an array of dates.
     * <p>
     * @param schedule
     * @return Date[]
     * @throws ScheduleFormatException
     */
    public static Date[] createDatesForSchedule( String schedule )
        throws ScheduleFormatException
    {
        if ( schedule == null )
        {
            throw new ScheduleFormatException( "Cannot create schedules for a null String." );
        }

        StringTokenizer toker = new StringTokenizer( schedule, "," );
        Date[] dates = new Date[toker.countTokens()];
        int cnt = 0;
        while ( toker.hasMoreTokens() )
        {
            String time = toker.nextToken();
            dates[cnt] = getDateForSchedule( time );
            cnt++;
        }
        return dates;
    }

    /**
     * For a single string it creates a date that is the next time this hh:mm:ss
     * combo will be seen.
     * <p>
     * @param startTime
     * @return Date
     * @throws ScheduleFormatException
     */
    public static Date getDateForSchedule( String startTime )
        throws ScheduleFormatException
    {
        if ( startTime == null )
        {
            throw new ScheduleFormatException( "Cannot create date for a null String." );
        }

        int firstColon = startTime.indexOf( ":" );
        int lastColon = startTime.lastIndexOf( ":" );
        int len = startTime.length();
        if ( firstColon == -1 || lastColon == -1 || firstColon == lastColon || lastColon == len )
        {
            String message = "StartTime [" + startTime + "] is deformed.  Unable to schedule optimizaiton.";
            throw new ScheduleFormatException( message );
        }

        Calendar cal = Calendar.getInstance();
        try
        {
            int hour = Integer.parseInt( startTime.substring( 0, firstColon ) );
            cal.set( Calendar.HOUR_OF_DAY, hour );
            int minute = Integer.parseInt( startTime.substring( firstColon + 1, lastColon ) );
            cal.set( Calendar.MINUTE, minute );
            int second = Integer.parseInt( startTime.substring( lastColon + 1, len ) );
            cal.set( Calendar.SECOND, second );
        }
        catch ( NumberFormatException e )
        {
            String message = "Problem parsing start time [" + startTime + "].  It should be in HH:MM:SS format.";
            throw new ScheduleFormatException( message );
        }

        // if the date is less than now, add a day.
        Date now = new Date();
        if ( cal.getTime().before( now ) )
        {
            cal.add( Calendar.DAY_OF_MONTH, 1 );
        }

        return cal.getTime();
    }
}
