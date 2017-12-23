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
import javax.json.JsonMergePatch;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * This class is an implementation of a JSON Merge Patch as specified in
 * <a href="http://tools.ietf.org/html/rfc7396">RFC 7396</a>.
 *
 * @since 1.1
 */

public final class JsonMergePatchImpl implements JsonMergePatch {

    private JsonValue patch;

    public JsonMergePatchImpl(JsonValue patch) {
        this.patch = patch;
    }

    @Override
    public JsonValue apply(JsonValue target) {
        return mergePatch(target, patch);
    }

    @Override
    public JsonValue toJsonValue() {
        return patch;
    }
    /**
     * Applies the specified patch to the specified target.
     * The target is not modified by the patch.
     *
     * @param target the {@code JsonValue} to apply the patch operations
     * @param patch the patch
     * @return the {@code JsonValue} as the result of applying the patch
     *    operations on the target.
     */
    private static JsonValue mergePatch(JsonValue target, JsonValue patch) {

        if (patch.getValueType() != JsonValue.ValueType.OBJECT) {
            return patch;
        }
        if (target.getValueType() != JsonValue.ValueType.OBJECT) {
            target = JsonValue.EMPTY_JSON_OBJECT;
        }
        JsonObject targetJsonObject = target.asJsonObject();
        JsonObjectBuilder builder =
            Json.createObjectBuilder(targetJsonObject);
        patch.asJsonObject().forEach((key, value) -> {
            if (value == JsonValue.NULL) {
                if (targetJsonObject.containsKey(key)) {
                    builder.remove(key);
                }
            } else if (targetJsonObject.containsKey(key)) {
                builder.add(key, mergePatch(targetJsonObject.get(key), value));
            } else {
                builder.add(key, mergePatch(JsonValue.EMPTY_JSON_OBJECT, value));
            }
        });
        return builder.build();
    }

    /**
     * Generate a JSON Merge Patch from the source and target {@code JsonValue}.
     * @param source the source
     * @param target the target
     * @return a JSON Patch which when applied to the source, yields the target
     */
    static JsonValue diff(JsonValue source, JsonValue target) {
        if (source.getValueType() != JsonValue.ValueType.OBJECT ||
                target.getValueType() != JsonValue.ValueType.OBJECT) {
            return target;
        }
        JsonObject s = (JsonObject) source;
        JsonObject t = (JsonObject) target;
        JsonObjectBuilder builder = Json.createObjectBuilder();
        // First find members to be replaced or removed
        s.forEach((key, value) -> {
            if (t.containsKey(key)) {
                // key present in both.
                if (! value.equals(t.get(key))) {
                    // If the values are equal, nop, else get diff for the values
                    builder.add(key, diff(value, t.get(key)));
                }
            } else {
                builder.addNull(key);
            }
        });
        // Then find members to be added
        t.forEach((key, value) -> {
            if (! s.containsKey(key))
                builder.add(key, value);
        });
        return builder.build();
    }

}

