package org.apache.commons.jcs.auxiliary.remote;

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

import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheFactory;
import org.apache.commons.jcs.auxiliary.AuxiliaryCache;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.behavior.IRemoteCacheAttributes;
import org.apache.commons.jcs.auxiliary.remote.server.behavior.RemoteType;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;

/**
 * The RemoteCacheFactory creates remote caches for the cache hub. It returns a no wait facade which
 * is a wrapper around a no wait. The no wait object is either an active connection to a remote
 * cache or a balking zombie if the remote cache is not accessible. It should be transparent to the
 * clients.
 */
public class RemoteCacheFactory
    extends AbstractAuxiliaryCacheFactory
{
    /** Monitor thread */
    private RemoteCacheMonitor monitor;

    /** Contains mappings of RemoteLocation instance to RemoteCacheManager instance. */
    private ConcurrentMap<RemoteLocation, RemoteCacheManager> managers;

    /** Lock for initialization of manager instances */
    private Lock managerLock;

    /**
     * For LOCAL clients we get a handle to all the failovers, but we do not register a listener
     * with them. We create the RemoteCacheManager, but we do not get a cache.
     * <p>
     * The failover runner will get a cache from the manager. When the primary is restored it will
     * tell the manager for the failover to deregister the listener.
     * <p>
     * @param iaca
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     * @return AuxiliaryCache
     */
    @Override
    public <K, V> AuxiliaryCache<K, V> createCache(
            AuxiliaryCacheAttributes iaca, ICompositeCacheManager cacheMgr,
           ICacheEventLogger cacheEventLogger, IElementSerializer elementSerializer )
    {
        RemoteCacheAttributes rca = (RemoteCacheAttributes) iaca;

        ArrayList<RemoteCacheNoWait<K,V>> noWaits = new ArrayList<>();

        switch (rca.getRemoteType())
        {
            case LOCAL:
                // a list to be turned into an array of failover server information
                ArrayList<RemoteLocation> failovers = new ArrayList<>();

                // not necessary if a failover list is defined
                // REGISTER PRIMARY LISTENER
                // if it is a primary
                boolean primaryDefined = false;
                if ( rca.getRemoteLocation() != null )
                {
                    primaryDefined = true;

                    failovers.add( rca.getRemoteLocation() );
                    RemoteCacheManager rcm = getManager( rca, cacheMgr, cacheEventLogger, elementSerializer );
                    RemoteCacheNoWait<K,V> ic = rcm.getCache( rca );
                    noWaits.add( ic );
                }

                // GET HANDLE BUT DONT REGISTER A LISTENER FOR FAILOVERS
                String failoverList = rca.getFailoverServers();
                if ( failoverList != null )
                {
                    StringTokenizer fit = new StringTokenizer( failoverList, "," );
                    int fCnt = 0;
                    while ( fit.hasMoreTokens() )
                    {
                        fCnt++;

                        String server = fit.nextToken();
                        RemoteLocation location = RemoteLocation.parseServerAndPort(server);

                        if (location != null)
                        {
                            failovers.add( location );
                            rca.setRemoteLocation(location);
                            RemoteCacheManager rcm = getManager( rca, cacheMgr, cacheEventLogger, elementSerializer );

                            // add a listener if there are none, need to tell rca what
                            // number it is at
                            if ( ( !primaryDefined && fCnt == 1 ) || noWaits.size() <= 0 )
                            {
                                RemoteCacheNoWait<K,V> ic = rcm.getCache( rca );
                                noWaits.add( ic );
                            }
                        }
                    }
                    // end while
                }
                // end if failoverList != null

                rca.setFailovers( failovers );
                break;

            case CLUSTER:
                // REGISTER LISTENERS FOR EACH SYSTEM CLUSTERED CACHEs
                StringTokenizer it = new StringTokenizer( rca.getClusterServers(), "," );
                while ( it.hasMoreElements() )
                {
                    String server = (String) it.nextElement();
                    RemoteLocation location = RemoteLocation.parseServerAndPort(server);

                    if (location != null)
                    {
                        rca.setRemoteLocation(location);
                        RemoteCacheManager rcm = getManager( rca, cacheMgr, cacheEventLogger, elementSerializer );
                        rca.setRemoteType( RemoteType.CLUSTER );
                        RemoteCacheNoWait<K,V> ic = rcm.getCache( rca );
                        noWaits.add( ic );
                    }
                }
                break;
        }

        RemoteCacheNoWaitFacade<K, V> rcnwf =
            new RemoteCacheNoWaitFacade<>(noWaits, rca, cacheEventLogger, elementSerializer, this );

        return rcnwf;
    }

    // end createCache

    /**
     * Returns an instance of RemoteCacheManager for the given connection parameters.
     * <p>
     * Host and Port uniquely identify a manager instance.
     * <p>
     * @param cattr
     *
     * @return The instance value or null if no such manager exists
     */
    public RemoteCacheManager getManager( IRemoteCacheAttributes cattr )
    {
        if ( cattr.getRemoteLocation() == null )
        {
            cattr.setRemoteLocation("", Registry.REGISTRY_PORT);
        }

        RemoteLocation loc = cattr.getRemoteLocation();
        RemoteCacheManager ins = managers.get( loc );

        return ins;
    }

    /**
     * Returns an instance of RemoteCacheManager for the given connection parameters.
     * <p>
     * Host and Port uniquely identify a manager instance.
     * <p>
     * If the connection cannot be established, zombie objects will be used for future recovery
     * purposes.
     * <p>
     * @param cattr
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     * @return The instance value, never null
     */
    public RemoteCacheManager getManager( IRemoteCacheAttributes cattr, ICompositeCacheManager cacheMgr,
                                                  ICacheEventLogger cacheEventLogger,
                                                  IElementSerializer elementSerializer )
    {
        RemoteCacheManager ins = getManager( cattr );

        if ( ins == null )
        {
            managerLock.lock();

            try
            {
                ins = managers.get( cattr.getRemoteLocation() );

                if (ins == null)
                {
                    ins = new RemoteCacheManager( cattr, cacheMgr, monitor, cacheEventLogger, elementSerializer);
                    managers.put( cattr.getRemoteLocation(), ins );
                    monitor.addManager(ins);
                }
            }
            finally
            {
                managerLock.unlock();
            }
        }

        return ins;
    }

	/**
	 * @see org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheFactory#initialize()
	 */
	@Override
	public void initialize()
	{
		super.initialize();

		managers = new ConcurrentHashMap<>();
		managerLock = new ReentrantLock();

        monitor = new RemoteCacheMonitor();
        monitor.setDaemon(true);
	}

	/**
	 * @see org.apache.commons.jcs.auxiliary.AbstractAuxiliaryCacheFactory#dispose()
	 */
	@Override
	public void dispose()
	{
		for (RemoteCacheManager manager : managers.values())
		{
			manager.release();
		}

		managers.clear();

        if (monitor != null)
        {
            monitor.notifyShutdown();
            try
            {
                monitor.join(5000);
            }
            catch (InterruptedException e)
            {
                // swallow
            }
            monitor = null;
        }

		super.dispose();
	}
}
