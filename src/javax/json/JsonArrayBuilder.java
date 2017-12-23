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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A builder for creating {@link JsonArray} models from scratch, and for
 * modifying a existing {@code JsonArray}.
 * <p>A {@code JsonArrayBuilder} can start with an empty or a non-empty
 * JSON array model. This interface provides methods to add, insert, remove
 * and replace values in the JSON array model.</p>
 * <p>Methods in this class can be chained to perform multiple values to
 * the array.</p>
 *
 * <p>The class {@link javax.json.Json} contains methods to create the builder
 * object. The example code below shows how to build an empty {@code JsonArray}
 * instance.
 * <pre>
 * <code>
 * JsonArray array = Json.createArrayBuilder().build();
 * </code>
 * </pre>
 *
 * <p>The class {@link JsonBuilderFactory} also contains methods to create
 * {@code JsonArrayBuilder} instances. A factory instance can be used to create
 * multiple builder instances with the same configuration. This the preferred
 * way to create multiple instances.
 *
 * The example code below shows how to build a {@code JsonArray} object
 * that represents the following JSON array:
 *
 * <pre>
 * <code>
 * [
 *     { "type": "home", "number": "212 555-1234" },
 *     { "type": "fax", "number": "646 555-4567" }
 * ]
 * </code>
 * </pre>
 *
 * <p>The following code creates the JSON array above:
 *
 * <pre>
 * <code>
 * JsonBuilderFactory factory = Json.createBuilderFactory(config);
 * JsonArray value = factory.createArrayBuilder()
 *     .add(factory.createObjectBuilder()
 *         .add("type", "home")
 *         .add("number", "212 555-1234"))
 *     .add(factory.createObjectBuilder()
 *         .add("type", "fax")
 *         .add("number", "646 555-4567"))
 *     .build();
 * </code>
 * </pre>
 *
 * <p>This class does <em>not</em> allow <tt>null</tt> to be used as a
 * value while building the JSON array
 *
 * @see JsonObjectBuilder
 */
public interface JsonArrayBuilder {

    /**
     * Adds a value to the array.
     *
     * @param value the JSON value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     */
    JsonArrayBuilder add(JsonValue value);

    /**
     * Adds a value to the array as a {@link JsonString}.
     *
     * @param value the string value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     */
    JsonArrayBuilder add(String value);

    /**
     * Adds a value to the array as a {@link JsonNumber}.
     *
     * @param value the number value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     *
     * @see JsonNumber
     */
    JsonArrayBuilder add(BigDecimal value);

    /**
     * Adds a value to the array as a {@link JsonNumber}.
     *
     * @param value the number value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     *
     * @see JsonNumber
     */
    JsonArrayBuilder add(BigInteger value);

    /**
     * Adds a value to the array as a {@link JsonNumber}.
     *
     * @param value the number value
     * @return this array builder
     *
     * @see JsonNumber
     */
    JsonArrayBuilder add(int value);

    /**
     * Adds a value to the array as a {@link JsonNumber}.
     *
     * @param value the number value
     * @return this array builder
     *
     * @see JsonNumber
     */
    JsonArrayBuilder add(long value);

    /**
     * Adds a value to the array as a {@link JsonNumber}.
     *
     * @param value the number value
     * @return this array builder
     * @throws NumberFormatException if the value is Not-a-Number (NaN) or
     *      infinity
     *
     * @see JsonNumber
     */
    JsonArrayBuilder add(double value);

    /**
     * Adds a {@link JsonValue#TRUE}  or {@link JsonValue#FALSE} value to the
     * array.
     *
     * @param value the boolean value
     * @return this array builder
     */
    JsonArrayBuilder add(boolean value);

    /**
     * Adds a {@link JsonValue#NULL} value to the array.
     *
     * @return this array builder
     */
    JsonArrayBuilder addNull();

    /**
     * Adds a {@link JsonObject} from an object builder to the array.
     *
     * @param builder the object builder
     * @return this array builder
     * @throws NullPointerException if the specified builder is null
     */
    JsonArrayBuilder add(JsonObjectBuilder builder);

