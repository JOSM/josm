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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Provides thread safe access to the underlying random access file. */
class IndexedDisk
{
    /** The size of the header that indicates the amount of data stored in an occupied block. */
    public static final byte HEADER_SIZE_BYTES = 4;

    /** The serializer. */
    private final IElementSerializer elementSerializer;

    /** The logger */
    private static final Log log = LogFactory.getLog( IndexedDisk.class );

    /** The path to the log directory. */
    private final String filepath;

    /** The data file. */
    private final FileChannel fc;

    /**
     * Constructor for the Disk object
     * <p>
     * @param file
     * @param elementSerializer
     * @throws FileNotFoundException
     */
    public IndexedDisk( File file, IElementSerializer elementSerializer )
        throws FileNotFoundException
    {
        this.filepath = file.getAbsolutePath();
        this.elementSerializer = elementSerializer;
        RandomAccessFile raf = new RandomAccessFile( filepath, "rw" );
        this.fc = raf.getChannel();
    }

    /**
     * This reads an object from the given starting position on the file.
     * <p>
     * The first four bytes of the record should tell us how long it is. The data is read into a byte
     * array and then an object is constructed from the byte array.
     * <p>
     * @return Serializable
     * @param ded
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected <T extends Serializable> T readObject( IndexedDiskElementDescriptor ded )
        throws IOException, ClassNotFoundException
    {
        String message = null;
        boolean corrupted = false;
        long fileLength = fc.size();
        if ( ded.pos > fileLength )
        {
            corrupted = true;
            message = "Record " + ded + " starts past EOF.";
        }
        else
        {
            ByteBuffer datalength = ByteBuffer.allocate(HEADER_SIZE_BYTES);
            fc.read(datalength, ded.pos);
            datalength.flip();
            int datalen = datalength.getInt();
            if ( ded.len != datalen )
            {
                corrupted = true;
                message = "Record " + ded + " does not match data length on disk (" + datalen + ")";
            }
            else if ( ded.pos + ded.len > fileLength )
            {
                corrupted = true;
                message = "Record " + ded + " exceeds file length.";
            }
        }

        if ( corrupted )
        {
            log.warn( "\n The file is corrupt: " + "\n " + message );
            throw new IOException( "The File Is Corrupt, need to reset" );
        }

        ByteBuffer data = ByteBuffer.allocate(ded.len);
        fc.read(data, ded.pos + HEADER_SIZE_BYTES);
        data.flip();

        return elementSerializer.deSerialize( data.array(), null );
    }

    /**
     * Moves the data stored from one position to another. The descriptor's position is updated.
     * <p>
     * @param ded
     * @param newPosition
     * @throws IOException
     */
    protected void move( final IndexedDiskElementDescriptor ded, final long newPosition )
        throws IOException
    {
        ByteBuffer datalength = ByteBuffer.allocate(HEADER_SIZE_BYTES);
        fc.read(datalength, ded.pos);
        datalength.flip();
        int length = datalength.getInt();

        if ( length != ded.len )
        {
            throw new IOException( "Mismatched memory and disk length (" + length + ") for " + ded );
        }

        // TODO: more checks?

        long readPos = ded.pos;
        long writePos = newPosition;

        // header len + data len
        int remaining = HEADER_SIZE_BYTES + length;
        ByteBuffer buffer = ByteBuffer.allocate(16384);

        while ( remaining > 0 )
        {
            // chunk it
            int chunkSize = Math.min( remaining, buffer.capacity() );
            buffer.limit(chunkSize);
            fc.read(buffer, readPos);
            buffer.flip();
            fc.write(buffer, writePos);
            buffer.clear();

            writePos += chunkSize;
            readPos += chunkSize;
            remaining -= chunkSize;
        }

        ded.pos = newPosition;
    }

    /**
     * Writes the given byte array to the Disk at the specified position.
     * <p>
     * @param data
     * @param ded
     * @return true if we wrote successfully
     * @throws IOException
     */
    protected boolean write( IndexedDiskElementDescriptor ded, byte[] data )
        throws IOException
    {
        long pos = ded.pos;
        if ( log.isTraceEnabled() )
        {
            log.trace( "write> pos=" + pos );
            log.trace( fc + " -- data.length = " + data.length );
        }

        if ( data.length != ded.len )
        {
            throw new IOException( "Mismatched descriptor and data lengths" );
        }

        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE_BYTES + data.length);
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.flip();
        int written = fc.write(buffer, pos);
        //fc.force(true);

        return written == data.length;
    }

    /**
     * Serializes the object and write it out to the given position.
     * <p>
     * TODO: make this take a ded as well.
     * @return true unless error
     * @param obj
     * @param pos
     * @throws IOException
     */
    protected boolean writeObject( Serializable obj, long pos )
        throws IOException
    {
        byte[] data = elementSerializer.serialize( obj );
        write( new IndexedDiskElementDescriptor( pos, data.length ), data );
        return true;
    }

    /**
     * Returns the raf length.
     * <p>
     * @return the length of the file.
     * @throws IOException
     */
    protected long length()
        throws IOException
    {
        return fc.size();
    }

    /**
     * Closes the raf.
     * <p>
     * @throws IOException
     */
    protected void close()
        throws IOException
    {
        fc.close();
    }

    /**
     * Sets the raf to empty.
     * <p>
     * @throws IOException
     */
    protected synchronized void reset()
        throws IOException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Resetting Indexed File [" + filepath + "]" );
        }
        fc.truncate(0);
        fc.force(true);
    }

    /**
     * Truncates the file to a given length.
     * <p>
     * @param length the new length of the file
     * @throws IOException
     */
    protected void truncate( long length )
        throws IOException
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Truncating file [" + filepath + "] to " + length );
        }
        fc.truncate( length );
    }

    /**
     * This is used for debugging.
     * <p>
     * @return the file path.
     */
    protected String getFilePath()
    {
        return filepath;
    }
}
