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
 * A builder for creating {@link JsonObject} models from scratch. This
 * interface initializes an empty JSON object model and provides methods to add
 * name/value pairs to the object model and to return the resulting object.
 * The methods in this class can be chained to add multiple name/value pairs
 * to the object.
 *
 * <p>The class {@link javax.json.Json} contains methods to create the builder
 * object. The example code below shows how to build an empty {@code JsonObject}
 * instance.
 * <pre>
 * <code>
 * JsonObject object = Json.createObjectBuilder().build();
 * </code>
 * </pre>
 *
 * <p>The class {@link JsonBuilderFactory} also contains methods to create
 * {@code JsonObjectBuilder} instances. A factory instance can be used to create
 * multiple builder instances with the same configuration. This the preferred
 * way to create multiple instances.
 *
 * The example code below shows how to build a {@code JsonObject} model that
 * represents the following JSON object:
 *
 * <pre>
 * <code>
 * {
 *     "firstName": "John", "lastName": "Smith", "age": 25,
 *     "address" : {
 *         "streetAddress": "21 2nd Street",
 *         "city": "New York",
 *         "state": "NY",
 *         "postalCode": "10021"
 *     },
 *     "phoneNumber": [
 *         { "type": "home", "number": "212 555-1234" },
 *         { "type": "fax", "number": "646 555-4567" }
 *     ]
 * }
 * </code>
 * </pre>
 *
 * <p>The code to create the object shown above is the following:
 *
 * <pre>
 * <code>
 * JsonBuilderFactory factory = Json.createBuilderFactory(config);
 * JsonObject value = factory.createObjectBuilder()
 *     .add("firstName", "John")
 *     .add("lastName", "Smith")
 *     .add("age", 25)
 *     .add("address", factory.createObjectBuilder()
 *         .add("streetAddress", "21 2nd Street")
 *         .add("city", "New York")
 *         .add("state", "NY")
 *         .add("postalCode", "10021"))
 *     .add("phoneNumber", factory.createArrayBuilder()
 *         .add(factory.createObjectBuilder()
 *             .add("type", "home")
 *             .add("number", "212 555-1234"))
 *         .add(factory.createObjectBuilder()
 *             .add("type", "fax")
 *             .add("number", "646 555-4567")))
 *     .build();
 * </code>
 * </pre>
 *
 * <p>This class does <em>not</em> allow <tt>null</tt> to be used as a name or
 * value while building the JSON object
 *
 * @see JsonArrayBuilder
 */
public interface JsonObjectBuilder {

    /**
     * Adds a name/{@code JsonValue} pair to the JSON object associated with
     * this object builder. If the object contains a mapping for the specified
     * name, this method replaces the old value with the specified value.
     *
     * @param name name in the name/value pair
     * @param value value in the name/value pair
     * @return this object builder
     * @throws NullPointerException if the specified name or value is null
     */
    JsonObjectBuilder add(String name, JsonValue value);

    /**
     * Adds a name/{@code JsonString} pair to the JSON object associated with
     * this object builder. If the object contains a mapping for the specified
     * name, this method replaces the old value with the specified value.
     *
     * @param name name in the name/value pair
     * @param value value in the name/value pair
     * @return this object builder
     * @throws NullPointerException if the specified name or value is null
     */
    JsonObjectBuilder add(String name, String value);

    /**
     * Adds a name/{@code JsonNumber} pair to the JSON object associated with
     * this object builder. If the object contains a mapping for the specified
     * name, this method replaces the old value with the specified value.
     *
     * @param name name in the name/value pair
     * @param value value in the name/value pair
     * @return this object builder
     * @throws NullPointerException if the specified name or value is null
     *
     * @see JsonNumber
     */
    JsonObjectBuilder add(String name, BigInteger value);

    /**
     * Adds a name/{@code JsonNumber} pair to the JSON object associated with
     * this object builder. If the object contains a mapping for the specified
     * name, this method replaces the old value with the specified value.
     *
     * @param name name in the name/value pair
     * @param value value in the name/value pair
     * @return this object builder
     * @throws NullPointerException if the specified name or value is null
     *
     * @see JsonNumber
     */
    JsonObjectBuilder add(String name, BigDecimal value);

