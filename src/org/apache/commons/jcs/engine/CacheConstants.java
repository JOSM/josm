package org.apache.commons.jcs.engine;

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

/**
 * Constants used throughout the JCS cache engine
 * <p>
 * @version $Id: CacheConstants.java 1590887 2014-04-29 07:07:32Z olamy $
 */
public interface CacheConstants
{
    /** This is the name of the config file that we will look for by default. */
    String DEFAULT_CONFIG = "/cache.ccf";

    /** Delimiter of a cache name component. This is used for hierarchical deletion */
    String NAME_COMPONENT_DELIMITER = ":";
}
