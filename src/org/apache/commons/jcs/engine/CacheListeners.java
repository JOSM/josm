package org.apache.commons.jcs.engine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.jcs.engine.behavior.ICache;
import org.apache.commons.jcs.engine.behavior.ICacheEventQueue;

/**
 * Used to associates a set of [cache listener to cache event queue] for a
 * cache.
 */
public class CacheListeners<K, V>
{
    /** The cache using the queue. */
    public final ICache<K, V> cache;

    /** Map ICacheListener to ICacheEventQueue */
    public final ConcurrentMap<Long, ICacheEventQueue<K, V>> eventQMap =
        new ConcurrentHashMap<>();

    /**
     * Constructs with the given cache.
     * <p>
     * @param cache
     */
    public CacheListeners( ICache<K, V> cache )
    {
        if ( cache == null )
        {
            throw new IllegalArgumentException( "cache must not be null" );
        }
        this.cache = cache;
    }

    /** @return info on the listeners */
    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "\n CacheListeners" );
        if ( cache != null )
        {
            buffer.append( "\n Region = " + cache.getCacheName() );
        }
        if ( eventQMap != null )
        {
            buffer.append( "\n Event Queue Map " );
            buffer.append( "\n size = " + eventQMap.size() );
            eventQMap.forEach((key, value)
                    -> buffer.append( "\n Entry: key: ").append(key)
                        .append(", value: ").append(value));
        }
        else
        {
            buffer.append( "\n No Listeners. " );
        }
        return buffer.toString();
    }
}
