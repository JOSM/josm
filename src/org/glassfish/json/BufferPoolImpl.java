/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.json;

import org.glassfish.json.api.BufferPool;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * char[] pool that pool instances of char[] which are expensive to create.
 *
 * @author Jitendra Kotamraju
 */
class BufferPoolImpl implements BufferPool {

    // volatile since multiple threads may access queue reference
    private volatile WeakReference<ConcurrentLinkedQueue<char[]>> queue;

    /**
     * Gets a new object from the pool.
     *
     * <p>
     * If no object is available in the pool, this method creates a new one.
     *
     * @return
     *      always non-null.
     */
    @Override
    public final char[] take() {
        char[] t = getQueue().poll();
        if (t==null)
            return new char[4096];
        return t;
    }

    private ConcurrentLinkedQueue<char[]> getQueue() {
        WeakReference<ConcurrentLinkedQueue<char[]>> q = queue;
        if (q != null) {
            ConcurrentLinkedQueue<char[]> d = q.get();
            if (d != null)
                return d;
        }

        // overwrite the queue
        ConcurrentLinkedQueue<char[]> d = new ConcurrentLinkedQueue<>();
        queue = new WeakReference<>(d);

        return d;
    }

    /**
     * Returns an object back to the pool.
     */
    @Override
    public final void recycle(char[] t) {
        getQueue().offer(t);
    }

}
