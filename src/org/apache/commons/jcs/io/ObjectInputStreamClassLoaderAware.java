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
package org.apache.commons.jcs.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

public class ObjectInputStreamClassLoaderAware extends ObjectInputStream {
    private final ClassLoader classLoader;

    public ObjectInputStreamClassLoaderAware(final InputStream in, final ClassLoader classLoader) throws IOException {
        super(in);
        this.classLoader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws ClassNotFoundException {
        return Class.forName(BlacklistClassResolver.DEFAULT.check(desc.getName()), false, classLoader);
    }

    @Override
    protected Class<?> resolveProxyClass(final String[] interfaces) throws IOException, ClassNotFoundException {
        final Class<?>[] cinterfaces = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            cinterfaces[i] = Class.forName(interfaces[i], false, classLoader);
        }

        try {
            return Proxy.getProxyClass(classLoader, cinterfaces);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }

    private static class BlacklistClassResolver {
        private static final BlacklistClassResolver DEFAULT = new BlacklistClassResolver(
            toArray(System.getProperty(
                "jcs.serialization.class.blacklist",
                "org.codehaus.groovy.runtime.,org.apache.commons.collections.functors.,org.apache.xalan")),
            toArray(System.getProperty("jcs.serialization.class.whitelist")));

        private final String[] blacklist;
        private final String[] whitelist;

        protected BlacklistClassResolver(final String[] blacklist, final String[] whitelist) {
            this.whitelist = whitelist;
            this.blacklist = blacklist;
        }

        protected boolean isBlacklisted(final String name) {
            return (whitelist != null && !contains(whitelist, name)) || contains(blacklist, name);
        }

        public final String check(final String name) {
            if (isBlacklisted(name)) {
                throw new SecurityException(name + " is not whitelisted as deserialisable, prevented before loading.");
            }
            return name;
        }

        private static String[] toArray(final String property) {
            return property == null ? null : property.split(" *, *");
        }

        private static boolean contains(final String[] list, String name) {
            if (list != null) {
                for (final String white : list) {
                    if (name.startsWith(white)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
