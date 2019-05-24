package org.apache.commons.jcs.auxiliary.disk.jdbc;

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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.commons.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.commons.jcs.auxiliary.disk.AbstractDiskCache;
import org.apache.commons.jcs.auxiliary.disk.jdbc.dsfactory.DataSourceFactory;
import org.apache.commons.jcs.engine.CacheConstants;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEvent;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.engine.stats.StatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStatElement;
import org.apache.commons.jcs.engine.stats.behavior.IStats;
import org.apache.commons.jcs.utils.serialization.StandardSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the jdbc disk cache plugin.
 * <p>
 * It expects a table created by the following script. The table name is configurable.
 * <p>
 *
 * <pre>
 *                       drop TABLE JCS_STORE;
 *                       CREATE TABLE JCS_STORE
 *                       (
 *                       CACHE_KEY                  VARCHAR(250)          NOT NULL,
 *                       REGION                     VARCHAR(250)          NOT NULL,
 *                       ELEMENT                    BLOB,
 *                       CREATE_TIME                TIMESTAMP,
 *                       UPDATE_TIME_SECONDS        BIGINT,
 *                       MAX_LIFE_SECONDS           BIGINT,
 *                       SYSTEM_EXPIRE_TIME_SECONDS BIGINT,
 *                       IS_ETERNAL                 CHAR(1),
 *                       PRIMARY KEY (CACHE_KEY, REGION)
 *                       );
 * </pre>
 * <p>
 * The cleanup thread will delete non eternal items where (now - create time) &gt; max life seconds *
 * 1000
 * <p>
 * To speed up the deletion the SYSTEM_EXPIRE_TIME_SECONDS is used instead. It is recommended that
 * an index be created on this column is you will have over a million records.
 * <p>
 * @author Aaron Smuts
 */
