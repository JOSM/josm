package org.apache.commons.jcs.auxiliary.disk.block;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.utils.serialization.StandardSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class manages reading an writing data to disk. When asked to write a value, it returns a
 * block array. It can read an object from the block numbers in a byte array.
 * <p>
 * @author Aaron Smuts
 */
public class BlockDisk
{
    /** The logger */
    private static final Log log = LogFactory.getLog( BlockDisk.class );

    /** The size of the header that indicates the amount of data stored in an occupied block. */
    public static final byte HEADER_SIZE_BYTES = 4;
    // N.B. 4 bytes is the size used for ByteBuffer.putInt(int value) and ByteBuffer.getInt()

    /** defaults to 4kb */
    private static final int DEFAULT_BLOCK_SIZE_BYTES = 4 * 1024;

    /** Size of the blocks */
    private final int blockSizeBytes;

    /**
     * the total number of blocks that have been used. If there are no free, we will use this to
     * calculate the position of the next block.
     */
    private final AtomicInteger numberOfBlocks = new AtomicInteger(0);

    /** Empty blocks that can be reused. */
    private final ConcurrentLinkedQueue<Integer> emptyBlocks = new ConcurrentLinkedQueue<>();

    /** The serializer. */
    private final IElementSerializer elementSerializer;

    /** Location of the spot on disk */
    private final String filepath;

    /** File channel for multiple concurrent reads and writes */
    private final FileChannel fc;

    /** How many bytes have we put to disk */
    private final AtomicLong putBytes = new AtomicLong(0);

    /** How many items have we put to disk */
    private final AtomicLong putCount = new AtomicLong(0);

    /**
     * Constructor for the Disk object
     * <p>
     * @param file
     * @param elementSerializer
     * @throws IOException
     */
    public BlockDisk( File file, IElementSerializer elementSerializer )
        throws IOException
    {
        this( file, DEFAULT_BLOCK_SIZE_BYTES, elementSerializer );
    }

    /**
     * Creates the file and set the block size in bytes.
     * <p>
     * @param file
     * @param blockSizeBytes
     * @throws IOException
     */
    public BlockDisk( File file, int blockSizeBytes )
        throws IOException
    {
        this( file, blockSizeBytes, new StandardSerializer() );
    }

    /**
     * Creates the file and set the block size in bytes.
     * <p>
     * @param file
     * @param blockSizeBytes
     * @param elementSerializer
     * @throws IOException
     */
    public BlockDisk( File file, int blockSizeBytes, IElementSerializer elementSerializer )
        throws IOException
    {
        this.filepath = file.getAbsolutePath();
        RandomAccessFile raf = new RandomAccessFile( filepath, "rw" );
        this.fc = raf.getChannel();
        this.numberOfBlocks.set((int) Math.ceil(1f * this.fc.size() / blockSizeBytes));

        if ( log.isInfoEnabled() )
        {
            log.info( "Constructing BlockDisk, blockSizeBytes [" + blockSizeBytes + "]" );
        }

        this.blockSizeBytes = blockSizeBytes;
        this.elementSerializer = elementSerializer;
    }

    /**
     * Allocate a given number of blocks from the available set
     *
     * @param numBlocksNeeded
     * @return an array of allocated blocks
     */
    private int[] allocateBlocks(int numBlocksNeeded)
    {
        assert numBlocksNeeded >= 1;

        int[] blocks = new int[numBlocksNeeded];
        // get them from the empty list or take the next one
        for (int i = 0; i < numBlocksNeeded; i++)
        {
            Integer emptyBlock = emptyBlocks.poll();
            if (emptyBlock == null)
            {
                emptyBlock = Integer.valueOf(numberOfBlocks.getAndIncrement());
            }
            blocks[i] = emptyBlock.intValue();
        }

        return blocks;
    }

