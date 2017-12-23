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

package javax.json;

/**
 * <p>This interface represents an immutable implementation of a JSON Patch
 * as defined by <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>.
 * </p>
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
 * @see <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>
 *
 * @since 1.1
 */
public interface JsonPatch {

    /**
     * This enum represents the list of valid JSON Patch operations
     * as defined by <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>.
     *
     * @see <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>
     */
    enum Operation {

        /**
         * "add" operation.
         */
        ADD("add"),

        /**
         * "remove" operation.
         */
        REMOVE("remove"),

        /**
         * "remove" operation.
         */
        REPLACE("replace"),

        /**
         * "move" operation.
         */
        MOVE("move"),

        /**
         * "copy" operation.
         */
        COPY("copy"),

        /**
         * "test" operation.
         */
        TEST("test");

        private final String operationName;

        private Operation(String operationName) {
            this.operationName = operationName;
        }

        /**
         * Returns enum constant name as lower case string.
         *
         * @return lower case name of the enum constant
         */
        public String operationName() {
            return operationName;
        }

        /**
         * Returns the enum constant with the specified name.
         *
         * @param operationName {@code operationName} to convert to the enum constant.
         * @return the enum constant for given {@code operationName}
         * @throws JsonException if given {@code operationName} is not recognized
         */
        public static Operation fromOperationName(String operationName) {
            for (Operation op : values()) {
                if (op.operationName().equalsIgnoreCase(operationName)) {
                    return op;
                }
            }
            throw new JsonException("Illegal value for the operationName of the JSON patch operation: " + operationName);
        }
    }

    /**
     * Applies the patch operations to the specified {@code target}.
     * The target is not modified by the patch.
     *
     * @param <T> the target type, must be a subtype of {@link JsonStructure}
     * @param target the target to apply the patch operations
     * @return the transformed target after the patch
     * @throws JsonException if the supplied JSON Patch is malformed or if
     *    it contains references to non-existing members
     */
    <T extends JsonStructure> T apply(T target);

    /**
     * Returns the {@code JsonPatch} as {@code JsonArray}.
     *
     * @return this {@code JsonPatch} as {@code JsonArray}
     */
    JsonArray toJsonArray();

}
