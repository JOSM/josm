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
 * <p>This interface represents an implementation of a JSON Merge Patch
 * as defined by <a href="http://tools.ietf.org/html/rfc7396">RFC 7396</a>.
 * </p>
 * <p>A {@code JsonMergePatch} can be instantiated with {@link Json#createMergePatch(JsonValue)}
 * by specifying the patch operations in a JSON Merge Patch or using {@link Json#createMergeDiff(JsonValue, JsonValue)}
 * to create a JSON Merge Patch based on the difference between two {@code JsonValue}s.
 * </p>
 * The following illustrates both approaches.
 * <p>1. Construct a JsonMergePatch with an existing JSON Merge Patch.
 * <pre>{@code
 *   JsonValue contacts = ... ; // The target to be patched
 *   JsonValue patch = ...  ; // JSON Merge Patch
 *   JsonMergePatch mergePatch = Json.createMergePatch(patch);
 *   JsonValue result = mergePatch.apply(contacts);
 * } </pre>
 * 2. Construct a JsonMergePatch from a difference between two {@code JsonValue}s.
 * <pre>{@code
 *   JsonValue source = ... ; // The source object
 *   JsonValue target = ... ; // The modified object
 *   JsonMergePatch mergePatch = Json.createMergeDiff(source, target); // The diff between source and target in a Json Merge Patch format
 * } </pre>
 *
 * @see <a href="http://tools.ietf.org/html/rfc7396">RFC 7396</a>
 *
 * @since 1.1
 */
public interface JsonMergePatch {

    /**
     * Applies the JSON Merge Patch to the specified {@code target}.
     * The target is not modified by the patch.
     *
     * @param target the target to apply the merge patch
     * @return the transformed target after the patch
     */
    JsonValue apply(JsonValue target);

    /**
     * Returns the {@code JsonMergePatch} as {@code JsonValue}.
     *
     * @return this {@code JsonMergePatch} as {@code JsonValue}
     */
    JsonValue toJsonValue();
}
