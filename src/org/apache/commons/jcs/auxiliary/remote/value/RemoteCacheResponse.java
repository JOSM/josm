package org.apache.commons.jcs.auxiliary.remote.value;

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

import java.io.Serializable;

/**
 * This is the response wrapper. The servlet wraps all different type of responses in one of these
 * objects.
 */
public class RemoteCacheResponse<T>
    implements Serializable
{
    /** Don't change. */
    private static final long serialVersionUID = -8858447417390442568L;

    /** Was the event processed without error */
    private boolean success = true;

    /** Simple error messaging */
    private String errorMessage;

    /**
     * The payload. Typically a key / ICacheElement&lt;K, V&gt; map. A normal get will return a map with one
     * record.
     */
    private T payload;

    /**
     * @param success the success to set
     */
    public void setSuccess( boolean success )
    {
        this.success = success;
    }

    /**
     * @return the success
     */
    public boolean isSuccess()
    {
        return success;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage()
    {
        return errorMessage;
    }

    /**
     * @param payload the payload to set
     */
    public void setPayload( T payload )
    {
        this.payload = payload;
    }

    /**
     * @return the payload
     */
    public T getPayload()
    {
        return payload;
    }

    /** @return string */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "\nRemoteHttpCacheResponse" );
        buf.append( "\n success [" + isSuccess() + "]" );
        buf.append( "\n payload [" + getPayload() + "]" );
        buf.append( "\n errorMessage [" + getErrorMessage() + "]" );
        return buf.toString();
    }
}
