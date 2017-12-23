/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;

/**
 * Reads a JSON {@link JsonObject object} or an {@link JsonArray array}
 * structure from an input source.
 *
 * <p>The class {@link javax.json.Json} contains methods to create readers from
 * input sources ({@link java.io.InputStream} and {@link java.io.Reader}).
 *
 * <p>
 * The following example demonstrates how to read an empty JSON array from
 * a string:
 * <pre>
 * <code>
 * JsonReader jsonReader = Json.createReader(new StringReader("[]"));
 * JsonArray array = jsonReader.readArray();
 * jsonReader.close();
 * </code>
 * </pre>
 *
 * <p>
 * The class {@link JsonReaderFactory} also contains methods to create
 * {@code JsonReader} instances. A factory instance can be used to create
 * multiple reader instances with the same configuration. This the preferred
 * way to create multiple instances. A sample usage is shown in the following
 * example:
 * <pre>
 * <code>
 * JsonReaderFactory factory = Json.createReaderFactory(config);
 * JsonReader reader1 = factory.createReader(...);
 * JsonReader reader2 = factory.createReader(...);
 * </code>
 * </pre>
 */
public interface JsonReader extends  /*Auto*/Closeable {

    /**
     * Returns a JSON array or object that is represented in
     * the input source. This method needs to be called
     * only once for a reader instance.
     *
     * @return a JSON object or array
     * @throws JsonException if a JSON object or array cannot
     *     be created due to i/o error (IOException would be
     * cause of JsonException)
     * @throws javax.json.stream.JsonParsingException if a JSON object or array
     *     cannot be created due to incorrect representation
     * @throws IllegalStateException if read, readObject, readArray,
     *     readValue or close method is already called
     */
    JsonStructure read();

    /**
     * Returns a JSON object that is represented in
     * the input source. This method needs to be called
     * only once for a reader instance.
     *
     * @return a JSON object
     * @throws JsonException if a JSON object cannot
     *     be created due to i/o error (IOException would be
     *     cause of JsonException)
     * @throws javax.json.stream.JsonParsingException if a JSON object cannot
     *     be created due to incorrect representation
     * @throws IllegalStateException if read, readObject, readArray,
     *     readValue or close method is already called
     */
    JsonObject readObject();

    /**
     * Returns a JSON array that is represented in
     * the input source. This method needs to be called
     * only once for a reader instance.
     *
     * @return a JSON array
     * @throws JsonException if a JSON array cannot
     *     be created due to i/o error (IOException would be
     *     cause of JsonException)
     * @throws javax.json.stream.JsonParsingException if a JSON array cannot
     *     be created due to incorrect representation
     * @throws IllegalStateException if read, readObject, readArray,
     *     readValue or close method is already called
     */
    JsonArray readArray();

    /**
     * Returns a JSON value that is represented in
     * the input source. This method needs to be called
     * only once for a reader instance.
     *
     * @return a JSON value
     * @throws JsonException if a JSON value
     *     be created due to i/o error (IOException would be
     *     cause of JsonException)
     * @throws javax.json.stream.JsonParsingException if a JSON value
     *     cannot be created due to incorrect representation
     * @throws IllegalStateException if read, readObject, readArray,
     *     readValue or close method is already called
     *
     * @since 1.1
     */
    default JsonValue readValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * Closes this reader and frees any resources associated with the
     * reader. This method closes the underlying input source.
     *
     * @throws JsonException if an i/o error occurs (IOException would be
     * cause of JsonException)
     */
    @Override
    void close();

}
