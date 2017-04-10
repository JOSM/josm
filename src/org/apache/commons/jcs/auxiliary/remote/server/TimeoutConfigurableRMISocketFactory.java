package org.apache.commons.jcs.auxiliary.remote.server;

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
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;

/**
 * This can be injected into the the remote cache server as follows:
 *
 * <pre>
 * jcs.remotecache.customrmisocketfactory=org.apache.commons.jcs.auxiliary.remote.server.TimeoutConfigurableRMISocketFactory
 * jcs.remotecache.customrmisocketfactory.readTimeout=5000
 * jcs.remotecache.customrmisocketfactory.openTimeout=5000
 * </pre>
 */
public class TimeoutConfigurableRMISocketFactory
    extends RMISocketFactory
    implements Serializable
{
    /** Don't change. */
    private static final long serialVersionUID = 1489909775271203334L;

    /** The socket read timeout */
    private int readTimeout = 5000;

    /** The socket open timeout */
    private int openTimeout = 5000;

    /**
     * @param port
     * @return ServerSocket
     * @throws IOException
     */
    @Override
    public ServerSocket createServerSocket( int port )
        throws IOException
    {
        return new ServerSocket( port );
    }

    /**
     * @param host
     * @param port
     * @return Socket
     * @throws IOException
     */
    @Override
    public Socket createSocket( String host, int port )
        throws IOException
    {
        Socket socket = new Socket();
        socket.setSoTimeout( readTimeout );
        socket.setSoLinger( false, 0 );
        socket.connect( new InetSocketAddress( host, port ), openTimeout );
        return socket;
    }

    /**
     * @param readTimeout the readTimeout to set
     */
    public void setReadTimeout( int readTimeout )
    {
        this.readTimeout = readTimeout;
    }

    /**
     * @return the readTimeout
     */
    public int getReadTimeout()
    {
        return readTimeout;
    }

    /**
     * @param openTimeout the openTimeout to set
     */
    public void setOpenTimeout( int openTimeout )
    {
        this.openTimeout = openTimeout;
    }

    /**
     * @return the openTimeout
     */
    public int getOpenTimeout()
    {
        return openTimeout;
    }
}
