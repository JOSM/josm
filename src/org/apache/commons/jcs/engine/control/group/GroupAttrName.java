package org.apache.commons.jcs.engine.control.group;

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
 * Description of the Class
 */
public class GroupAttrName<T>
    implements Serializable
{
    /** Don't change */
    private static final long serialVersionUID = 1586079686300744198L;

    /** Description of the Field */
    public final GroupId groupId;

    /** the name of the attribute */
    public final T attrName;

    /** Cached toString value */
    private String toString;

    /**
     * Constructor for the GroupAttrName object
     * @param groupId
     * @param attrName
     */
    public GroupAttrName( GroupId groupId, T attrName )
    {
        this.groupId = groupId;
        this.attrName = attrName;

        if ( groupId == null )
        {
            throw new IllegalArgumentException( "groupId must not be null." );
        }
    }

    /**
     * Tests object equality.
     * @param obj The <code>GroupAttrName</code> instance to test.
     * @return Whether equal.
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( obj == null || !( obj instanceof GroupAttrName ) )
        {
            return false;
        }
        GroupAttrName<?> to = (GroupAttrName<?>) obj;

        if (groupId.equals( to.groupId ))
        {
            if (attrName == null && to.attrName == null)
            {
                return true;
            }
            else if (attrName == null || to.attrName == null)
            {
                return false;
            }

            return  attrName.equals( to.attrName );
        }

        return false;
    }

    /**
     * @return A hash code based on the hash code of @ #groupid} and {@link #attrName}.
     */
    @Override
    public int hashCode()
    {
        if (attrName == null)
        {
            return groupId.hashCode();
        }

        return groupId.hashCode() ^ attrName.hashCode();
    }

    /**
     * @return the cached value.
     */
    @Override
    public String toString()
    {
        if ( toString == null )
        {
            toString = "[GAN: groupId=" + groupId + ", attrName=" + attrName + "]";
        }

        return toString;
    }

}