public class JDBCDiskCache<K, V>
    extends AbstractDiskCache<K, V>
{
    /** The local logger. */
    private static final Log log = LogFactory.getLog( JDBCDiskCache.class );

    /** custom serialization */
    private IElementSerializer elementSerializer = new StandardSerializer();

    /** configuration */
    private JDBCDiskCacheAttributes jdbcDiskCacheAttributes;

    /** # of times update was called */
    private AtomicInteger updateCount = new AtomicInteger(0);

    /** # of times get was called */
    private AtomicInteger getCount = new AtomicInteger(0);

    /** # of times getMatching was called */
    private AtomicInteger getMatchingCount = new AtomicInteger(0);

    /** if count % interval == 0 then log */
    private static final int LOG_INTERVAL = 100;

    /** db connection pool */
    private DataSourceFactory dsFactory = null;

    /** tracks optimization */
    private TableState tableState;

    /**
     * Constructs a JDBC Disk Cache for the provided cache attributes. The table state object is
     * used to mark deletions.
     * <p>
     * @param cattr the configuration object for this cache
     * @param dsFactory the DataSourceFactory for this cache
     * @param tableState an object to track table operations
     * @param compositeCacheManager the global cache manager
     */
    public JDBCDiskCache( JDBCDiskCacheAttributes cattr, DataSourceFactory dsFactory, TableState tableState,
                          ICompositeCacheManager compositeCacheManager )
    {
        super( cattr );

        setTableState( tableState );
        setJdbcDiskCacheAttributes( cattr );

        if ( log.isInfoEnabled() )
        {
            log.info( "jdbcDiskCacheAttributes = " + getJdbcDiskCacheAttributes() );
        }

        // This initializes the pool access.
        this.dsFactory = dsFactory;

        // Initialization finished successfully, so set alive to true.
        setAlive(true);
    }

    /**
     * Inserts or updates. By default it will try to insert. If the item exists we will get an
     * error. It will then update. This behavior is configurable. The cache can be configured to
     * check before inserting.
     * <p>
     * @param ce
     */
    @Override
    protected void processUpdate( ICacheElement<K, V> ce )
    {
    	updateCount.incrementAndGet();

        if ( log.isDebugEnabled() )
        {
            log.debug( "updating, ce = " + ce );
        }

        try (Connection con = getDataSource().getConnection())
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Putting [" + ce.getKey() + "] on disk." );
            }

            byte[] element;

            try
            {
                element = getElementSerializer().serialize( ce );
            }
            catch ( IOException e )
            {
                log.error( "Could not serialize element", e );
                return;
            }

            insertOrUpdate( ce, con, element );
        }
        catch ( SQLException e )
        {
            log.error( "Problem getting connection.", e );
        }

        if ( log.isInfoEnabled() )
        {
            if ( updateCount.get() % LOG_INTERVAL == 0 )
            {
                // TODO make a log stats method
                log.info( "Update Count [" + updateCount + "]" );
            }
        }
    }

    /**
     * If test before insert it true, we check to see if the element exists. If the element exists
     * we will update. Otherwise, we try inserting.  If this fails because the item exists, we will
     * update.
     * <p>
     * @param ce
     * @param con
     * @param element
     */
    private void insertOrUpdate( ICacheElement<K, V> ce, Connection con, byte[] element )
    {
        boolean exists = false;

        // First do a query to determine if the element already exists
        if ( this.getJdbcDiskCacheAttributes().isTestBeforeInsert() )
        {
            exists = doesElementExist( ce, con );
        }

        // If it doesn't exist, insert it, otherwise update
        if ( !exists )
        {
            exists = insertRow( ce, con, element );
        }

        // update if it exists.
        if ( exists )
        {
            updateRow( ce, con, element );
        }
    }

    /**
     * This inserts a new row in the database.
     * <p>
     * @param ce
     * @param con
     * @param element
     * @return true if the insertion fails because the record exists.
     */
    private boolean insertRow( ICacheElement<K, V> ce, Connection con, byte[] element )
    {
        boolean exists = false;
        String sqlI = "insert into "
                + getJdbcDiskCacheAttributes().getTableName()
                + " (CACHE_KEY, REGION, ELEMENT, MAX_LIFE_SECONDS, IS_ETERNAL, CREATE_TIME, UPDATE_TIME_SECONDS, SYSTEM_EXPIRE_TIME_SECONDS) "
                + " values (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement psInsert = con.prepareStatement( sqlI ))
        {
            psInsert.setString( 1, (String) ce.getKey() );
            psInsert.setString( 2, this.getCacheName() );
            psInsert.setBytes( 3, element );
            psInsert.setLong( 4, ce.getElementAttributes().getMaxLife() );
            if ( ce.getElementAttributes().getIsEternal() )
            {
                psInsert.setString( 5, "T" );
            }
            else
            {
                psInsert.setString( 5, "F" );
            }
            Timestamp createTime = new Timestamp( ce.getElementAttributes().getCreateTime() );
            psInsert.setTimestamp( 6, createTime );

            long now = System.currentTimeMillis() / 1000;
            psInsert.setLong( 7, now );

            long expireTime = now + ce.getElementAttributes().getMaxLife();
            psInsert.setLong( 8, expireTime );

            psInsert.execute();
        }
        catch ( SQLException e )
        {
            if ("23000".equals(e.getSQLState()))
            {
                exists = true;
            }
            else
            {
                log.error( "Could not insert element", e );
            }

            // see if it exists, if we didn't already
            if ( !exists && !this.getJdbcDiskCacheAttributes().isTestBeforeInsert() )
            {
                exists = doesElementExist( ce, con );
            }
        }

        return exists;
    }

    /**
     * This updates a row in the database.
     * <p>
     * @param ce
     * @param con
     * @param element
     */
    private void updateRow( ICacheElement<K, V> ce, Connection con, byte[] element )
    {
        String sqlU = "update " + getJdbcDiskCacheAttributes().getTableName()
                + " set ELEMENT  = ?, CREATE_TIME = ?, UPDATE_TIME_SECONDS = ?, " + " SYSTEM_EXPIRE_TIME_SECONDS = ? "
                + " where CACHE_KEY = ? and REGION = ?";

        try (PreparedStatement psUpdate = con.prepareStatement( sqlU ))
        {
            psUpdate.setBytes( 1, element );

            Timestamp createTime = new Timestamp( ce.getElementAttributes().getCreateTime() );
            psUpdate.setTimestamp( 2, createTime );

            long now = System.currentTimeMillis() / 1000;
            psUpdate.setLong( 3, now );

            long expireTime = now + ce.getElementAttributes().getMaxLife();
            psUpdate.setLong( 4, expireTime );

            psUpdate.setString( 5, (String) ce.getKey() );
            psUpdate.setString( 6, this.getCacheName() );
            psUpdate.execute();

            if ( log.isDebugEnabled() )
            {
                log.debug( "ran update " + sqlU );
            }
        }
        catch ( SQLException e2 )
        {
            log.error( "e2 sql [" + sqlU + "] Exception: ", e2 );
        }
    }

    /**
     * Does an element exist for this key?
     * <p>
     * @param ce the cache element
     * @param con a database connection
     * @return boolean
     */
    protected boolean doesElementExist( ICacheElement<K, V> ce, Connection con )
    {
        boolean exists = false;
        // don't select the element, since we want this to be fast.
        String sqlS = "select CACHE_KEY from " + getJdbcDiskCacheAttributes().getTableName()
            + " where REGION = ? and CACHE_KEY = ?";

        try (PreparedStatement psSelect = con.prepareStatement( sqlS ))
        {
            psSelect.setString( 1, this.getCacheName() );
            psSelect.setString( 2, (String) ce.getKey() );

            try (ResultSet rs = psSelect.executeQuery())
            {
                exists = rs.next();
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( "[" + ce.getKey() + "] existing status is " + exists );
            }
        }
        catch ( SQLException e )
        {
            log.error( "Problem looking for item before insert.", e );
        }

        return exists;
    }

    /**
     * Queries the database for the value. If it gets a result, the value is deserialized.
     * <p>
     * @param key
     * @return ICacheElement
     * @see org.apache.commons.jcs.auxiliary.disk.AbstractDiskCache#get(Object)
     */
    @Override
    protected ICacheElement<K, V> processGet( K key )
    {
    	getCount.incrementAndGet();

        if ( log.isDebugEnabled() )
        {
            log.debug( "Getting [" + key + "] from disk" );
        }

        if ( !isAlive() )
        {
            return null;
        }

        ICacheElement<K, V> obj = null;

        byte[] data = null;
        try
        {
            // region, key
            String selectString = "select ELEMENT from " + getJdbcDiskCacheAttributes().getTableName()
                + " where REGION = ? and CACHE_KEY = ?";

            try (Connection con = getDataSource().getConnection())
            {
                try (PreparedStatement psSelect = con.prepareStatement( selectString ))
                {
                    psSelect.setString( 1, this.getCacheName() );
                    psSelect.setString( 2, key.toString() );

                    try (ResultSet rs = psSelect.executeQuery())
                    {
                        if ( rs.next() )
                        {
                            data = rs.getBytes( 1 );
                        }
                        if ( data != null )
                        {
                            try
                            {
                                // USE THE SERIALIZER
                                obj = getElementSerializer().deSerialize( data, null );
                            }
                            catch ( IOException ioe )
                            {
                                log.error( "Problem getting item for key [" + key + "]", ioe );
                            }
                            catch ( Exception e )
                            {
                                log.error( "Problem getting item for key [" + key + "]", e );
                            }
                        }
                    }
                }
            }
        }
        catch ( SQLException sqle )
        {
            log.error( "Caught a SQL exception trying to get the item for key [" + key + "]", sqle );
        }

        if ( log.isInfoEnabled() )
        {
            if ( getCount.get() % LOG_INTERVAL == 0 )
            {
                // TODO make a log stats method
                log.info( "Get Count [" + getCount + "]" );
            }
        }
        return obj;
    }

    /**
     * This will run a like query. It will try to construct a usable query but different
     * implementations will be needed to adjust the syntax.
     * <p>
     * @param pattern
     * @return key,value map
     */
    @Override
    protected Map<K, ICacheElement<K, V>> processGetMatching( String pattern )
    {
    	getMatchingCount.incrementAndGet();

        if ( log.isDebugEnabled() )
        {
            log.debug( "Getting [" + pattern + "] from disk" );
        }

        if ( !isAlive() )
        {
            return null;
        }

        Map<K, ICacheElement<K, V>> results = new HashMap<>();

        try
        {
            // region, key
            String selectString = "select CACHE_KEY, ELEMENT from " + getJdbcDiskCacheAttributes().getTableName()
                + " where REGION = ? and CACHE_KEY like ?";

            try (Connection con = getDataSource().getConnection())
            {
                try (PreparedStatement psSelect = con.prepareStatement( selectString ))
                {
                    psSelect.setString( 1, this.getCacheName() );
                    psSelect.setString( 2, constructLikeParameterFromPattern( pattern ) );

                    try (ResultSet rs = psSelect.executeQuery())
                    {
                        while ( rs.next() )
                        {
                            String key = rs.getString( 1 );
                            byte[] data = rs.getBytes( 2 );
                            if ( data != null )
                            {
                                try
                                {
                                    // USE THE SERIALIZER
                                    ICacheElement<K, V> value = getElementSerializer().deSerialize( data, null );
                                    results.put( (K) key, value );
                                }
                                catch ( IOException ioe )
                                {
                                    log.error( "Problem getting items for pattern [" + pattern + "]", ioe );
                                }
                                catch ( Exception e )
                                {
                                    log.error( "Problem getting items for pattern [" + pattern + "]", e );
                                }
                            }
                        }
                    }
                }
            }
        }
        catch ( SQLException sqle )
        {
            log.error( "Caught a SQL exception trying to get items for pattern [" + pattern + "]", sqle );
        }

        if ( log.isInfoEnabled() )
        {
            if ( getMatchingCount.get() % LOG_INTERVAL == 0 )
            {
                // TODO make a log stats method
                log.info( "Get Matching Count [" + getMatchingCount + "]" );
            }
        }
        return results;
    }

    /**
     * @param pattern
     * @return String to use in the like query.
     */
    public String constructLikeParameterFromPattern( String pattern )
    {
        String likePattern = pattern.replaceAll( "\\.\\+", "%" );
        likePattern = likePattern.replaceAll( "\\.", "_" );

        if ( log.isDebugEnabled() )
        {
            log.debug( "pattern = [" + likePattern + "]" );
        }

        return likePattern;
    }

    /**
     * Returns true if the removal was successful; or false if there is nothing to remove. Current
     * implementation always results in a disk orphan.
     * <p>
     * @param key
     * @return boolean
     */
    @Override
    protected boolean processRemove( K key )
    {
        // remove single item.
        String sql = "delete from " + getJdbcDiskCacheAttributes().getTableName()
            + " where REGION = ? and CACHE_KEY = ?";

        try (Connection con = getDataSource().getConnection())
        {
            boolean partial = false;
            if ( key instanceof String && key.toString().endsWith( CacheConstants.NAME_COMPONENT_DELIMITER ) )
            {
                // remove all keys of the same name group.
                sql = "delete from " + getJdbcDiskCacheAttributes().getTableName()
                    + " where REGION = ? and CACHE_KEY like ?";
                partial = true;
            }

            try (PreparedStatement psSelect = con.prepareStatement( sql ))
            {
                psSelect.setString( 1, this.getCacheName() );
                if ( partial )
                {
                    psSelect.setString( 2, key.toString() + "%" );
                }
                else
                {
                    psSelect.setString( 2, key.toString() );
                }

                psSelect.executeUpdate();

                setAlive(true);
            }
            catch ( SQLException e )
            {
                log.error( "Problem creating statement. sql [" + sql + "]", e );
                setAlive(false);
            }
        }
        catch ( SQLException e )
        {
            log.error( "Problem updating cache.", e );
            reset();
        }
        return false;
    }

    /**
     * This should remove all elements. The auxiliary can be configured to forbid this behavior. If
     * remove all is not allowed, the method balks.
     */
    @Override
    protected void processRemoveAll()
    {
        // it should never get here from the abstract disk cache.
        if ( this.jdbcDiskCacheAttributes.isAllowRemoveAll() )
        {
            try (Connection con = getDataSource().getConnection())
            {
                String sql = "delete from " + getJdbcDiskCacheAttributes().getTableName() + " where REGION = ?";

                try (PreparedStatement psDelete = con.prepareStatement( sql ))
                {
                    psDelete.setString( 1, this.getCacheName() );
                    setAlive(true);
                    psDelete.executeUpdate();
                }
                catch ( SQLException e )
                {
                    log.error( "Problem creating statement.", e );
                    setAlive(false);
                }
            }
            catch ( Exception e )
            {
                log.error( "Problem removing all.", e );
                reset();
            }
        }
        else
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "RemoveAll was requested but the request was not fulfilled: allowRemoveAll is set to false." );
            }
        }
    }

    /**
     * Removed the expired. (now - create time) &gt; max life seconds * 1000
     * <p>
     * @return the number deleted
     */
    protected int deleteExpired()
    {
        int deleted = 0;

        try (Connection con = getDataSource().getConnection())
        {
            getTableState().setState( TableState.DELETE_RUNNING );

            long now = System.currentTimeMillis() / 1000;

            // This is to slow when we push over a million records
            // String sql = "delete from " +
            // getJdbcDiskCacheAttributes().getTableName() + " where REGION = '"
            // + this.getCacheName() + "' and IS_ETERNAL = 'F' and (" + now
            // + " - UPDATE_TIME_SECONDS) > MAX_LIFE_SECONDS";

            String sql = "delete from " + getJdbcDiskCacheAttributes().getTableName()
                + " where IS_ETERNAL = ? and REGION = ? and ? > SYSTEM_EXPIRE_TIME_SECONDS";

            try (PreparedStatement psDelete = con.prepareStatement( sql ))
            {
                psDelete.setString( 1, "F" );
                psDelete.setString( 2, this.getCacheName() );
                psDelete.setLong( 3, now );

                setAlive(true);

                deleted = psDelete.executeUpdate();
            }
            catch ( SQLException e )
            {
                log.error( "Problem creating statement.", e );
                setAlive(false);
            }

            logApplicationEvent( getAuxiliaryCacheAttributes().getName(), "deleteExpired",
                                 "Deleted expired elements.  URL: " + getDiskLocation() );
        }
        catch ( Exception e )
        {
            logError( getAuxiliaryCacheAttributes().getName(), "deleteExpired", e.getMessage() + " URL: "
                + getDiskLocation() );
            log.error( "Problem removing expired elements from the table.", e );
            reset();
        }
        finally
        {
            getTableState().setState( TableState.FREE );
        }

        return deleted;
    }

    /**
     * Typically this is used to handle errors by last resort, force content update, or removeall
     */
    public void reset()
    {
        // nothing
    }

    /** Shuts down the pool */
    @Override
    public void processDispose()
    {
        ICacheEvent<K> cacheEvent = createICacheEvent( getCacheName(), (K)"none", ICacheEventLogger.DISPOSE_EVENT );
        try
        {
            try
            {
            	dsFactory.close();
            }
            catch ( SQLException e )
            {
                log.error( "Problem shutting down.", e );
            }
        }
        finally
        {
            logICacheEvent( cacheEvent );
        }
    }

    /**
     * Returns the current cache size. Just does a count(*) for the region.
     * <p>
     * @return The size value
     */
    @Override
    public int getSize()
    {
        int size = 0;

        // region, key
        String selectString = "select count(*) from " + getJdbcDiskCacheAttributes().getTableName()
            + " where REGION = ?";

        try (Connection con = getDataSource().getConnection())
        {
            try (PreparedStatement psSelect = con.prepareStatement( selectString ))
            {
                psSelect.setString( 1, this.getCacheName() );

                try (ResultSet rs = psSelect.executeQuery())
                {
                    if ( rs.next() )
                    {
                        size = rs.getInt( 1 );
                    }
                }
            }
        }
        catch ( SQLException e )
        {
            log.error( "Problem getting size.", e );
        }

        return size;
    }

    /**
     * Return the keys in this cache.
     * <p>
     * @see org.apache.commons.jcs.auxiliary.disk.AbstractDiskCache#getKeySet()
     */
    @Override
    public Set<K> getKeySet() throws IOException
    {
        throw new UnsupportedOperationException( "Groups not implemented." );
        // return null;
    }

    /**
     * @param elementSerializer The elementSerializer to set.
     */
    @Override
    public void setElementSerializer( IElementSerializer elementSerializer )
    {
        this.elementSerializer = elementSerializer;
    }

    /**
     * @return Returns the elementSerializer.
     */
    @Override
    public IElementSerializer getElementSerializer()
    {
        return elementSerializer;
    }

    /**
     * @param jdbcDiskCacheAttributes The jdbcDiskCacheAttributes to set.
     */
    protected void setJdbcDiskCacheAttributes( JDBCDiskCacheAttributes jdbcDiskCacheAttributes )
    {
        this.jdbcDiskCacheAttributes = jdbcDiskCacheAttributes;
    }

    /**
     * @return Returns the jdbcDiskCacheAttributes.
     */
    protected JDBCDiskCacheAttributes getJdbcDiskCacheAttributes()
    {
        return jdbcDiskCacheAttributes;
    }

    /**
     * @return Returns the AuxiliaryCacheAttributes.
     */
    @Override
    public AuxiliaryCacheAttributes getAuxiliaryCacheAttributes()
    {
        return this.getJdbcDiskCacheAttributes();
    }

    /**
     * Extends the parent stats.
     * <p>
     * @return IStats
     */
    @Override
    public IStats getStatistics()
    {
        IStats stats = super.getStatistics();
        stats.setTypeName( "JDBC/Abstract Disk Cache" );

        List<IStatElement<?>> elems = stats.getStatElements();

        elems.add(new StatElement<>( "Update Count", updateCount ) );
        elems.add(new StatElement<>( "Get Count", getCount ) );
        elems.add(new StatElement<>( "Get Matching Count", getMatchingCount ) );
        elems.add(new StatElement<>( "DB URL", getJdbcDiskCacheAttributes().getUrl()) );

        stats.setStatElements( elems );

        return stats;
    }

    /**
     * Returns the name of the table.
     * <p>
     * @return the table name or UNDEFINED
     */
    protected String getTableName()
    {
        String name = "UNDEFINED";
        if ( this.getJdbcDiskCacheAttributes() != null )
        {
            name = this.getJdbcDiskCacheAttributes().getTableName();
        }
        return name;
    }

    /**
     * @param tableState The tableState to set.
     */
    public void setTableState( TableState tableState )
    {
        this.tableState = tableState;
    }

    /**
     * @return Returns the tableState.
     */
    public TableState getTableState()
    {
        return tableState;
    }

    /**
     * This is used by the event logging.
     * <p>
     * @return the location of the disk, either path or ip.
     */
    @Override
    protected String getDiskLocation()
    {
        return this.jdbcDiskCacheAttributes.getUrl();
    }

    /**
     * Public so managers can access it.
     * @return the dsFactory
     * @throws SQLException if getting a data source fails
     */
    public DataSource getDataSource() throws SQLException
    {
        return dsFactory.getDataSource();
    }

    /**
     * For debugging.
     * <p>
     * @return this.getStats();
     */
    @Override
    public String toString()
    {
        return this.getStats();
    }
}
