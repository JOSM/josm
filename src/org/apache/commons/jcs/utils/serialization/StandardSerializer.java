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

import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.io.ObjectInputStreamClassLoaderAware;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Performs default serialization and de-serialization.
 * <p>
 * @author Aaron Smuts
 */
public class StandardSerializer
    implements IElementSerializer
{
    /**
     * Serializes an object using default serialization.
     * <p>
     * @param obj
     * @return byte[]
     * @throws IOException
     */
    @Override
    public <T> byte[] serialize( T obj )
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        try
        {
            oos.writeObject( obj );
        }
        finally
        {
            oos.close();
        }
        return baos.toByteArray();
    }

    /**
     * Uses default de-serialization to turn a byte array into an object. All exceptions are
     * converted into IOExceptions.
     * <p>
     * @param data
     * @return Object
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Override
    public <T> T deSerialize( byte[] data, ClassLoader loader )
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream( data );
        BufferedInputStream bis = new BufferedInputStream( bais );
        ObjectInputStream ois = new ObjectInputStreamClassLoaderAware( bis, loader );
        try
        {
            @SuppressWarnings("unchecked") // Need to cast from Object
            T readObject = (T) ois.readObject();
            return readObject;
        }
        finally
        {
            ois.close();
        }
    }
}
