package org.apache.commons.jcs.utils.timing;

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

/**
 * Utility methods to help deal with thread issues.
 */
public class SleepUtil
{
    /**
     * Sleep for a specified duration in milliseconds. This method is a
     * platform-specific workaround for Windows due to its inability to resolve
     * durations of time less than approximately 10 - 16 ms.
     * <p>
     * @param milliseconds the number of milliseconds to sleep
     */
    public static void sleepAtLeast( long milliseconds )
    {
        long endTime = System.currentTimeMillis() + milliseconds;

        while ( System.currentTimeMillis() <= endTime )
        {
            try
            {
                Thread.sleep( milliseconds );
            }
            catch ( InterruptedException e )
            {
                // TODO - Do something here?
            }
        }
    }
}
