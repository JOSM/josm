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

import javax.json.*;
import javax.json.JsonValue.ValueType;

/**
 * This class is an immutable representation of a JSON Patch as specified in
 * <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>.
 * <p>A {@code JsonPatch} can be instantiated with {@link Json#createPatch(JsonArray)}
 * by specifying the patch operations in a JSON Patch. Alternately, it
 * can also be constructed with a {@link JsonPatchBuilder}.
 * </p>
 * The following illustrates both approaches.
 * <p>1. Construct a JsonPatch with a JSON Patch.
 * <pre>{@code
 *   JsonArray contacts = ... // The target to be patched
 *   JsonArray patch = ...  ; // JSON Patch
 *   JsonPatch jsonpatch = Json.createPatch(patch);
 *   JsonArray result = jsonpatch.apply(contacts);
 * } </pre>
 * 2. Construct a JsonPatch with JsonPatchBuilder.
 * <pre>{@code
 *   JsonPatchBuilder builder = Json.createPatchBuilder();
 *   JsonArray result = builder.add("/John/phones/office", "1234-567")
 *                             .remove("/Amy/age")
 *                             .build()
 *                             .apply(contacts);
 * } </pre>
 *
 * @since 1.1
 */
public class JsonPatchImpl implements JsonPatch {

    private final JsonArray patch;

    /**
     * Constructs a JsonPatchImpl
     * @param patch the JSON Patch
     */
    public JsonPatchImpl(JsonArray patch) {
        this.patch = patch;
    }

    /**
     * Compares this {@code JsonPatchImpl} with another object.
     * @param obj the object to compare this {@code JsonPatchImpl} against
     * @return true if the given object is a {@code JsonPatchImpl} with the same
     * reference tokens as this one, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || obj.getClass() != JsonPatchImpl.class)
            return false;
        return patch.equals(((JsonPatchImpl)obj).patch);
    }

    /**
     * Returns the hash code value for this {@code JsonPatchImpl}.
     *
     * @return the hash code value for this {@code JsonPatchImpl} object
     */
    @Override
    public int hashCode() {
        return patch.hashCode();
    }

    /**
     * Returns the JSON Patch text
     * @return the JSON Patch text
     */
    @Override
    public String toString() {
        return patch.toString();
    }

    /**
     * Applies the patch operations to the specified {@code target}.
     * The target is not modified by the patch.
     *
     * @param target the target to apply the patch operations
     * @return the transformed target after the patch
     * @throws JsonException if the supplied JSON Patch is malformed or if
     *    it contains references to non-existing members
     */
    @Override
    public JsonStructure apply(JsonStructure target) {

        JsonStructure result = target;

        for (JsonValue operation: patch) {
            if (operation.getValueType() != ValueType.OBJECT) {
                throw new JsonException(JsonMessages.PATCH_MUST_BE_ARRAY());
            }
            result = apply(result, (JsonObject) operation);
        }
        return result;
    }

    @Override
    public JsonArray toJsonArray() {
        return patch;
    }

    /**
     * Generates a JSON Patch from the source and target {@code JsonStructure}.
     * The generated JSON Patch need not be unique.
     * @param source the source
     * @param target the target, must be the same type as the source
     * @return a JSON Patch which when applied to the source, yields the target
     */
    public static JsonArray diff(JsonStructure source, JsonStructure target) {
        return (new DiffGenerator()).diff(source, target);
    }

    /**
     * Applies a JSON Patch operation to the target.
     * @param target the target to apply the operation
     * @param operation the JSON Patch operation
     * @return the target after the patch
     */
    private JsonStructure apply(JsonStructure target, JsonObject operation) {

        JsonPointer pointer = getPointer(operation, "path");
        JsonPointer from;
        switch (Operation.fromOperationName(operation.getString("op"))) {
            case ADD:
                return pointer.add(target, getValue(operation));
            case REPLACE:
                return pointer.replace(target, getValue(operation));
            case REMOVE:
                return pointer.remove(target);
            case COPY:
                from = getPointer(operation, "from");
                return pointer.add(target, from.getValue(target));
            case MOVE:
                // Check if from is a proper prefix of path
                String dest = operation.getString("path");
                String src = operation.getString("from");
                if (dest.startsWith(src) && src.length() < dest.length()) {
                    throw new JsonException(JsonMessages.PATCH_MOVE_PROPER_PREFIX(src, dest));
                }
                from = getPointer(operation, "from");
                // Check if 'from' exists in target object
                if (!from.containsValue(target)) {
                    throw new JsonException(JsonMessages.PATCH_MOVE_TARGET_NULL(src));
                }
                if (pointer.equals(from)) {
                    // nop
                    return target;
                }
                return pointer.add(from.remove(target), from.getValue(target));
            case TEST:
                if (! getValue(operation).equals(pointer.getValue(target))) {
                    throw new JsonException(JsonMessages.PATCH_TEST_FAILED(operation.getString("path"), getValue(operation).toString()));
                }
                return target;
            default:
                throw new JsonException(JsonMessages.PATCH_ILLEGAL_OPERATION(operation.getString("op")));
        }
    }