    /**
     * This writes an object to disk and returns the blocks it was stored in.
     * <p>
     * The program flow is as follows:
     * <ol>
     * <li>Serialize the object.</li>
     * <li>Determine the number of blocks needed.</li>
     * <li>Look for free blocks in the emptyBlock list.</li>
     * <li>If there were not enough in the empty list. Take the nextBlock and increment it.</li>
     * <li>If the data will not fit in one block, create sub arrays.</li>
     * <li>Write the subarrays to disk.</li>
     * <li>If the process fails we should decrement the block count if we took from it.</li>
     * </ol>
     * @param object
     * @return the blocks we used.
     * @throws IOException
     */
    protected int[] write( Serializable object )
        throws IOException
    {
        // serialize the object
        byte[] data = elementSerializer.serialize(object);

        if ( log.isDebugEnabled() )
        {
            log.debug( "write, total pre-chunking data.length = " + data.length );
        }

        this.putBytes.addAndGet(data.length);
        this.putCount.incrementAndGet();

        // figure out how many blocks we need.
        int numBlocksNeeded = calculateTheNumberOfBlocksNeeded(data);

        if ( log.isDebugEnabled() )
        {
            log.debug( "numBlocksNeeded = " + numBlocksNeeded );
        }

        // allocate blocks
        int[] blocks = allocateBlocks(numBlocksNeeded);

        int offset = 0;
        final int maxChunkSize = blockSizeBytes - HEADER_SIZE_BYTES;
        ByteBuffer headerBuffer = ByteBuffer.allocate(HEADER_SIZE_BYTES);

        for (int i = 0; i < numBlocksNeeded; i++)
        {
            headerBuffer.clear();
            int length = Math.min(maxChunkSize, data.length - offset);
            headerBuffer.putInt(length);

            ByteBuffer dataBuffer = ByteBuffer.wrap(data, offset, length);

            long position = calculateByteOffsetForBlockAsLong(blocks[i]);
            // write the header
            headerBuffer.flip();
            int written = fc.write(headerBuffer, position);
            assert written == HEADER_SIZE_BYTES;

            //write the data
            written = fc.write(dataBuffer, position + HEADER_SIZE_BYTES);
            assert written == length;

            offset += length;
        }

        //fc.force(false);

        return blocks;
    }

    /**
     * Return the amount to put in each block. Fill them all the way, minus the header.
     * <p>
     * @param complete
     * @param numBlocksNeeded
     * @return byte[][]
     */
    protected byte[][] getBlockChunks( byte[] complete, int numBlocksNeeded )
    {
        byte[][] chunks = new byte[numBlocksNeeded][];

        if ( numBlocksNeeded == 1 )
        {
            chunks[0] = complete;
        }
        else
        {
            int maxChunkSize = this.blockSizeBytes - HEADER_SIZE_BYTES;
            int totalBytes = complete.length;
            int totalUsed = 0;
            for ( short i = 0; i < numBlocksNeeded; i++ )
            {
                // use the max that can be written to a block or whatever is left in the original
                // array
                int chunkSize = Math.min( maxChunkSize, totalBytes - totalUsed );
                byte[] chunk = new byte[chunkSize];
                // copy from the used position to the chunk size on the complete array to the chunk
                // array.
                System.arraycopy( complete, totalUsed, chunk, 0, chunkSize );
                chunks[i] = chunk;
                totalUsed += chunkSize;
            }
        }

        return chunks;
    }

    /**
     * Reads an object that is located in the specified blocks.
     * <p>
     * @param blockNumbers
     * @return Serializable
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected <T extends Serializable> T read( int[] blockNumbers )
        throws IOException, ClassNotFoundException
    {
        byte[] data = null;

        if ( blockNumbers.length == 1 )
        {
            data = readBlock( blockNumbers[0] );
        }
        else
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(getBlockSizeBytes());
            // get all the blocks into data
            for ( short i = 0; i < blockNumbers.length; i++ )
            {
                byte[] chunk = readBlock( blockNumbers[i] );
                baos.write(chunk);
            }

            data = baos.toByteArray();
            baos.close();
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "read, total post combination data.length = " + data.length );
        }

        return elementSerializer.deSerialize( data, null );
    }

    /**
     * This reads the occupied data in a block.
     * <p>
     * The first four bytes of the record should tell us how long it is. The data is read into a
     * byte array and then an object is constructed from the byte array.
     * <p>
     * @return byte[]
     * @param block
     * @throws IOException
     */
    private byte[] readBlock( int block )
        throws IOException
    {
        int datalen = 0;

        String message = null;
        boolean corrupted = false;
        long fileLength = fc.size();

        long position = calculateByteOffsetForBlockAsLong( block );
//        if ( position > fileLength )
//        {
//            corrupted = true;
//            message = "Record " + position + " starts past EOF.";
//        }
//        else
        {
            ByteBuffer datalength = ByteBuffer.allocate(HEADER_SIZE_BYTES);
            fc.read(datalength, position);
            datalength.flip();
            datalen = datalength.getInt();
            if ( position + datalen > fileLength )
            {
                corrupted = true;
                message = "Record " + position + " exceeds file length.";
            }
        }

        if ( corrupted )
        {
            log.warn( "\n The file is corrupt: " + "\n " + message );
            throw new IOException( "The File Is Corrupt, need to reset" );
        }

        ByteBuffer data = ByteBuffer.allocate(datalen);
        fc.read(data, position + HEADER_SIZE_BYTES);
        data.flip();

        return data.array();
    }

