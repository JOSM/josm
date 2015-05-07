package org.apache.commons.jcs.admin;

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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Keeps track of the number of bytes written to it, but doesn't write them anywhere.
 */
public class CountingOnlyOutputStream
    extends OutputStream
{
    /** number of bytes passed through */
    private int count; // TODO should this be long?

    /**
     * count as we write.
     * <p>
     * @param b
     * @throws IOException
     */
    @Override
    public void write( byte[] b )
        throws IOException
    {
        this.count += b.length;
    }

    /**
     * count as we write.
     * <p>
     * @param b
     * @param off
     * @param len
     * @throws IOException
     */
    @Override
    public void write( byte[] b, int off, int len )
        throws IOException
    {
        this.count += len;
    }

    /**
     * count as we write.
     * <p>
     * @param b
     * @throws IOException
     */
    @Override
    public void write( int b )
        throws IOException
    {
        this.count++;
    }

    /**
     * The number of bytes that have passed through this stream.
     * <p>
     * @return int
     */
    public int getCount()
    {
        return this.count;
    }
}