    /**
     * Adds a {@link JsonArray} from an array builder to the array.
     *
     * @param builder the array builder
     * @return this array builder
     * @throws NullPointerException if the specified builder is null
     */
    JsonArrayBuilder add(JsonArrayBuilder builder);

    /**
     * Adds all elements of the array in the specified array builder to the array.
     *
     * @param builder the array builder
     * @return this array builder
     * @throws NullPointerException if the specified builder is null
     *
     @since 1.1
     */
    default JsonArrayBuilder addAll(JsonArrayBuilder builder) {
        throw new UnsupportedOperationException();
    }

    /**
     * Inserts a value to the array at the specified position. Shifts the value
     * currently at that position (if any) and any subsequent values to the right
     * (adds one to their indices).  Index starts with 0.
     *
     * @param index the position in the array
     * @param value the JSON value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index > array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder add(int index, JsonValue value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a value to the array as a {@link JsonString} at the specified position.
     * Shifts the value currently at that position (if any) and any subsequent values
     * to the right (adds one to their indices).  Index starts with 0.
     *
     * @param index the position in the array
     * @param value the string value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index > array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder add(int index, String value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a value to the array as a {@link JsonNumber} at the specified position.
     * Shifts the value currently at that position (if any) and any subsequent values
     * to the right (adds one to their indices).  Index starts with 0.
     *
     * @param index the position in the array
     * @param value the number value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index > array size)}
     *
     * @see JsonNumber
     *
     * @since 1.1
     */
    default JsonArrayBuilder add(int index, BigDecimal value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a value to the array as a {@link JsonNumber} at the specified position.
     * Shifts the value currently at that position (if any) and any subsequent values
     * to the right (adds one to their indices).  Index starts with 0.
     *
     * @param index the position in the array
     * @param value the number value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index > array size)}
     *
     * @see JsonNumber
     *
     * @since 1.1
     */
    default JsonArrayBuilder add(int index, BigInteger value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a value to the array as a {@link JsonNumber} at the specified position.
     * Shifts the value currently at that position (if any) and any subsequent values
     * to the right (adds one to their indices).  Index starts with 0.
     *
     * @param index the position in the array
     * @param value the number value
     * @return this array builder
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index > array size)}
     *
     * @see JsonNumber
     *
     * @since 1.1
     */
    default JsonArrayBuilder add(int index, int value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a value to the array as a {@link JsonNumber} at the specified position.
     * Shifts the value currently at that position (if any) and any subsequent values
     * to the right (adds one to their indices).  Index starts with 0.
     *
     * @param index the position in the array
     * @param value the number value
     * @return this array builder
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index > array size)}
     *
     * @see JsonNumber
     *
     * @since 1.1
     */
    default JsonArrayBuilder add(int index, long value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a value to the array as a {@link JsonNumber} at the specified position.
     * Shifts the value currently at that position (if any) and any subsequent values
     * to the right (adds one to their indices).  Index starts with 0.
     *
     * @param index the position in the array
     * @param value the number value
     * @return this array builder
     * @throws NumberFormatException if the value is Not-a-Number (NaN) or
     *      infinity
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index > array size)}
     *
     * @see JsonNumber
     *
     * @since 1.1
     */
    default JsonArrayBuilder add(int index, double value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a {@link JsonValue#TRUE}  or {@link JsonValue#FALSE} value to the
     * array at the specified position.
     * Shifts the value currently at that position (if any) and any subsequent values
     * to the right (adds one to their indices).  Index starts with 0.
     *
     * @param index the position in the array
     * @param value the boolean value
     * @return this array builder
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index > array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder add(int index, boolean value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a {@link JsonValue#NULL} value to the array at the specified position.
     * Shifts the value currently at that position (if any) and any subsequent values
     * to the right (adds one to their indices).  Index starts with 0.
     *
     * @param index the position in the array
     * @return this array builder
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index > array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder addNull(int index) {
        return add(index, JsonValue.NULL);
    }

    /**
     * Adds a {@link JsonObject} from an object builder to the array at the specified position.
     * Shifts the value currently at that position (if any) and any subsequent values
     * to the right (adds one to their indices).  Index starts with 0.
     *
     * @param index the position in the array
     * @param builder the object builder
     * @return this array builder
     * @throws NullPointerException if the specified builder is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index > array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder add(int index, JsonObjectBuilder builder) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a {@link JsonArray} from an array builder to the array at the specified position.
     * Shifts the value currently at that position (if any) and any subsequent values
     * to the right (adds one to their indices).  Index starts with 0.
     *
     * @param index the position in the array
     * @param builder the array builder
     * @return this array builder
     * @throws NullPointerException if the specified builder is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index > array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder add(int index, JsonArrayBuilder builder) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replaces a value in the array with the specified value at the
     * specified position.
     *
     * @param index the position in the array
     * @param value the JSON value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder set(int index, JsonValue value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replaces a value in the array with the specified value as a
     * {@link JsonString} at the specified position.
     *
     * @param index the position in the array
     * @param value the string value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder set(int index, String value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replaces a value in the array with the specified value as a
     * {@link JsonNumber} at the specified position.
     *
     * @param index the position in the array
     * @param value the number value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @see JsonNumber
     *
     * @since 1.1
     */
    default JsonArrayBuilder set(int index, BigDecimal value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replaces a value in the array with the specified value as a
     * {@link JsonNumber} at the specified position.
     *
     * @param index the position in the array
     * @param value the number value
     * @return this array builder
     * @throws NullPointerException if the specified value is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @see JsonNumber
     *
     * @since 1.1
     */
    default JsonArrayBuilder set(int index, BigInteger value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replaces a value in the array with the specified value as a
     * {@link JsonNumber} at the specified position.
     *
     * @param index the position in the array
     * @param value the number value
     * @return this array builder
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @see JsonNumber
     *
     * @since 1.1
     */
    default JsonArrayBuilder set(int index, int value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replaces a value in the array with the specified value as a
     * {@link JsonNumber} at the specified position.
     *
     * @param index the position in the array
     * @param value the number value
     * @return this array builder
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @see JsonNumber
     *
     * @since 1.1
     */
    default JsonArrayBuilder set(int index, long value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replaces a value in the array with the specified value as a
     * {@link JsonNumber} at the specified position.
     *
     * @param index the position in the array
     * @param value the number value
     * @return this array builder
     * @throws NumberFormatException if the value is Not-a-Number (NaN) or
     *      infinity
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @see JsonNumber
     *
     * @since 1.1
     */
    default JsonArrayBuilder set(int index, double value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replaces a value in the array with
     * a {@link JsonValue#TRUE}  or {@link JsonValue#FALSE} value
     * at the specified position.
     *
     * @param index the position in the array
     * @param value the boolean value
     * @return this array builder
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder set(int index, boolean value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replaces a value in the array with
     * a {@link JsonValue#NULL} value at the specified position.
     *
     * @param index the position in the array
     * @return this array builder
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder setNull(int index) {
        return set(index, JsonValue.NULL);
    }

    /**
     * Replaces a value in the array with the specified value as a
     * {@link JsonObject} from an object builder at the specified position.
     *
     * @param index the position in the array
     * @param builder the object builder
     * @return this array builder
     * @throws NullPointerException if the specified builder is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder set(int index, JsonObjectBuilder builder) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replaces a value in the array with the specified value as a
     * {@link JsonArray} from an array builder at the specified position.
     *
     * @param index the position in the array
     * @param builder the array builder
     * @return this array builder
     * @throws NullPointerException if the specified builder is null
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder set(int index, JsonArrayBuilder builder) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove the value in the array at the specified position.
     * Shift any subsequent values to the left (subtracts one from their
     * indices.
     *
     * @param index the position in the array
     * @return this array builder
     * @throws IndexOutOfBoundsException if the index is out of range
     *   {@code (index < 0 || index >= array size)}
     *
     * @since 1.1
     */
    default JsonArrayBuilder remove(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the current array.
     *
     * @return the current JSON array
     */
    JsonArray build();

}

