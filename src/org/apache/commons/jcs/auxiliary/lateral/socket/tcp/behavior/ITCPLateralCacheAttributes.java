package org.apache.commons.jcs.auxiliary.lateral.socket.tcp.behavior;

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

import org.apache.commons.jcs.auxiliary.lateral.behavior.ILateralCacheAttributes;

/**
 * This interface defines functions that are particular to the TCP Lateral Cache
 * plugin. It extends the generic LateralCacheAttributes interface which in turn
 * extends the AuxiliaryCache interface.
 * <p>
 * @author Aaron Smuts
 */
public interface ITCPLateralCacheAttributes
    extends ILateralCacheAttributes
{
    /**
     * Sets the tcpServer attribute of the ILateralCacheAttributes object
     * <p>
     * @param val
     *            The new tcpServer value
     */
    void setTcpServer( String val );

    /**
     * Gets the tcpServer attribute of the ILateralCacheAttributes object
     * <p>
     * @return The tcpServer value
     */
    String getTcpServer();

    /**
     * Sets the tcpServers attribute of the ILateralCacheAttributes object
     * <p>
     * @param val
     *            The new tcpServers value
     */
    void setTcpServers( String val );

    /**
     * Gets the tcpServers attribute of the ILateralCacheAttributes object
     * <p>
     * @return The tcpServers value
     */
    String getTcpServers();

    /**
     * Sets the tcpListenerPort attribute of the ILateralCacheAttributes object
     * <p>
     * @param val
     *            The new tcpListenerPort value
     */
    void setTcpListenerPort( int val );

    /**
     * Gets the tcpListenerPort attribute of the ILateralCacheAttributes object
     * <p>
     * @return The tcpListenerPort value
     */
    int getTcpListenerPort();

    /**
     * Can setup UDP Discovery. This only works for TCp laterals right now. It
     * allows TCP laterals to find each other by broadcasting to a multicast
     * port.
     * <p>
     * @param udpDiscoveryEnabled
     *            The udpDiscoveryEnabled to set.
     */
    void setUdpDiscoveryEnabled( boolean udpDiscoveryEnabled );

    /**
     * Whether or not TCP laterals can try to find each other by multicast
     * communication.
     * <p>
     * @return Returns the udpDiscoveryEnabled.
     */
    boolean isUdpDiscoveryEnabled();

    /**
     * The port to use if UDPDiscovery is enabled.
     * <p>
     * @return Returns the udpDiscoveryPort.
     */
    int getUdpDiscoveryPort();

    /**
     * Sets the port to use if UDPDiscovery is enabled.
     * <p>
     * @param udpDiscoveryPort
     *            The udpDiscoveryPort to set.
     */
    void setUdpDiscoveryPort( int udpDiscoveryPort );

    /**
     * The address to broadcast to if UDPDiscovery is enabled.
     * <p>
     * @return Returns the udpDiscoveryAddr.
     */
    String getUdpDiscoveryAddr();

    /**
     * Sets the address to broadcast to if UDPDiscovery is enabled.
     * <p>
     * @param udpDiscoveryAddr
     *            The udpDiscoveryAddr to set.
     */
    void setUdpDiscoveryAddr( String udpDiscoveryAddr );

    /**
     * Is the lateral allowed to try and get from other laterals.
     * <p>
     * This replaces the old putOnlyMode
     * <p>
     * @param allowGet
     */
    void setAllowGet( boolean allowGet );

    /**
     * Is the lateral allowed to try and get from other laterals.
     * <p>
     * @return true if the lateral will try to get
     */
    boolean isAllowGet();

    /**
     * Is the lateral allowed to put objects to other laterals.
     * <p>
     * @param allowPut
     */
    void setAllowPut( boolean allowPut );

    /**
     * Is the lateral allowed to put objects to other laterals.
     * <p>
     * @return true if puts are allowed
     */
    boolean isAllowPut();

    /**
     * Should the client send a remove command rather than a put when update is
     * called. This is a client option, not a receiver option. This allows you
     * to prevent the lateral from serializing objects.
     * <p>
     * @param issueRemoveOnPut
     */
    void setIssueRemoveOnPut( boolean issueRemoveOnPut );

    /**
     * Should the client send a remove command rather than a put when update is
     * called. This is a client option, not a receiver option. This allows you
     * to prevent the lateral from serializing objects.
     * <p>
     * @return true if updates will result in a remove command being sent.
     */
    boolean isIssueRemoveOnPut();

    /**
     * Should the receiver try to match hashcodes. If true, the receiver will
     * see if the client supplied a hashcode. If it did, then it will try to get
     * the item locally. If the item exists, then it will compare the hashcode.
     * if they are the same, it will not remove. This isn't perfect since
     * different objects can have the same hashcode, but it is unlikely of
     * objects of the same type.
     * <p>
     * @return boolean
     */
    boolean isFilterRemoveByHashCode();

    /**
     * Should the receiver try to match hashcodes. If true, the receiver will
     * see if the client supplied a hashcode. If it did, then it will try to get
     * the item locally. If the item exists, then it will compare the hashcode.
     * if they are the same, it will not remove. This isn't perfect since
     * different objects can have the same hashcode, but it is unlikely of
     * objects of the same type.
     * <p>
     * @param filter
     */
    void setFilterRemoveByHashCode( boolean filter );

    /**
     * @param socketTimeOut the socketTimeOut to set
     */
    void setSocketTimeOut( int socketTimeOut );

    /**
     * @return the socketTimeOut
     */
    int getSocketTimeOut();

    /**
     * @param openTimeOut the openTimeOut to set
     */
    void setOpenTimeOut( int openTimeOut );

    /**
     * @return the openTimeOut
     */
    int getOpenTimeOut();
}
