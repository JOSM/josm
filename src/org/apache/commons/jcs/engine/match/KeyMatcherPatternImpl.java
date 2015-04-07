package org.apache.commons.jcs.engine.match;

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

import org.apache.commons.jcs.engine.match.behavior.IKeyMatcher;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** This implementation of the KeyMatcher uses standard Java Pattern matching. */
public class KeyMatcherPatternImpl<K>
    implements IKeyMatcher<K>
{
    /** Serial version */
    private static final long serialVersionUID = 6667352064144381264L;

    /**
     * Creates a pattern and find matches on the array.
     * <p>
     * @param pattern
     * @param keyArray
     * @return Set of the matching keys
     */
    @Override
    public Set<K> getMatchingKeysFromArray( String pattern, Set<K> keyArray )
    {
        Pattern compiledPattern = Pattern.compile( pattern );

        Set<K> matchingKeys = new HashSet<K>();

        // Look for matches
        for (K key : keyArray)
        {
            // TODO we might want to match on the toString.
            if ( key instanceof String )
            {
                Matcher matcher = compiledPattern.matcher( (String) key );
                if ( matcher.matches() )
                {
                    matchingKeys.add( key );
                }
            }
        }

        return matchingKeys;
    }
}
