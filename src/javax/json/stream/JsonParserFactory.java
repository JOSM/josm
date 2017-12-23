/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

package javax.json.stream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Factory for creating {@link JsonParser} instances. If a factory
 * instance is configured with a configuration, the configuration applies
 * to all parser instances created using that factory instance.
 *
 * <p>
 * The class {@link javax.json.Json Json} also provides methods to create
 * {@link JsonParser} instances, but using {@code JsonParserFactory} is 
 * preferred when creating multiple parser instances as shown in the following
 * example:
 *
 * <pre>
 * <code>
 * JsonParserFactory factory = Json.createParserFactory();
 * JsonParser parser1 = factory.createParser(...);
 * JsonParser parser2 = factory.createParser(...);
 * </code>
 * </pre>
 *
 * <p> All the methods in this class are safe for use by multiple concurrent
 * threads.
 */
public interface JsonParserFactory {

    /**
     * Creates a JSON parser from a character stream.
     *
     * @param reader a i/o reader from which JSON is to be read
     * @return the created JSON parser
     */
    JsonParser createParser(Reader reader);

    /**
     * Creates a JSON parser from the specified byte stream.
     * The character encoding of the stream is determined
     * as specified in <a href="http://tools.ietf.org/rfc/rfc7159.txt">RFC 7159</a>.
     *
     * @param in i/o stream from which JSON is to be read
     * @return the created JSON parser
     * @throws javax.json.JsonException if encoding cannot be determined
     *         or i/o error (IOException would be cause of JsonException)
     */
    JsonParser createParser(InputStream in);

    /**
     * Creates a JSON parser from the specified byte stream.
     * The bytes of the stream are decoded to characters using the
     * specified charset.
     *
     * @param in i/o stream from which JSON is to be read
     * @param charset a charset
     * @return the created JSON parser
     */
    JsonParser createParser(InputStream in, Charset charset);

    /**
     * Creates a JSON parser from the specified JSON object.
     *
     * @param obj a JSON object
     * @return the created JSON parser
     */
    JsonParser createParser(JsonObject obj);

    /**
     * Creates a JSON parser from the specified JSON array.
     *
     * @param array a JSON array
     * @return the created JSON parser
     */
    JsonParser createParser(JsonArray array);

    /**
     * Returns a read-only map of supported provider specific configuration
     * properties that are used to configure the JSON parsers.
     * If there are any specified configuration properties that are not
     * supported by the provider, they won't be part of the returned map.
     *
     * @return a map of supported provider specific properties that are used
     * to configure the created parsers. The map may be empty but not null
     */
    Map<String, ?> getConfigInUse();

}