    /**
     * Adds a name/{@code JsonNumber} pair to the JSON object associated with
     * this object builder. If the object contains a mapping for the specified
     * name, this method replaces the old value with the specified value.
     *
     * @param name name in the name/value pair
     * @param value value in the name/value pair
     * @return this object builder
     * @throws NullPointerException if the specified name is null
     *
     * @see JsonNumber
     */
    JsonObjectBuilder add(String name, int value);

    /**
     * Adds a name/{@code JsonNumber} pair to the JSON object associated with
     * this object builder. If the object contains a mapping for the specified
     * name, this method replaces the old value with the specified value.
     *
     * @param name name in the name/value pair
     * @param value value in the name/value pair
     * @return this object builder
     * @throws NullPointerException if the specified name is null
     *
     * @see JsonNumber
     */
    JsonObjectBuilder add(String name, long value);

    /**
     * Adds a name/{@code JsonNumber} pair to the JSON object associated with
     * this object builder. If the object contains a mapping for the specified
     * name, this method replaces the old value with the specified value.
     *
     * @param name name in the name/value pair
     * @param value value in the name/value pair
     * @return this object builder
     * @throws NumberFormatException if the value is Not-a-Number (NaN) or 
     * infinity
     * @throws NullPointerException if the specified name is null
     *
     * @see JsonNumber
     */
    JsonObjectBuilder add(String name, double value);

    /**
     * Adds a name/{@code JsonValue#TRUE} or name/{@code JsonValue#FALSE} pair
     * to the JSON object associated with this object builder. If the object
     * contains a mapping for the specified name, this method replaces the old
     * value with the specified value.
     *
     * @param name name in the name/value pair
     * @param value value in the name/value pair
     * @return this object builder
     * @throws NullPointerException if the specified name is null
     */
    JsonObjectBuilder add(String name, boolean value);

    /**
     * Adds a name/{@code JsonValue#NULL} pair to the JSON object associated
     * with this object builder where the value is {@code null}.
     * If the object contains a mapping for the specified name, this method
     * replaces the old value with {@code null}.
     *
     * @param name name in the name/value pair
     * @return this object builder
     * @throws NullPointerException if the specified name is null
     */
    JsonObjectBuilder addNull(String name);

    /**
     * Adds a name/{@code JsonObject} pair to the JSON object associated
     * with this object builder. The value {@code JsonObject} is built from the
     * specified object builder. If the object contains a mapping for the
     * specified name, this method replaces the old value with the
     * {@code JsonObject} from the specified object builder.
     *
     * @param name name in the name/value pair
     * @param builder the value is the object associated with this builder
     * @return this object builder
     * @throws NullPointerException if the specified name or builder is null
     */
    JsonObjectBuilder add(String name, JsonObjectBuilder builder);

    /**
     * Adds a name/{@code JsonArray} pair to the JSON object associated with
     * this object builder. The value {@code JsonArray} is built from the
     * specified array builder. If the object contains a mapping for the
     * specified name, this method replaces the old value with the
     * {@code JsonArray} from the specified array builder.
     *
     * @param name the name in the name/value pair
     * @param builder the value is the object array with this builder
     * @return this object builder
     * @throws NullPointerException if the specified name or builder is null
     */
    JsonObjectBuilder add(String name, JsonArrayBuilder builder);

    /**
     * Adds all name/value pairs in the JSON object associated with the specified
     * object builder to the JSON object associated with this object builder.
     * The newly added name/value pair will replace any existing name/value pair with
     * the same name.
     *
     * @param builder the specified object builder
     * @return this object builder
     * @throws NullPointerException if the specified builder is null
     * @since 1.1
     */
    default JsonObjectBuilder addAll(JsonObjectBuilder builder) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove the name/value pair from the JSON object associated with this
     * object builder if it is present.
     *
     * @param name the name in the name/value pair to be removed
     * @return this object builder
     * @throws NullPointerException if the specified name is null
     * @since 1.1
     */
    default JsonObjectBuilder remove(String name) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the JSON object associated with this object builder. 
     * The iteration order for the {@code JsonObject} is based
     * on the order in which name/value pairs are added to the object using
     * this builder.
     *
     * @return JSON object that is being built
     */
    JsonObject build();

}