    /**
     * Add these blocks to the emptyBlock list.
     * <p>
     * @param blocksToFree
     */
    protected void freeBlocks( int[] blocksToFree )
    {
        if ( blocksToFree != null )
        {
            for ( short i = 0; i < blocksToFree.length; i++ )
            {
                emptyBlocks.offer( Integer.valueOf( blocksToFree[i] ) );
            }
        }
    }

    /**
     * Calculates the file offset for a particular block.
     * <p>
     * @param block number
     * @return the byte offset for this block in the file as a long
     * @since 2.0
     */
    protected long calculateByteOffsetForBlockAsLong( int block )
    {
        return (long) block * blockSizeBytes;
    }

    /**
     * The number of blocks needed.
     * <p>
     * @param data
     * @return the number of blocks needed to store the byte array
     */
    protected int calculateTheNumberOfBlocksNeeded( byte[] data )
    {
        int dataLength = data.length;

        int oneBlock = blockSizeBytes - HEADER_SIZE_BYTES;

        // takes care of 0 = HEADER_SIZE_BYTES + blockSizeBytes
        if ( dataLength <= oneBlock )
        {
            return 1;
        }

        int dividend = dataLength / oneBlock;

        if ( dataLength % oneBlock != 0 )
        {
            dividend++;
        }
        return dividend;
    }

    /**
     * Returns the file length.
     * <p>
     * @return the size of the file.
     * @throws IOException
     */
    protected long length()
        throws IOException
    {
        return fc.size();
    }

    /**
     * Closes the file.
     * <p>
     * @throws IOException
     */
    protected void close()
        throws IOException
    {
        fc.close();
    }

    /**
     * Resets the file.
     * <p>
     * @throws IOException
     */
    protected synchronized void reset()
        throws IOException
    {
        this.numberOfBlocks.set(0);
        this.emptyBlocks.clear();
        fc.truncate(0);
        fc.force(true);
    }

    /**
     * @return Returns the numberOfBlocks.
     */
    protected int getNumberOfBlocks()
    {
        return numberOfBlocks.get();
    }

    /**
     * @return Returns the blockSizeBytes.
     */
    protected int getBlockSizeBytes()
    {
        return blockSizeBytes;
    }

    /**
     * @return Returns the average size of the an element inserted.
     */
    protected long getAveragePutSizeBytes()
    {
        long count = this.putCount.get();

        if (count == 0 )
        {
            return 0;
        }
        return this.putBytes.get() / count;
    }

    /**
     * @return Returns the number of empty blocks.
     */
    protected int getEmptyBlocks()
    {
        return this.emptyBlocks.size();
    }

    /**
     * For debugging only.
     * <p>
     * @return String with details.
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\nBlock Disk " );
        buf.append( "\n  Filepath [" + filepath + "]" );
        buf.append( "\n  NumberOfBlocks [" + this.numberOfBlocks.get() + "]" );
        buf.append( "\n  BlockSizeBytes [" + this.blockSizeBytes + "]" );
        buf.append( "\n  Put Bytes [" + this.putBytes + "]" );
        buf.append( "\n  Put Count [" + this.putCount + "]" );
        buf.append( "\n  Average Size [" + getAveragePutSizeBytes() + "]" );
        buf.append( "\n  Empty Blocks [" + this.getEmptyBlocks() + "]" );
        try
        {
            buf.append( "\n  Length [" + length() + "]" );
        }
        catch ( IOException e )
        {
            // swallow
        }
        return buf.toString();
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
