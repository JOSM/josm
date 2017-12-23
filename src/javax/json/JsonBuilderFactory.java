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

import java.util.Collection;
import java.util.Map;

/**
 * Factory to create {@link JsonObjectBuilder} and {@link JsonArrayBuilder}
 * instances. If a factory instance is configured with some configuration,
 * that would be used to configure the created builder instances.
 *
 * <p>
 * {@code JsonObjectBuilder} and {@code JsonArrayBuilder} can also be created
 * using {@link Json}'s methods. If multiple builder instances are created,
 * then creating them using a builder factory is preferred.
 *
 * <p>
 * <b>For example:</b>
 * <pre>
 * <code>
 * JsonBuilderFactory factory = Json.createBuilderFactory(...);
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
 * <p> All the methods in this class are safe for use by multiple concurrent
 * threads.
 */
public interface JsonBuilderFactory {

    /**
     * Creates a {@code JsonObjectBuilder} instance that is used to build
     * {@link JsonObject}.
     *
     * @return a JSON object builder
     */
    JsonObjectBuilder createObjectBuilder();

    /**
     * Creates a {@code JsonObjectBuilder} instance, initialized with an object.
     *
     * @param object the initial object in the builder
     * @return a JSON object builder
     * @throws NullPointerException if specified object is {@code null}
     *
     * @since 1.1
     */
    default JsonObjectBuilder createObjectBuilder(JsonObject object) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a {@code JsonObjectBuilder} instance, initialized with the specified object.
     *
     * @param object the initial object in the builder
     * @return a JSON object builder
     * @throws NullPointerException if specified object is {@code null}
     *
     * @since 1.1
     */
    default JsonObjectBuilder createObjectBuilder(Map<String, Object> object) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a {@code JsonArrayBuilder} instance that is used to build
     * {@link JsonArray}
     *
     * @return a JSON array builder
     */
    JsonArrayBuilder createArrayBuilder();

    /**
     * Creates a {@code JsonArrayBuilder} instance, initialized with an array.
     *
     * @param array the initial array in the builder
     * @return a JSON array builder
     * @throws NullPointerException if specified array is {@code null}
     *
     * @since 1.1
     */
    default JsonArrayBuilder createArrayBuilder(JsonArray array) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a {@code JsonArrayBuilder} instance,
     * initialized with the content of specified collection.
     *
     * @param collection the initial data for the builder
     * @return a JSON array builder
     * @throws NullPointerException if specified collection is {@code null}
     *
     * @since 1.1
     */
    default JsonArrayBuilder createArrayBuilder(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns read-only map of supported provider specific configuration
     * properties that are used to configure the created JSON builders.
     * If there are any specified configuration properties that are not
     * supported by the provider, they won't be part of the returned map.
     *
     * @return a map of supported provider specific properties that are used
     * to configure the builders. The map be empty but not null.
     */
    Map<String, ?> getConfigInUse();

}
