/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;

/**
 * This class is a helper class for JsonPointer implementation,
 * and is not part of the API.
 *
 * This class encapsulates a reference to a JSON node.
 * There are three types of references.
 * <ol><li>a reference to the root of a JSON tree.</li>
 *     <li>a reference to a name/value (possibly non-existing) pair of a JSON object, identified by a name.</li>
 *     <li>a reference to a member value of a JSON array, identified by an index.</li>
 * </ol>
 * Static factory methods are provided for creating these references.
 *
 * <p>A referenced value can be retrieved or replaced.
 * The value of a JSON object or JSON array can be
 * removed.  A new value can be added to a JSON object or
 * inserted into a JSON array</p>
 *
 * <p>Since a {@code JsonObject} or {@code JsonArray} is immutable, these operations
 * must not modify the referenced JSON object or array. The methods {@link #add},
 * {@link #replace}, and {@link #remove} returns a new
 * JSON object or array after the execution of the operation.</p>
 */
abstract class NodeReference {

    /**
     * Return {@code true} if a reference points to a valid value, {@code false} otherwise.
     *
     * @return {@code true} if a reference points to a value
     */
    abstract public boolean contains();

    /**
     * Get the value at the referenced location.
     *
     * @return the JSON value referenced
     * @throws JsonException if the referenced value does not exist
     */
    abstract public JsonValue get();

    /**
     * Add or replace a value at the referenced location.
     * If the reference is the root of a JSON tree, the added value must be
     * a JSON object or array, which becomes the referenced JSON value.
     * If the reference is an index of a JSON array, the value is inserted
     * into the array at the index.  If the index is -1, the value is
     * appended to the array.
     * If the reference is a name of a JSON object, the name/value pair is added
     * to the object, replacing any pair with the same name.
     *
     * @param value the value to be added
     * @return the JsonStructure after the operation
     * @throws JsonException if the index to the array is not -1 or is out of range
     */
    abstract public JsonStructure add(JsonValue value);

    /**
     * Remove the name/value pair from the JSON object, or the value in a JSON array, as specified by the reference
     *
     * @return the JsonStructure after the operation
     * @throws JsonException if the name/value pair of the referenced JSON object
     *    does not exist, or if the index of the referenced JSON array is
     *    out of range, or if the reference is a root reference
     */
    abstract public JsonStructure remove();

    /**
     * Replace the referenced value with the specified value.
     *
     * @param value the JSON value to be stored at the referenced location
     * @return the JsonStructure after the operation
     * @throws JsonException if the name/value pair of the referenced JSON object
     *    does not exist, or if the index of the referenced JSON array is
     *    out of range, or if the reference is a root reference
     */
    abstract public JsonStructure replace(JsonValue value);

    /**
     * Returns a {@code NodeReference} for a {@code JsonStructure}.
     *
     * @param structure the {@code JsonStructure} referenced
     * @return the {@code NodeReference}
     */
    public static NodeReference of(JsonStructure structure) {
        return new RootReference(structure);
    }

    /**
     * Returns a {@code NodeReference} for a name/value pair in a
     * JSON object.
     *
     * @param object the referenced JSON object
     * @param name the name of the name/pair
     * @return the {@code NodeReference}
     */
    public static NodeReference of(JsonObject object, String name) {
        return new ObjectReference(object, name);
    }

    /**
     * Returns a {@code NodeReference} for a member value in a
     * JSON array.
     *
     * @param array the referenced JSON array
     * @param index the index of the member value in the JSON array
     * @return the {@code NodeReference}
     */
    public static NodeReference of(JsonArray array, int index) {
        return new ArrayReference(array, index);
    }

    static class RootReference extends NodeReference {

        private JsonStructure root;

        RootReference(JsonStructure root) {
            this.root = root;
        }

        @Override
        public boolean contains() {
            return root != null;
        }

        @Override
        public JsonValue get() {
            return root;
        }

        @Override
        public JsonStructure add(JsonValue value) {
            switch (value.getValueType() ) {
                case OBJECT:
                case ARRAY:
                    this.root = (JsonStructure) value;
                    break;
                default:
                    throw new JsonException(JsonMessages.NODEREF_VALUE_ADD_ERR());
            }
            return root;
        }

        @Override
        public JsonStructure remove() {
            throw new JsonException(JsonMessages.NODEREF_VALUE_CANNOT_REMOVE());
        }

        @Override
        public JsonStructure replace(JsonValue value) {
            return add(value);
        }
    }

    static class ObjectReference extends NodeReference {

        private final JsonObject object;
        private final String key;

        ObjectReference(JsonObject object, String key) {
            this.object = object;
            this.key = key;
        }

        @Override
        public boolean contains() {
            return object != null && object.containsKey(key);
        }

        @Override
        public JsonValue get() {
            if (!contains()) {
                throw new JsonException(JsonMessages.NODEREF_OBJECT_MISSING(key));
            }
            return object.get(key);
        }

        @Override
        public JsonObject add(JsonValue value) {
            return Json.createObjectBuilder(object).add(key, value).build();
        }

        @Override
        public JsonObject remove() {
            if (!contains()) {
                throw new JsonException(JsonMessages.NODEREF_OBJECT_MISSING(key));
            }
            return Json.createObjectBuilder(object).remove(key).build();
        }

        @Override
        public JsonObject replace(JsonValue value) {
            if (!contains()) {
                throw new JsonException(JsonMessages.NODEREF_OBJECT_MISSING(key));
            }
            return add(value);
        }
    }

    static class ArrayReference extends NodeReference {

        private final JsonArray array;
        private final int index; // -1 means "-" in JSON Pointer

        ArrayReference(JsonArray array, int index) {
            this.array = array;
            this.index = index;
        }

        @Override
        public boolean contains() {
            return array != null && index > -1 && index < array.size();
        }

        @Override
        public JsonValue get() {
            if (!contains()) {
                throw new JsonException(JsonMessages.NODEREF_ARRAY_INDEX_ERR(index, array.size()));
            }
            return array.get(index);
        }

        @Override
        public JsonArray add(JsonValue value) {
            //TODO should we check for arrayoutofbounds?
            // The spec seems to say index = array.size() is allowed. This is handled as append
            JsonArrayBuilder builder = Json.createArrayBuilder(this.array);
            if (index == -1 || index == array.size()) {
                builder.add(value);
            } else {
                if(index < array.size()) {
                    builder.add(index, value);
                } else {
                    throw new JsonException(JsonMessages.NODEREF_ARRAY_INDEX_ERR(index, array.size()));
                }
            }
            return builder.build();
        }

        @Override
        public JsonArray remove() {
            if (!contains()) {
                throw new JsonException(JsonMessages.NODEREF_ARRAY_INDEX_ERR(index, array.size()));
            }
            JsonArrayBuilder builder = Json.createArrayBuilder(this.array);
            return builder.remove(index).build();
        }

        @Override
        public JsonArray replace(JsonValue value) {
            if (!contains()) {
                throw new JsonException(JsonMessages.NODEREF_ARRAY_INDEX_ERR(index, array.size()));
            }
            JsonArrayBuilder builder = Json.createArrayBuilder(this.array);
            return builder.set(index, value).build();
        }
    }
}

