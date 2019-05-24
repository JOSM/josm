package org.apache.commons.jcs.auxiliary.disk.indexed;

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

import java.io.Serializable;


/**
 * Used to dump out a Disk cache from disk for debugging. This is meant to be
 * run as a command line utility for
 */
public class IndexedDiskDumper
{
    /**
     * The main program for the DiskDumper class
     * <p>
     * Creates a disk cache and then calls dump, which write out the contents to
     * a debug log.
     * <p>
     * @param args
     *            The command line arguments
     */
    public static void main( String[] args )
    {
        if ( args.length != 1 )
        {
            System.out.println( "Usage: java org.apache.commons.jcs.auxiliary.disk.DiskDump <cache_name>" );
            System.exit( 0 );
        }

        IndexedDiskCacheAttributes attr = new IndexedDiskCacheAttributes();

        attr.setCacheName( args[0] );
        attr.setDiskPath( args[0] );

        IndexedDiskCache<Serializable, Serializable> dc = new IndexedDiskCache<>( attr );
        dc.dump( true );
        System.exit( 0 );
    }
}
