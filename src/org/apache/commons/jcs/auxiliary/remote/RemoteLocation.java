package org.apache.commons.jcs.auxiliary.remote;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 * Location of the RMI registry.
 */
public final class RemoteLocation
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( RemoteLocation.class );

    /** Pattern for parsing server:port */
    private static final Pattern SERVER_COLON_PORT = Pattern.compile("(\\S+)\\s*:\\s*(\\d+)");

    /** Host name */
    private final String host;

    /** Port */
    private final int port;

    /**
     * Constructor for the Location object
     * <p>
     * @param host
     * @param port
     */
    public RemoteLocation( String host, int port )
    {
        this.host = host;
        this.port = port;
    }

    /**
     * @return the host
     */
    public String getHost()
    {
        return host;
    }

    /**
     * @return the port
     */
    public int getPort()
    {
        return port;
    }

    /**
     * @param obj
     * @return true if the host and port are equal
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }
        if ( obj == null || !( obj instanceof RemoteLocation ) )
        {
            return false;
        }
        RemoteLocation l = (RemoteLocation) obj;
        if ( this.host == null )
        {
            return l.host == null && port == l.port;
        }
        return host.equals( l.host ) && port == l.port;
    }

    /**
     * @return int
     */
    @Override
    public int hashCode()
    {
        return host == null ? port : host.hashCode() ^ port;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if (this.host != null)
        {
            sb.append(this.host);
        }
        sb.append(':').append(this.port);

        return sb.toString();
    }

    /**
     * Parse remote server and port from the string representation server:port and store them in
     * a RemoteLocation object
     *
     * @param server the input string
     * @return the remote location object
     */
    public static RemoteLocation parseServerAndPort(final String server)
    {
        Matcher match = SERVER_COLON_PORT.matcher(server);

        if (match.find() && match.groupCount() == 2)
        {
            RemoteLocation location = new RemoteLocation( match.group(1), Integer.parseInt( match.group(2) ) );
            return location;
        }
        else
        {
            log.error("Invalid server descriptor: " + server);
        }

        return null;
    }
}