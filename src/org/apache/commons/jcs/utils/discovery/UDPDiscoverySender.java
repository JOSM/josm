package org.apache.commons.jcs.utils.discovery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

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

import org.apache.commons.jcs.engine.CacheInfo;
import org.apache.commons.jcs.utils.discovery.UDPDiscoveryMessage.BroadcastType;
import org.apache.commons.jcs.utils.serialization.StandardSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a generic sender for the UDPDiscovery process.
 * <p>
 * @author Aaron Smuts
 */
public class UDPDiscoverySender implements AutoCloseable
{
    /** The logger. */
    private static final Log log = LogFactory.getLog( UDPDiscoverySender.class );

    /** The socket */
    private MulticastSocket localSocket;

    /** The address */
    private InetAddress multicastAddress;

    /** The port */
    private final int multicastPort;

    /** Used to serialize messages */
    private final StandardSerializer serializer = new StandardSerializer();

    /**
     * Constructor for the UDPDiscoverySender object
     * <p>
     * This sender can be used to send multiple messages.
     * <p>
     * When you are done sending, you should destroy the socket sender.
     * <p>
     * @param host
     * @param port
     * @throws IOException
     */
    public UDPDiscoverySender( String host, int port )
        throws IOException
    {
        try
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Constructing socket for sender on port [" + port + "]" );
            }
            localSocket = new MulticastSocket( port );

            // Remote address.
            multicastAddress = InetAddress.getByName( host );
        }
        catch ( IOException e )
        {
            log.error( "Could not bind to multicast address [" + host + "]", e );

            throw e;
        }

        multicastPort = port;
    }

    /**
     * Closes the socket connection.
     */
    @Override
    public void close()
    {
        try
        {
            if ( this.localSocket != null && !this.localSocket.isClosed() )
            {
                this.localSocket.close();
            }
        }
        catch ( Exception e )
        {
            log.error( "Problem closing sender", e );
        }
    }

    /**
     * Just being careful about closing the socket.
     * <p>
     * @throws Throwable
     */
    @Override
    protected void finalize()
        throws Throwable
    {
        super.finalize();
        close();
    }

    /**
     * Send messages.
     * <p>
     * @param message
     * @throws IOException
     */
    public void send( UDPDiscoveryMessage message )
        throws IOException
    {
        if ( this.localSocket == null )
        {
            throw new IOException( "Socket is null, cannot send message." );
        }

        if ( this.localSocket.isClosed() )
        {
            throw new IOException( "Socket is closed, cannot send message." );
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "sending UDPDiscoveryMessage, address [" + multicastAddress + "], port [" + multicastPort
                + "], message = " + message );
        }

        try
        {
            final byte[] bytes = serializer.serialize( message );

            // put the byte array in a packet
            final DatagramPacket packet = new DatagramPacket( bytes, bytes.length, multicastAddress, multicastPort );

            if ( log.isDebugEnabled() )
            {
                log.debug( "Sending DatagramPacket. bytes.length [" + bytes.length + "] to " + multicastAddress + ":"
                    + multicastPort );
            }

            localSocket.send( packet );
        }
        catch ( IOException e )
        {
            log.error( "Error sending message", e );
            throw e;
        }
    }

    /**
     * Ask other to broadcast their info the the multicast address. If a lateral is non receiving it
     * can use this. This is also called on startup so we can get info.
     * <p>
     * @throws IOException
     */
    public void requestBroadcast()
        throws IOException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "sending requestBroadcast " );
        }

        UDPDiscoveryMessage message = new UDPDiscoveryMessage();
        message.setRequesterId( CacheInfo.listenerId );
        message.setMessageType( BroadcastType.REQUEST );
        send( message );
    }

    /**
     * This sends a message broadcasting out that the host and port is available for connections.
     * <p>
     * It uses the vmid as the requesterDI
     * @param host
     * @param port
     * @param cacheNames
     * @throws IOException
     */
    public void passiveBroadcast( String host, int port, ArrayList<String> cacheNames )
        throws IOException
    {
        passiveBroadcast( host, port, cacheNames, CacheInfo.listenerId );
    }

    /**
     * This allows you to set the sender id. This is mainly for testing.
     * <p>
     * @param host
     * @param port
     * @param cacheNames names of the cache regions
     * @param listenerId
     * @throws IOException
     */
    protected void passiveBroadcast( String host, int port, ArrayList<String> cacheNames, long listenerId )
        throws IOException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "sending passiveBroadcast " );
        }

        UDPDiscoveryMessage message = new UDPDiscoveryMessage();
        message.setHost( host );
        message.setPort( port );
        message.setCacheNames( cacheNames );
        message.setRequesterId( listenerId );
        message.setMessageType( BroadcastType.PASSIVE );
        send( message );
    }

    /**
     * This sends a message broadcasting our that the host and port is no longer available.
     * <p>
     * It uses the vmid as the requesterID
     * <p>
     * @param host host
     * @param port port
     * @param cacheNames names of the cache regions
     * @throws IOException on error
     */
    public void removeBroadcast( String host, int port, ArrayList<String> cacheNames )
        throws IOException
    {
        removeBroadcast( host, port, cacheNames, CacheInfo.listenerId );
    }

    /**
     * This allows you to set the sender id. This is mainly for testing.
     * <p>
     * @param host host
     * @param port port
     * @param cacheNames names of the cache regions
     * @param listenerId listener ID
     * @throws IOException on error
     */
    protected void removeBroadcast( String host, int port, ArrayList<String> cacheNames, long listenerId )
        throws IOException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "sending removeBroadcast " );
        }

        UDPDiscoveryMessage message = new UDPDiscoveryMessage();
        message.setHost( host );
        message.setPort( port );
        message.setCacheNames( cacheNames );
        message.setRequesterId( listenerId );
        message.setMessageType( BroadcastType.REMOVE );
        send( message );
    }
}

/**
 * This allows us to get the byte array from an output stream.
 * <p>
 * @author asmuts
 * @created January 15, 2002
 */

class MyByteArrayOutputStream
    extends ByteArrayOutputStream
{
    /**
     * Gets the bytes attribute of the MyByteArrayOutputStream object
     * @return The bytes value
     */
    public byte[] getBytes()
    {
        return buf;
    }
}
