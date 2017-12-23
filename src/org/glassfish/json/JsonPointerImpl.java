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

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonPointer;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * <p>This class is an immutable representation of a JSON Pointer as specified in
 * <a href="http://tools.ietf.org/html/rfc6901">RFC 6901</a>.
 * </p>
 * <p> A JSON Pointer, when applied to a target {@link JsonValue},
 * defines a reference location in the target.</p>
 * <p> An empty JSON Pointer string defines a reference to the target itself.</p>
 * <p> If the JSON Pointer string is non-empty, it must be a sequence
 * of '/' prefixed tokens, and the target must either be a {@link JsonArray}
 * or {@link JsonObject}. If the target is a {@code JsonArray}, the pointer
 * defines a reference to an array element, and the last token specifies the index.
 * If the target is a {@link JsonObject}, the pointer defines a reference to a
 * name/value pair, and the last token specifies the name.
 * </p>
 * <p> The method {@link #getValue getValue()} returns the referenced value.
 * The methods {@link #add add()}, {@link #replace replace()},
 * and {@link #remove remove()} executes the operations specified in
 * <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>. </p>
 *
 * @since 1.1
 */

public final class JsonPointerImpl implements JsonPointer, Serializable {

    private static final long serialVersionUID = -8123110179640843141L;
    private final String[] tokens;
    private final String jsonPointer;

    /**
     * Constructs and initializes a JsonPointerImpl.
     * @param jsonPointer the JSON Pointer string
     * @throws NullPointerException if {@code jsonPointer} is {@code null}
     * @throws JsonException if {@code jsonPointer} is not a valid JSON Pointer
     */
    public JsonPointerImpl(String jsonPointer) {
        this.jsonPointer = jsonPointer;
        tokens = jsonPointer.split("/", -1);  // keep the trailing blanks
        if (! "".equals(tokens[0])) {
            throw new JsonException(JsonMessages.POINTER_FORMAT_INVALID());
        }
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            StringBuilder reftoken = new StringBuilder();
            for (int j = 0; j < token.length(); j++) {
                char ch = token.charAt(j);
                if (ch == '~' && j < token.length() - 1) {
                    char ch1 = token.charAt(j+1);
                    if (ch1 == '0') {
                        ch = '~'; j++;
                    } else if (ch1 == '1') {
                        ch = '/'; j++;
                    }
                }
                reftoken.append(ch);
            }
            tokens[i] = reftoken.toString();
        }
    }

    /**
     * Compares this {@code JsonPointer} with another object.
     * @param obj the object to compare this {@code JsonPointer} against
     * @return true if the given object is a {@code JsonPointer} with the same
     * reference tokens as this one, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || obj.getClass() != JsonPointerImpl.class)
            return false;
        return jsonPointer.equals(((JsonPointerImpl)obj).jsonPointer);
    }

    /**
     * Returns the hash code value for this {@code JsonPointer} object.
     * The hash code of this object is defined by the hash codes of it's reference tokens.
     *
     * @return the hash code value for this {@code JsonPointer} object
     */
    @Override
    public int hashCode() {
        return jsonPointer.hashCode();
    }

    /**
     * Returns {@code true} if there is a value at the referenced location in the specified {@code target}.
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @return {@code true} if this pointer points to a value in a specified {@code target}.
     */
    @Override
    public boolean containsValue(JsonStructure target) {
        NodeReference[] refs = getReferences(target);
        return refs[0].contains();
    }

    /**
     * Returns the value at the referenced location in the specified {@code target}
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @return the referenced value in the target.
     * @throws NullPointerException if {@code target} is null
     * @throws JsonException if the referenced value does not exist
     */
    @Override
    public JsonValue getValue(JsonStructure target) {
        NodeReference[] refs = getReferences(target);
        return refs[0].get();
    }

    /**
     * Adds or replaces a value at the referenced location in the specified
     * {@code target} with the specified {@code value}.
     * <ol>
     * <li>If the reference is the target (empty JSON Pointer string),
     * the specified {@code value}, which must be the same type as
     * specified {@code target}, is returned.</li>
     * <li>If the reference is an array element, the specified {@code value} is inserted
     * into the array, at the referenced index. The value currently at that location, and
     * any subsequent values, are shifted to the right (adds one to the indices).
     * Index starts with 0. If the reference is specified with a "-", or if the
     * index is equal to the size of the array, the value is appended to the array.</li>
     * <li>If the reference is a name/value pair of a {@code JsonObject}, and the
     * referenced value exists, the value is replaced by the specified {@code value}.
     * If the value does not exist, a new name/value pair is added to the object.</li>
     * </ol>
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @param value the value to be added
     * @return the transformed {@code target} after the value is added.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException if the reference is an array element and
     * the index is out of range ({@code index < 0 || index > array size}),
     * or if the pointer contains references to non-existing objects or arrays.
     */
    @Override
    public JsonStructure add(JsonStructure target, JsonValue value) {
        return execute(NodeReference::add, target, value);
    }

    /**
     * Replaces the value at the referenced location in the specified
     * {@code target} with the specified {@code value}.
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @param value the value to be stored at the referenced location
     * @return the transformed {@code target} after the value is replaced.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException if the referenced value does not exist,
     *    or if the reference is the target.
     */
    @Override
    public JsonStructure replace(JsonStructure target, JsonValue value) {
        return execute(NodeReference::replace, target, value);
    }

    /**
     * Removes the value at the reference location in the specified {@code target}
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @return the transformed {@code target} after the value is removed.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException if the referenced value does not exist,
     *    or if the reference is the target.
     */
    @Override
    public JsonStructure remove(JsonStructure target) {
        return execute((r,v)->r.remove(), target, null);
    }

    /**
     * Executes the operation
     * @param op a {code BiFunction} used to specify the operation to execute on
     *    the leaf node of the Json Pointer
     * @param target the target JsonStructure for this JsonPointer
     * @param value the JsonValue for add and replace, can be null for getvalue and remove
     */
    private JsonStructure execute(BiFunction<NodeReference, JsonValue, JsonStructure> op,
            JsonStructure target, JsonValue value) {

        NodeReference[] refs = getReferences(target);
        JsonStructure result = op.apply(refs[0], value);
        for (int i = 1; i < refs.length; i++) {
            result = refs[i].replace(result);
        }
        return result;
    }

    /**
     * Computes the {@code NodeReference}s for each node on the path of
     * the JSON Pointer, in reverse order, starting from the leaf node
     */
    private NodeReference[] getReferences(JsonStructure target) {
        NodeReference[] references;
        // First check if this is a reference to a JSON value tree
        if (tokens.length == 1) {
            references = new NodeReference[1];
            references[0] = NodeReference.of(target);
            return references;
        }

        references = new NodeReference[tokens.length-1];
        JsonValue value = target;
        int s = tokens.length;
        for (int i = 1; i < s; i++) {
             // Start with index 1, skipping the "" token
            switch (value.getValueType()) {
                case OBJECT:
                    JsonObject object = (JsonObject) value;
                    references[s-i-1] = NodeReference.of(object, tokens[i]);
                    if (i < s-1) {
                        value = object.get(tokens[i]);
                        if (value == null) {
                            // Except for the last name, the mapping must exist
                            throw new JsonException(JsonMessages.POINTER_MAPPING_MISSING(object, tokens[i]));
                        }
                    }
                    break;
                case ARRAY:
                    int index = getIndex(tokens[i]);
                    JsonArray array = (JsonArray) value;
                    references[s-i-1] = NodeReference.of(array, index);
                    if (i < s-1 && index != -1) {
                        if (index >= array.size()) {
                            throw new JsonException(JsonMessages.NODEREF_ARRAY_INDEX_ERR(index, array.size()));
                        }
                        // The last array index in the path can have index value of -1
                        // ("-" in the JSON pointer)
                        value = array.get(index);
                    }
                    break;
                default:
                    throw new JsonException(JsonMessages.POINTER_REFERENCE_INVALID(value.getValueType()));
             }
        }
        return references;
    }

    /**
     * Computes the array index
     * @param token the input string token
     * @return the array index. -1 if the token is "-"
     * @throws JsonException if the string token is not in correct format
     */
    static private int getIndex(String token) {
        if (token == null || token.length() == 0) {
            throw new JsonException(JsonMessages.POINTER_ARRAY_INDEX_ERR(token));
        }
        if (token.equals("-")) {
            return -1;
        }
        if (token.equals("0")) {
            return 0;
        }
        if (token.charAt(0) == '+' || token.charAt(0) == '-') {
            throw new JsonException(JsonMessages.POINTER_ARRAY_INDEX_ERR(token));
        }
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ex) {
            throw new JsonException(JsonMessages.POINTER_ARRAY_INDEX_ILLEGAL(token), ex);
       }
    }
}
