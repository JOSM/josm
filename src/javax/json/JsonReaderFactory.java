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

package javax.json;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Factory to create {@link javax.json.JsonReader} instances. If a factory
 * instance is configured with some configuration, that would be
 * used to configure the created reader instances.
 *
 * <p>
 * {@link javax.json.JsonReader} can also be created using {@link Json}'s
 * {@code createReader} methods. If multiple reader instances are created,
 * then creating them using a reader factory is preferred.
 *
 * <p>
 * <b>For example:</b>
 * <pre>
 * <code>
 * JsonReaderFactory factory = Json.createReaderFactory(...);
 * JsonReader reader1 = factory.createReader(...);
 * JsonReader reader2 = factory.createReader(...);
 * </code>
 * </pre>
 *
 * <p> All the methods in this class are safe for use by multiple concurrent
 * threads.
 */
public interface JsonReaderFactory {

    /**
     * Creates a JSON reader from a character stream. The reader is configured
     * with the factory configuration.
     *
     * @param reader a reader from which JSON is to be read
     * @return a JSON reader
     */
    JsonReader createReader(Reader reader);

    /**
     * Creates a JSON reader from a byte stream. The character encoding of
     * the stream is determined as described in
     * <a href="http://tools.ietf.org/rfc/rfc7159.txt">RFC 7159</a>.
     * The reader is configured with the factory configuration.
     *
     * @param in a byte stream from which JSON is to be read
     * @return a JSON reader
     */
    JsonReader createReader(InputStream in);

    /**
     * Creates a JSON reader from a byte stream. The bytes of the stream
     * are decoded to characters using the specified charset. The reader is
     * configured with the factory configuration.
     *
     * @param in a byte stream from which JSON is to be read
     * @param charset a charset
     * @return a JSON reader
     */
    JsonReader createReader(InputStream in, Charset charset);

    /**
     * Returns read-only map of supported provider specific configuration
     * properties that are used to configure the created JSON readers.
     * If there are any specified configuration properties that are not
     * supported by the provider, they won't be part of the returned map.
     *
     * @return a map of supported provider specific properties that are used
     * to configure the readers. The map be empty but not null.
     */
    Map<String, ?> getConfigInUse();

}
