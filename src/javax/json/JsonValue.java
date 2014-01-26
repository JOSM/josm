/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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
 * <code>JsonValue</code> represents an immutable JSON value.
 *
 * 
 * <p>A JSON value is one of the following:
 * an object ({@link JsonObject}), an array ({@link JsonArray}),
 * a number ({@link JsonNumber}), a string ({@link JsonString}),
 * {@code true} ({@link JsonValue#TRUE JsonValue.TRUE}), {@code false} 
 * ({@link JsonValue#FALSE JsonValue.FALSE}),
 * or {@code null} ({@link JsonValue#NULL JsonValue.NULL}).
 *
 * @author Jitendra Kotamraju
 */
public interface JsonValue {

    /**
     * Indicates the type of a {@link JsonValue} object.
     */
    enum ValueType {
        /**
         * JSON array.
         */
        ARRAY,

        /**
         * JSON object.
         */
        OBJECT,

        /**
         * JSON string.
         */
        STRING,

        /**
         * JSON number.
         */
        NUMBER,

        /**
         * JSON true.
         */
        TRUE,

        /**
         * JSON false.
         */
        FALSE,

        /**
         * JSON null.
         */
        NULL
    }

    /**
     * JSON null value.
     */
    static final JsonValue NULL = new JsonValue() {
        @Override
        public ValueType getValueType() {
            return ValueType.NULL;
        }

        /**
         * Compares the specified object with this {@link JsonValue#NULL}
         * object for equality. Returns {@code true} if and only if the
         * specified object is also a {@code JsonValue}, and their
         * {@link #getValueType()} objects are <i>equal</i>.
         *
         * @param obj the object to be compared for equality with this 
         *      {@code JsonValue}
         * @return {@code true} if the specified object is equal to this 
         *      {@code JsonValue}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof JsonValue) {
                return getValueType().equals(((JsonValue)obj).getValueType());
            }
            return false;
        }

        /**
         * Returns the hash code value for this {@link JsonValue#NULL} object.
         * The hash code of the {@link JsonValue#NULL} object is defined to be
         * its {@link #getValueType()} object's hash code.
         *
         * @return the hash code value for this JsonString object
         */
        @Override
        public int hashCode() {
            return ValueType.NULL.hashCode();
        }

        /**
         * Returns a "null" string.
         *
         * @return "null"
         */
        @Override
        public String toString() {
            return "null";
        }
    };

    /**
     * JSON true value.
     */
    static final JsonValue TRUE = new JsonValue() {
        @Override
        public ValueType getValueType() {
            return ValueType.TRUE;
        }

        /**
         * Compares the specified object with this {@link JsonValue#TRUE}
         * object for equality. Returns {@code true} if and only if the
         * specified object is also a JsonValue, and their
         * {@link #getValueType()} objects are <i>equal</i>.
         *
         * @param obj the object to be compared for equality with this JsonValue.
         * @return {@code true} if the specified object is equal to this JsonValue.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof JsonValue) {
                return getValueType().equals(((JsonValue)obj).getValueType());
            }
            return false;
        }

        /**
         * Returns the hash code value for this {@link JsonValue#TRUE} object.
         * The hash code of the {@link JsonValue#TRUE} object is defined to be
         * its {@link #getValueType()} object's hash code.
         *
         * @return the hash code value for this JsonString object
         */
        @Override
        public int hashCode() {
            return ValueType.TRUE.hashCode();
        }

        /**
         * Returns "true" string
         *
         * @return "true"
         */
        @Override
        public String toString() {
            return "true";
        }
    };

    /**
     * JSON false value
     */
    static final JsonValue FALSE = new JsonValue() {
        @Override
        public ValueType getValueType() {
            return ValueType.FALSE;
        }

        /**
         * Compares the specified object with this {@link JsonValue#FALSE}
         * object for equality. Returns {@code true} if and only if the
         * specified object is also a JsonValue, and their
         * {@link #getValueType()} objects are <i>equal</i>.
         *
         * @param obj the object to be compared for equality with this JsonValue
         * @return {@code true} if the specified object is equal to this JsonValue
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof JsonValue) {
                return getValueType().equals(((JsonValue)obj).getValueType());
            }
            return false;
        }

        /**
         * Returns the hash code value for this {@link JsonValue#FALSE} object.
         * The hash code of the {@link JsonValue#FALSE} object is defined to be
         * its {@link #getValueType()} object's hash code.
         *
         * @return the hash code value for this JsonString object
         */
        @Override
        public int hashCode() {
            return ValueType.FALSE.hashCode();
        }

        /**
         * Returns "false" string
         *
         * @return "false"
         */
        @Override
        public String toString() {
            return "false";
        }
    };

    /**
     * Returns the value type of this JSON value.
     *
     * @return JSON value type
     */
    ValueType getValueType();

    /**
     * Returns JSON text for this JSON value.
     *
     * @return JSON text
     */
    @Override
    String toString();

}
