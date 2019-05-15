package org.apache.commons.jcs.utils.serialization;

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

import org.apache.commons.jcs.utils.zip.CompressionUtil;

/**
 * Performs default serialization and de-serialization. It gzips the value.
 */
public class CompressingSerializer extends StandardSerializer
{
    /**
     * Serializes an object using default serialization. Compresses the byte array.
     * <p>
     * @param obj object
     * @return byte[]
     * @throws IOException on i/o problem
     */
    @Override
    public <T> byte[] serialize( T obj )
        throws IOException
    {
        byte[] uncompressed = super.serialize(obj);
        byte[] compressed = CompressionUtil.compressByteArray( uncompressed );
        return compressed;
    }

    /**
     * Uses default de-serialization to turn a byte array into an object. Decompresses the value
     * first. All exceptions are converted into IOExceptions.
     * <p>
     * @param data bytes of data
     * @return Object
     * @throws IOException on i/o problem
     * @throws ClassNotFoundException if class is not found during deserialization
     */
    @Override
    public <T> T deSerialize( byte[] data, ClassLoader loader )
        throws IOException, ClassNotFoundException
    {
        if ( data == null )
        {
            return null;
        }
        
        byte[] decompressedByteArray = CompressionUtil.decompressByteArray( data );
        return super.deSerialize(decompressedByteArray, loader);
    }
}
