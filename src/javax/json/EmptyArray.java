/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * Private implementation of immutable {@link JsonArray}.
 *
 * @author Lukas Jungmann
 */
final class EmptyArray extends AbstractList<JsonValue> implements JsonArray, Serializable, RandomAccess {

    private static final long serialVersionUID = 7295439472061642859L;

    @Override
    public JsonValue get(int index) {
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public JsonObject getJsonObject(int index) {
        return (JsonObject) get(index);
    }

    @Override
    public JsonArray getJsonArray(int index) {
        return (JsonArray) get(index);
    }

    @Override
    public JsonNumber getJsonNumber(int index) {
        return (JsonNumber) get(index);
    }

    @Override
    public JsonString getJsonString(int index) {
        return (JsonString) get(index);
    }

    @Override
    public <T extends JsonValue> List<T> getValuesAs(Class<T> clazz) {
        return Collections.emptyList();
    }

    @Override
    public String getString(int index) {
        return getJsonString(index).getString();
    }

    @Override
    public String getString(int index, String defaultValue) {
        return defaultValue;
    }

    @Override
    public int getInt(int index) {
        return getJsonNumber(index).intValue();
    }

    @Override
    public int getInt(int index, int defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean getBoolean(int index) {
        return get(index) == JsonValue.TRUE;
    }

    @Override
    public boolean getBoolean(int index, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean isNull(int index) {
        return get(index) == JsonValue.NULL;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.ARRAY;
    }

    // Preserves singleton property
    private Object readResolve() {
        return JsonValue.EMPTY_JSON_ARRAY;
    }
}
