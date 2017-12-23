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

package javax.json;

/**
 * <code>JsonValue</code> represents an immutable JSON value.
 *
 *
 * <p>A JSON value is one of the following:
 * an object ({@link JsonObject}), an array ({@link JsonArray}),
 * a number ({@link JsonNumber}), a string ({@link JsonString}),
 * {@code true} ({@link JsonValue#TRUE JsonValue.TRUE}), {@code false}
 * ({@link JsonValue#FALSE JsonValue.FALSE}),
 * or {@code null} ({@link JsonValue#NULL JsonValue.NULL}).
 */
public interface JsonValue {

    /**
     * The empty JSON object.
     *
     * @since 1.1
     */
    static final JsonObject EMPTY_JSON_OBJECT = new EmptyObject();

    /**
     * The empty JSON array.
     *
     * @since 1.1
     */
    static final JsonArray EMPTY_JSON_ARRAY = new EmptyArray();

    /**
     * Indicates the type of a {@link JsonValue} object.
     */
    enum ValueType {
        /**
         * JSON array.
         */
        ARRAY,

        /**
         * JSON object.
         */
        OBJECT,

        /**
         * JSON string.
         */
        STRING,

        /**
         * JSON number.
         */
        NUMBER,

        /**
         * JSON true.
         */
        TRUE,

        /**
         * JSON false.
         */
        FALSE,

        /**
         * JSON null.
         */
        NULL
    }

    /**
     * JSON null value.
     */
    static final JsonValue NULL = new JsonValueImpl(ValueType.NULL);

    /**
     * JSON true value.
     */
    static final JsonValue TRUE = new JsonValueImpl(ValueType.TRUE);

    /**
     * JSON false value.
     */
    static final JsonValue FALSE = new JsonValueImpl(ValueType.FALSE);

    /**
     * Returns the value type of this JSON value.
     *
     * @return JSON value type
     */
    ValueType getValueType();

    /**
     * Return the JsonValue as a JsonObject
     *
     * @return the JsonValue as a JsonObject
     * @throws ClassCastException if the JsonValue is not a JsonObject
     *
     * @since 1.1
     */
    default JsonObject asJsonObject() {
        return JsonObject.class.cast(this);
    }

    /**
     * Return the JsonValue as a JsonArray
     *
     * @return the JsonValue as a JsonArray
     * @throws ClassCastException if the JsonValue is not a JsonArray
     *
     * @since 1.1
     */
    default JsonArray asJsonArray() {
        return JsonArray.class.cast(this);
    }

    /**
     * Returns JSON text for this JSON value.
     *
     * @return JSON text
     */
    @Override
    String toString();

}
