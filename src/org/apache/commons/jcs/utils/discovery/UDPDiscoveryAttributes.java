package org.apache.commons.jcs.utils.discovery;

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
 * Configuration properties for UDP discover service.
 * <p>
 * The service will allow out applications to find each other.
 * <p>
 * @author Aaron Smuts
 */
public final class UDPDiscoveryAttributes
    implements Cloneable
{
    /** service name */
    private String serviceName;

    /** service address */
    private String serviceAddress;

    /** service port */
    private int servicePort;

    /**
     * false -> this service instance is not ready to receive requests. true -> ready for use
     */
    private boolean isDark;

    /** default udp discovery address */
    private static final String DEFAULT_UDP_DISCOVERY_ADDRESS = "228.4.5.6";

    /** default udp discovery port */
    private static final int DEFAULT_UDP_DISCOVERY_PORT = 5678;

    /** udp discovery address */
    private String udpDiscoveryAddr = DEFAULT_UDP_DISCOVERY_ADDRESS;

    /** udp discovery port */
    private int udpDiscoveryPort = DEFAULT_UDP_DISCOVERY_PORT;

    /** default delay between sending passive broadcasts */
    private static final int DEFAULT_SEND_DELAY_SEC = 60;

    /** delay between sending passive broadcasts */
    private int sendDelaySec = DEFAULT_SEND_DELAY_SEC;

    /** default amount of time before we remove services that we haven't heard from */
    private static final int DEFAULT_MAX_IDLE_TIME_SEC = 180;

    /** amount of time before we remove services that we haven't heard from */
    private int maxIdleTimeSec = DEFAULT_MAX_IDLE_TIME_SEC;

    /**
     * @param serviceName The serviceName to set.
     */
    public void setServiceName( String serviceName )
    {
        this.serviceName = serviceName;
    }

    /**
     * @return Returns the serviceName.
     */
    public String getServiceName()
    {
        return serviceName;
    }

    /**
     * @param serviceAddress The serviceAddress to set.
     */
    public void setServiceAddress( String serviceAddress )
    {
        this.serviceAddress = serviceAddress;
    }

    /**
     * @return Returns the serviceAddress.
     */
    public String getServiceAddress()
    {
        return serviceAddress;
    }

    /**
     * @param servicePort The servicePort to set.
     */
    public void setServicePort( int servicePort )
    {
        this.servicePort = servicePort;
    }

    /**
     * @return Returns the servicePort.
     */
    public int getServicePort()
    {
        return servicePort;
    }

    /**
     * @param udpDiscoveryAddr The udpDiscoveryAddr to set.
     */
    public void setUdpDiscoveryAddr( String udpDiscoveryAddr )
    {
        this.udpDiscoveryAddr = udpDiscoveryAddr;
    }

    /**
     * @return Returns the udpDiscoveryAddr.
     */
    public String getUdpDiscoveryAddr()
    {
        return udpDiscoveryAddr;
    }

    /**
     * @param udpDiscoveryPort The udpDiscoveryPort to set.
     */
    public void setUdpDiscoveryPort( int udpDiscoveryPort )
    {
        this.udpDiscoveryPort = udpDiscoveryPort;
    }

    /**
     * @return Returns the udpDiscoveryPort.
     */
    public int getUdpDiscoveryPort()
    {
        return udpDiscoveryPort;
    }

    /**
     * @param sendDelaySec The sendDelaySec to set.
     */
    public void setSendDelaySec( int sendDelaySec )
    {
        this.sendDelaySec = sendDelaySec;
    }

    /**
     * @return Returns the sendDelaySec.
     */
    public int getSendDelaySec()
    {
        return sendDelaySec;
    }

    /**
     * @param maxIdleTimeSec The maxIdleTimeSec to set.
     */
    public void setMaxIdleTimeSec( int maxIdleTimeSec )
    {
        this.maxIdleTimeSec = maxIdleTimeSec;
    }

    /**
     * @return Returns the maxIdleTimeSec.
     */
    public int getMaxIdleTimeSec()
    {
        return maxIdleTimeSec;
    }

    /**
     * @return Returns the isDark.
     */
    public boolean isDark()
    {
        return isDark;
    }

    /**
     * @param isDark The isDark to set.
     */
    public void setDark( boolean isDark )
    {
        this.isDark = isDark;
    }

    /** @return a clone of this object */
    @Override
    public UDPDiscoveryAttributes clone()
    {
        UDPDiscoveryAttributes attributes = new UDPDiscoveryAttributes();
        attributes.setSendDelaySec( this.getSendDelaySec() );
        attributes.setMaxIdleTimeSec( this.getMaxIdleTimeSec() );
        attributes.setServiceName( this.getServiceName() );
        attributes.setServicePort( this.getServicePort() );
        attributes.setUdpDiscoveryAddr( this.getUdpDiscoveryAddr() );
        attributes.setUdpDiscoveryPort( this.getUdpDiscoveryPort() );
        attributes.setDark( this.isDark() );
        return attributes;
    }

    /**
     * @return string for debugging purposes.
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\n UDPDiscoveryAttributes" );
        buf.append( "\n ServiceName = [" + getServiceName() + "]" );
        buf.append( "\n ServiceAddress = [" + getServiceAddress() + "]" );
        buf.append( "\n ServicePort = [" + getServicePort() + "]" );
        buf.append( "\n UdpDiscoveryAddr = [" + getUdpDiscoveryAddr() + "]" );
        buf.append( "\n UdpDiscoveryPort = [" + getUdpDiscoveryPort() + "]" );
        buf.append( "\n SendDelaySec = [" + getSendDelaySec() + "]" );
        buf.append( "\n MaxIdleTimeSec = [" + getMaxIdleTimeSec() + "]" );
        buf.append( "\n IsDark = [" + isDark() + "]" );
        return buf.toString();
    }
}