    private JsonPointer getPointer(JsonObject operation, String member) {
        JsonString pointerString = operation.getJsonString(member);
        if (pointerString == null) {
            missingMember(operation.getString("op"), member);
        }
        return Json.createPointer(pointerString.getString());
    }

    private JsonValue getValue(JsonObject operation) {
        JsonValue value = operation.get("value");
        if (value == null) {
            missingMember(operation.getString("op"), "value");
        }
        return value;
    }

    private void missingMember(String op, String  member) {
        throw new JsonException(JsonMessages.PATCH_MEMBER_MISSING(op, member));
    }

    static class DiffGenerator {
        private JsonPatchBuilder builder;

        JsonArray diff(JsonStructure source, JsonStructure target) {
            builder = Json.createPatchBuilder();
            diff("", source, target);
            return builder.build().toJsonArray();
        }

        private void diff(String path, JsonValue source, JsonValue target) {
            if (source.equals(target)) {
                return;
            }
            ValueType s = source.getValueType();
            ValueType t = target.getValueType();
            if (s == ValueType.OBJECT && t == ValueType.OBJECT) {
                diffObject(path, (JsonObject) source, (JsonObject) target);
            } else if (s == ValueType.ARRAY && t == ValueType.ARRAY) {
                diffArray(path, (JsonArray) source, (JsonArray) target);
            } else {
                builder.replace(path, target);
            }
        }

        private void diffObject(String path, JsonObject source, JsonObject target) {
            source.forEach((key, value) -> {
                if (target.containsKey(key)) {
                    diff(path + '/' + key, value, target.get(key));
                } else {
                    builder.remove(path + '/' + key);
                }
            });
            target.forEach((key, value) -> {
                if (! source.containsKey(key)) {
                    builder.add(path + '/' + key, value);
                }
            });
        }

        /*
         * For array element diff, find the longest common subsequence, per
         * http://en.wikipedia.org/wiki/Longest_common_subsequence_problem .
         * We modify the algorithm to generate a replace if possible.
         */
        private void diffArray(String path, JsonArray source, JsonArray target) {
            /* The array c keeps track of length of the subsequence. To avoid
             * computing the equality of array elements again, we
             * left shift its value by 1, and use the low order bit to mark
             * that two items are equal.
             */
            int m = source.size();
            int n = target.size();
            int [][] c = new int[m+1][n+1];
            for (int i = 0; i < m+1; i++)
                c[i][0] = 0;
            for (int i = 0; i < n+1; i++)
                c[0][i] = 0;
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    if (source.get(i).equals(target.get(j))) {
                        c[i+1][j+1] = ((c[i][j]) & ~1) + 3;
                        // 3 = (1 << 1) | 1;
                    } else {
                        c[i+1][j+1] = Math.max(c[i+1][j], c[i][j+1]) & ~1;
                    }
                }
            }

            int i = m;
            int j = n;
            while (i > 0 || j > 0) {
                if (i == 0) {
                    j--;
                    builder.add(path + '/' + j, target.get(j));
                } else if (j == 0) {
                    i--;
                    builder.remove(path + '/' + i);
                } else if ((c[i][j] & 1) == 1) {
                    i--; j--;
                } else {
                    int f = c[i][j-1] >> 1;
                    int g = c[i-1][j] >> 1;
                    if (f > g) {
                        j--;
                        builder.add(path + '/' + j, target.get(j));
                    } else if (f < g) {
                        i--;
                        builder.remove(path + '/' + i);
                    } else { // f == g) {
                       i--; j--;
                       diff(path + '/' + i, source.get(i), target.get(j));
                    }
                }
            }
        }
    }
}

