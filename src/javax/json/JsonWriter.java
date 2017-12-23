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
 * Writes a JSON {@link JsonObject object} or {@link JsonArray array} structure
 * to an output source.
 *
 * <p>The class {@link javax.json.Json} contains methods to create writers from
 * output sources ({@link java.io.OutputStream} and {@link java.io.Writer}).
 *
 * <p>
 * The following example demonstrates how write an empty JSON object:
 * <pre>
 * <code>
 * JsonWriter jsonWriter = Json.createWriter(...);
 * jsonWriter.writeObject(Json.createObjectBuilder().build());
 * jsonWriter.close();
 * </code>
 * </pre>
 *
 * <p>
 * The class {@link JsonWriterFactory} also contains methods to create
 * {@code JsonWriter} instances. A factory instance can be used to create
 * multiple writer instances with the same configuration. This the preferred
 * way to create multiple instances. A sample usage is shown in the following
 * example:
 * <pre>
 * <code>
 * JsonWriterFactory factory = Json.createWriterFactory(config);
 * JsonWriter writer1 = factory.createWriter(...);
 * JsonWriter writer2 = factory.createWriter(...);
 * </code>
 * </pre>
 */
public interface JsonWriter extends  /*Auto*/Closeable {

    /**
     * Writes the specified JSON {@link JsonArray array} to the output
     * source. This method needs to be called only once for a writer instance.
     *
     * @param array JSON array that is to be written to the output source
     * @throws JsonException if the specified JSON object cannot be
     *     written due to i/o error (IOException would be cause of
     *     JsonException)
     * @throws IllegalStateException if writeArray, writeObject, write or close
     *     method is already called
     */
    void writeArray(JsonArray array);

    /**
     * Writes the specified JSON {@link JsonObject object} to the output
     * source. This method needs to be called only once for a writer instance.
     *
     * @param object JSON object that is to be written to the output source
     * @throws JsonException if the specified JSON object cannot be
     *     written due to i/o error (IOException would be cause of JsonException)
     * @throws IllegalStateException if writeArray, writeObject, write or close
     *     method is already called
     */
    void writeObject(JsonObject object);

    /**
     * Writes the specified JSON {@link JsonObject object} or
     * {@link JsonArray array} to the output source. This method needs
     * to be called only once for a writer instance.
     *
     * @param value JSON array or object that is to be written to the output
     *              source
     * @throws JsonException if the specified JSON object cannot be
     *     written due to i/o error (IOException would be cause of
     *     JsonException)
     * @throws IllegalStateException if writeArray, writeObject, write
     *     or close method is already called
     */
    void write(JsonStructure value);

    /**
     * Closes this JSON writer and frees any resources associated with the
     * writer. This method closes the underlying output source.
     *
     * @throws JsonException if an i/o error occurs (IOException would be
     * cause of JsonException)
     */

    /**
     * Writes the specified {@link JsonValue} to the output source.
     * method needs to be called only once for a write instance.
     *
     * @param value a {@code JsonValue} to be written to the output
     *              source
     * @throws JsonException if the specified JSON object cannot be
     *     written due to i/o error (IOException would be cause of
     *     JsonException)
     * @throws IllegalStateException if writeArray, writeObject, write
     *     or close method is already called
     *
     * @since 1.1
     */
    default void write(JsonValue value) {
        throw new UnsupportedOperationException();
    }

    @Override
    void close();

}
