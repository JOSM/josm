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
 * A builder for constructing a JSON Patch as defined by
 * <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a> by adding
 * JSON Patch operations incrementally.
 * <p>
 * The following illustrates the approach.
 * <pre>
 *   JsonPatchBuilder builder = Json.createPatchBuilder();
 *   JsonPatch patch = builder.add("/John/phones/office", "1234-567")
 *                            .remove("/Amy/age")
 *                            .build();
 * </pre>
 * The result is equivalent to the following JSON Patch.
 * <pre>
 * [
 *    {"op" = "add", "path" = "/John/phones/office", "value" = "1234-567"},
 *    {"op" = "remove", "path" = "/Amy/age"}
 * ] </pre>
 *
 * @see <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>
 *
 * @since 1.1
 */
public interface JsonPatchBuilder {

    /**
     * Adds an "add" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder add(String path, JsonValue value);

    /**
     * Adds an "add" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder add(String path, String value);

    /**
     * Adds an "add" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder add(String path, int value);

    /**
     * Adds an "add" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder add(String path, boolean value);

    /**
     * Adds a "remove" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder remove(String path);

    /**
     * Adds a "replace" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder replace(String path, JsonValue value);

    /**
     * Adds a "replace" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder replace(String path, String value);

    /**
     * Adds a "replace" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder replace(String path, int value);

    /**
     * Adds a "replace" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder replace(String path, boolean value);

    /**
     * Adds a "move" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param from the "from" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder move(String path, String from);

    /**
     * Adds a "copy" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param from the "from" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder copy(String path, String from);

    /**
     * Adds a "test" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder test(String path, JsonValue value);

    /**
     * Adds a "test" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder test(String path, String value);

    /**
     * Adds a "test" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder test(String path, int value);

    /**
     * Adds a "test" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder test(String path, boolean value);


    /**
     * Returns the JSON Patch.
     *
     * @return a JSON Patch
     */
    JsonPatch build();

}
