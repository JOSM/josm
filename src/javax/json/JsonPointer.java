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
 * <p>This interface represents an immutable implementation of a JSON Pointer
 * as defined by <a href="http://tools.ietf.org/html/rfc6901">RFC 6901</a>.
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
 * Methods {@link #add add()}, {@link #replace replace()},
 * and {@link #remove remove()} execute operations specified in
 * <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>. </p>
 *
 * @see <a href="http://tools.ietf.org/html/rfc6901">RFC 6901</a>
 * @see <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>
 *
 * @since 1.1
 */
public interface JsonPointer {

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
     * @param <T> the target type, must be a subtype of {@link JsonValue}
     * @param target the target referenced by this {@code JsonPointer}
     * @param value the value to be added
     * @return the transformed {@code target} after the value is added.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException if the reference is an array element and
     * the index is out of range ({@code index < 0 || index > array size}),
     * or if the pointer contains references to non-existing objects or arrays.
     */
    <T extends JsonStructure> T add(T target, JsonValue value);

    /**
     * Removes the value at the reference location in the specified {@code target}.
     *
     * @param <T> the target type, must be a subtype of {@link JsonValue}
     * @param target the target referenced by this {@code JsonPointer}
     * @return the transformed {@code target} after the value is removed.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException if the referenced value does not exist,
     *    or if the reference is the target.
     */
    <T extends JsonStructure> T remove(T target);

    /**
     * Replaces the value at the referenced location in the specified
     * {@code target} with the specified {@code value}.
     *
     * @param <T> the target type, must be a subtype of {@link JsonValue}
     * @param target the target referenced by this {@code JsonPointer}
     * @param value the value to be stored at the referenced location
     * @return the transformed {@code target} after the value is replaced.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException if the referenced value does not exist,
     *    or if the reference is the target.
     */
    <T extends JsonStructure> T replace(T target, JsonValue value);

    /**
     * Returns {@code true} if there is a value at the referenced location in the specified {@code target}.
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @return {@code true} if this pointer points to a value in a specified {@code target}.
     */
    boolean containsValue(JsonStructure target);

    /**
     * Returns the value at the referenced location in the specified {@code target}.
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @return the referenced value in the target.
     * @throws NullPointerException if {@code target} is null
     * @throws JsonException if the referenced value does not exist
     */
    JsonValue getValue(JsonStructure target);
}
