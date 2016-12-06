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
 * This is a simple timer utility.
 */
public class ElapsedTimer
{
    /** display suffix describing the unit of measure. */
    private static final String SUFFIX = "ms.";

    /**
     * Sets the start time when created.
     */
    private long timeStamp = System.currentTimeMillis();

    /**
     * Gets the time elapsed between the start time and now. The start time is reset to now.
     * Subsequent calls will get the time between then and now.
     * <p>
     * @return the elapsed time
     */
    public long getElapsedTime()
    {
        long now = System.currentTimeMillis();
        long elapsed = now - timeStamp;
        timeStamp = now;
        return elapsed;
    }

    /**
     * Returns the elapsed time with the display suffix.
     * <p>
     * @return formatted elapsed Time
     */
    public String getElapsedTimeString()
    {
        return String.valueOf( getElapsedTime() ) + SUFFIX;
    }
}
