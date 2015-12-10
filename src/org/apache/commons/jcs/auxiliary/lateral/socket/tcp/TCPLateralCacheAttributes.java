package org.apache.commons.jcs.auxiliary.lateral.socket.tcp;

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

import org.apache.commons.jcs.auxiliary.lateral.LateralCacheAttributes;
import org.apache.commons.jcs.auxiliary.lateral.socket.tcp.behavior.ITCPLateralCacheAttributes;

/**
 * This interface defines functions that are particular to the TCP Lateral Cache plugin. It extends
 * the generic LateralCacheAttributes interface which in turn extends the AuxiliaryCache interface.
 */
public class TCPLateralCacheAttributes
    extends LateralCacheAttributes
    implements ITCPLateralCacheAttributes
{
    /** Don't change. */
    private static final long serialVersionUID = 1077889204513905220L;

    /** default */
    private static final String DEFAULT_UDP_DISCOVERY_ADDRESS = "228.5.6.7";

    /** default */
    private static final int DEFAULT_UDP_DISCOVERY_PORT = 6789;

    /** default */
    private static final boolean DEFAULT_UDP_DISCOVERY_ENABLED = true;

    /** default */
    private static final boolean DEFAULT_ALLOW_GET = true;

    /** default */
    private static final boolean DEFAULT_ALLOW_PUT = true;

    /** default */
    private static final boolean DEFAULT_ISSUE_REMOVE_FOR_PUT = false;

    /** default */
    private static final boolean DEFAULT_FILTER_REMOVE_BY_HASH_CODE = true;

    /** default - Only block for 1 second before timing out on a read.*/
    private static final int DEFAULT_SOCKET_TIME_OUT = 1000;

    /** default - Only block for 2 seconds before timing out on startup.*/
    private static final int DEFAULT_OPEN_TIMEOUT = 2000;

    /** TCP -------------------------------------------- */
    private String tcpServers = "";

    /** used to identify the service that this manager will be operating on */
    private String tcpServer = "";

    /** The pot */
    private int tcpListenerPort = 0;

    /** udp discovery for tcp server */
    private String udpDiscoveryAddr = DEFAULT_UDP_DISCOVERY_ADDRESS;

    /** discovery port */
    private int udpDiscoveryPort = DEFAULT_UDP_DISCOVERY_PORT;

    /** discovery switch */
    private boolean udpDiscoveryEnabled = DEFAULT_UDP_DISCOVERY_ENABLED;

    /** can we put */
    private boolean allowPut = DEFAULT_ALLOW_GET;

    /** can we go laterally for a get */
    private boolean allowGet = DEFAULT_ALLOW_PUT;

    /** call remove when there is a put */
    private boolean issueRemoveOnPut = DEFAULT_ISSUE_REMOVE_FOR_PUT;

    /** don't remove it the hashcode is the same */
    private boolean filterRemoveByHashCode = DEFAULT_FILTER_REMOVE_BY_HASH_CODE;

    /** Only block for socketTimeOut seconds before timing out on a read.  */
    private int socketTimeOut = DEFAULT_SOCKET_TIME_OUT;

    /** Only block for openTimeOut seconds before timing out on startup. */
    private int openTimeOut = DEFAULT_OPEN_TIMEOUT;

    /**
     * Sets the tcpServer attribute of the ILateralCacheAttributes object
     * <p>
     * @param val The new tcpServer value
     */
    @Override
    public void setTcpServer( String val )
    {
        this.tcpServer = val;
    }

    /**
     * Gets the tcpServer attribute of the ILateralCacheAttributes object
     * <p>
     * @return The tcpServer value
     */
    @Override
    public String getTcpServer()
    {
        return this.tcpServer;
    }

    /**
     * Sets the tcpServers attribute of the ILateralCacheAttributes object
     * <p>
     * @param val The new tcpServers value
     */
    @Override
    public void setTcpServers( String val )
    {
        this.tcpServers = val;
    }

    /**
     * Gets the tcpServers attribute of the ILateralCacheAttributes object
     * <p>
     * @return The tcpServers value
     */
    @Override
    public String getTcpServers()
    {
        return this.tcpServers;
    }

    /**
     * Sets the tcpListenerPort attribute of the ILateralCacheAttributes object
     * <p>
     * @param val The new tcpListenerPort value
     */
    @Override
    public void setTcpListenerPort( int val )
    {
        this.tcpListenerPort = val;
    }

    /**
     * Gets the tcpListenerPort attribute of the ILateralCacheAttributes object
     * <p>
     * @return The tcpListenerPort value
     */
    @Override
    public int getTcpListenerPort()
    {
        return this.tcpListenerPort;
    }

    /**
     * Can setup UDP Discovery. This only works for TCp laterals right now. It allows TCP laterals
     * to find each other by broadcasting to a multicast port.
     * <p>
     * @param udpDiscoveryEnabled The udpDiscoveryEnabled to set.
     */
    @Override
    public void setUdpDiscoveryEnabled( boolean udpDiscoveryEnabled )
    {
        this.udpDiscoveryEnabled = udpDiscoveryEnabled;
    }

    /**
     * Whether or not TCP laterals can try to find each other by multicast communication.
     * <p>
     * @return Returns the udpDiscoveryEnabled.
     */
    @Override
    public boolean isUdpDiscoveryEnabled()
    {
        return this.udpDiscoveryEnabled;
    }

    /**
     * The port to use if UDPDiscovery is enabled.
     * <p>
     * @return Returns the udpDiscoveryPort.
     */
    @Override
    public int getUdpDiscoveryPort()
    {
        return this.udpDiscoveryPort;
    }

    /**
     * Sets the port to use if UDPDiscovery is enabled.
     * <p>
     * @param udpDiscoveryPort The udpDiscoveryPort to set.
     */
    @Override
    public void setUdpDiscoveryPort( int udpDiscoveryPort )
    {
        this.udpDiscoveryPort = udpDiscoveryPort;
    }

    /**
     * The address to broadcast to if UDPDiscovery is enabled.
     * <p>
     * @return Returns the udpDiscoveryAddr.
     */
    @Override
    public String getUdpDiscoveryAddr()
    {
        return this.udpDiscoveryAddr;
    }

    /**
     * Sets the address to broadcast to if UDPDiscovery is enabled.
     * <p>
     * @param udpDiscoveryAddr The udpDiscoveryAddr to set.
     */
    @Override
    public void setUdpDiscoveryAddr( String udpDiscoveryAddr )
    {
        this.udpDiscoveryAddr = udpDiscoveryAddr;
    }

    /**
     * Is the lateral allowed to try and get from other laterals.
     * <p>
     * This replaces the old putOnlyMode
     * <p>
     * @param allowGet
     */
    @Override
    public void setAllowGet( boolean allowGet )
    {
        this.allowGet = allowGet;
    }

    /**
     * Is the lateral allowed to try and get from other laterals.
     * <p>
     * @return true if the lateral will try to get
     */
    @Override
    public boolean isAllowGet()
    {
        return this.allowGet;
    }

    /**
     * Is the lateral allowed to put objects to other laterals.
     * <p>
     * @param allowPut
     */
    @Override
    public void setAllowPut( boolean allowPut )
    {
        this.allowPut = allowPut;
    }

    /**
     * Is the lateral allowed to put objects to other laterals.
     * <p>
     * @return true if puts are allowed
     */
    @Override
    public boolean isAllowPut()
    {
        return this.allowPut;
    }

    /**
     * Should the client send a remove command rather than a put when update is called. This is a
     * client option, not a receiver option. This allows you to prevent the lateral from serializing
     * objects.
     * <p>
     * @param issueRemoveOnPut
     */
    @Override
    public void setIssueRemoveOnPut( boolean issueRemoveOnPut )
    {
        this.issueRemoveOnPut = issueRemoveOnPut;
    }

    /**
     * Should the client send a remove command rather than a put when update is called. This is a
     * client option, not a receiver option. This allows you to prevent the lateral from serializing
     * objects.
     * <p>
     * @return true if updates will result in a remove command being sent.
     */
    @Override
    public boolean isIssueRemoveOnPut()
    {
        return this.issueRemoveOnPut;
    }

    /**
     * Should the receiver try to match hashcodes. If true, the receiver will see if the client
     * supplied a hashcode. If it did, then it will try to get the item locally. If the item exists,
     * then it will compare the hashcode. if they are the same, it will not remove. This isn't
     * perfect since different objects can have the same hashcode, but it is unlikely of objects of
     * the same type.
     * <p>
     * @return boolean
     */
    @Override
    public boolean isFilterRemoveByHashCode()
    {
        return this.filterRemoveByHashCode;
    }

    /**
     * Should the receiver try to match hashcodes. If true, the receiver will see if the client
     * supplied a hashcode. If it did, then it will try to get the item locally. If the item exists,
     * then it will compare the hashcode. if they are the same, it will not remove. This isn't
     * perfect since different objects can have the same hashcode, but it is unlikely of objects of
     * the same type.
     * <p>
     * @param filter
     */
    @Override
    public void setFilterRemoveByHashCode( boolean filter )
    {
        this.filterRemoveByHashCode = filter;
    }

    /**
     * @param socketTimeOut the socketTimeOut to set
     */
    @Override
    public void setSocketTimeOut( int socketTimeOut )
    {
        this.socketTimeOut = socketTimeOut;
    }

    /**
     * @return the socketTimeOut
     */
    @Override
    public int getSocketTimeOut()
    {
        return socketTimeOut;
    }

    /**
     * @param openTimeOut the openTimeOut to set
     */
    @Override
    public void setOpenTimeOut( int openTimeOut )
    {
        this.openTimeOut = openTimeOut;
    }

    /**
     * @return the openTimeOut
     */
    @Override
    public int getOpenTimeOut()
    {
        return openTimeOut;
    }

    /**
     * Used to key the instance TODO create another method for this and use toString for debugging
     * only.
     * <p>
     * @return String
     */
    @Override
    public String toString()
    {
        return this.getTcpServer() + ":" + this.getTcpListenerPort();
    }
}
