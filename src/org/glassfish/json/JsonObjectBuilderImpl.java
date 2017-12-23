/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.json.api.BufferPool;

import javax.json.JsonArrayBuilder;
import javax.json.*;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * JsonObjectBuilder implementation
 *
 * @author Jitendra Kotamraju
 * @author Kin-man Chung
 */
class JsonObjectBuilderImpl implements JsonObjectBuilder {

    private Map<String, JsonValue> valueMap;
    private final BufferPool bufferPool;

    JsonObjectBuilderImpl(BufferPool bufferPool) {
        this.bufferPool = bufferPool;
    }

    JsonObjectBuilderImpl(JsonObject object, BufferPool bufferPool) {
        this.bufferPool = bufferPool;
        valueMap = new LinkedHashMap<>();
        valueMap.putAll(object);
    }

    JsonObjectBuilderImpl(Map<String, Object> map, BufferPool bufferPool) {
        this.bufferPool = bufferPool;
        valueMap = new LinkedHashMap<>();
        populate(map);
    }

    @Override
    public JsonObjectBuilder add(String name, JsonValue value) {
        validateName(name);
        validateValue(value);
        putValueMap(name, value);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, String value) {
        validateName(name);
        validateValue(value);
        putValueMap(name, new JsonStringImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, BigInteger value) {
        validateName(name);
        validateValue(value);
        putValueMap(name, JsonNumberImpl.getJsonNumber(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, BigDecimal value) {
        validateName(name);
        validateValue(value);
        putValueMap(name, JsonNumberImpl.getJsonNumber(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, int value) {
        validateName(name);
        putValueMap(name, JsonNumberImpl.getJsonNumber(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, long value) {
        validateName(name);
        putValueMap(name, JsonNumberImpl.getJsonNumber(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, double value) {
        validateName(name);
        putValueMap(name, JsonNumberImpl.getJsonNumber(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, boolean value) {
        validateName(name);
        putValueMap(name, value ? JsonValue.TRUE : JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonObjectBuilder addNull(String name) {
        validateName(name);
        putValueMap(name, JsonValue.NULL);
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, JsonObjectBuilder builder) {
        validateName(name);
        if (builder == null) {
            throw new NullPointerException(JsonMessages.OBJBUILDER_OBJECT_BUILDER_NULL());
        }
        putValueMap(name, builder.build());
        return this;
    }

    @Override
    public JsonObjectBuilder add(String name, JsonArrayBuilder builder) {
        validateName(name);
        if (builder == null) {
            throw new NullPointerException(JsonMessages.OBJBUILDER_ARRAY_BUILDER_NULL());
        }
        putValueMap(name, builder.build());
        return this;
    }

    @Override
    public JsonObjectBuilder addAll(JsonObjectBuilder builder) {
        if (builder == null) {
            throw new NullPointerException(JsonMessages.OBJBUILDER_OBJECT_BUILDER_NULL());
        }
        if (valueMap == null) {
            this.valueMap = new LinkedHashMap<>();
        }
        this.valueMap.putAll(builder.build());
        return this;
    }

    @Override
    public JsonObjectBuilder remove(String name) {
        validateName(name);
        this.valueMap.remove(name);
        return this;
    }

    @Override
    public JsonObject build() {
        Map<String, JsonValue> snapshot = (valueMap == null)
                ? Collections.<String, JsonValue>emptyMap()
                : Collections.unmodifiableMap(valueMap);
        valueMap = null;
        return new JsonObjectImpl(snapshot, bufferPool);
    }

    private void populate(Map<String, Object> map) {
        final Set<String> fields = map.keySet();
        for (String field : fields) {
            Object value = map.get(field);
            if (value != null && value instanceof Optional) {
                ((Optional<?>) value).ifPresent(v ->
                        this.valueMap.put(field, MapUtil.handle(v, bufferPool)));
            } else {
                this.valueMap.put(field, MapUtil.handle(value, bufferPool));
            }
        }
    }

    private void putValueMap(String name, JsonValue value) {
        if (valueMap == null) {
            this.valueMap = new LinkedHashMap<>();
        }
        valueMap.put(name, value);
    }

    private void validateName(String name) {
        if (name == null) {
            throw new NullPointerException(JsonMessages.OBJBUILDER_NAME_NULL());
        }
    }

    private void validateValue(Object value) {
        if (value == null) {
            throw new NullPointerException(JsonMessages.OBJBUILDER_VALUE_NULL());
        }
    }

    private static final class JsonObjectImpl extends AbstractMap<String, JsonValue> implements JsonObject {
        private final Map<String, JsonValue> valueMap;      // unmodifiable
        private final BufferPool bufferPool;

        JsonObjectImpl(Map<String, JsonValue> valueMap, BufferPool bufferPool) {
            this.valueMap = valueMap;
            this.bufferPool = bufferPool;
        }

        @Override
        public JsonArray getJsonArray(String name) {
            return (JsonArray)get(name);
        }

        @Override
        public JsonObject getJsonObject(String name) {
            return (JsonObject)get(name);
        }

        @Override
        public JsonNumber getJsonNumber(String name) {
            return (JsonNumber)get(name);
        }

        @Override
        public JsonString getJsonString(String name) {
            return (JsonString)get(name);
        }

        @Override
        public String getString(String name) {
            return getJsonString(name).getString();
        }

        @Override
        public String getString(String name, String defaultValue) {
            try {
                return getString(name);
            } catch (Exception e) {
                return defaultValue;
            }
        }

        @Override
        public int getInt(String name) {
            return getJsonNumber(name).intValue();
        }

        @Override
        public int getInt(String name, int defaultValue) {
            try {
                return getInt(name);
            } catch (Exception e) {
                return defaultValue;
            }
        }

        @Override
        public boolean getBoolean(String name) {
            JsonValue value = get(name);
            if (value == null) {
                throw new NullPointerException();
            } else if (value == JsonValue.TRUE) {
                return true;
            } else if (value == JsonValue.FALSE) {
                return false;
            } else {
                throw new ClassCastException();
            }
        }

        @Override
        public boolean getBoolean(String name, boolean defaultValue) {
            try {
                return getBoolean(name);
            } catch (Exception e) {
                return defaultValue;
            }
        }

        @Override
        public boolean isNull(String name) {
            return get(name).equals(JsonValue.NULL);
        }

        @Override
        public ValueType getValueType() {
            return ValueType.OBJECT;
        }

        @Override
        public Set<Entry<String, JsonValue>> entrySet() {
            return valueMap.entrySet();
        }

        @Override
        public String toString() {
            StringWriter sw = new StringWriter();
            try (JsonWriter jw = new JsonWriterImpl(sw, bufferPool)) {
                jw.write(this);
            }
            return sw.toString();
        }

        @Override
        public JsonObject asJsonObject() {
            return this;
        }

        @Override
        public int size() {
            return valueMap.size();
        }

        @Override
        public JsonValue get(Object key) {
            return valueMap.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return valueMap.containsKey(key);
        }
    }

}
