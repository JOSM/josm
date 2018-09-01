/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Private implementation of {@link JsonValue} for simple {@link ValueType}s
 * allowing their usage in constants which are better to implement {@link Serializable}.
 *
 * @author Lukas Jungmann
 */
final class JsonValueImpl implements JsonValue, Serializable {

    private final ValueType valueType;

    JsonValueImpl(ValueType valueType) {
        this.valueType = valueType;
    }

    /**
     * Returns the value type of this JSON value.
     *
     * @return JSON value type
     */
    @Override
    public ValueType getValueType() {
        return valueType;
    }

    /**
     * Compares the specified object with this {@link JsonValue}
     * object for equality. Returns {@code true} if and only if the
     * specified object is also a JsonValue, and their
     * {@link #getValueType()} objects are <i>equal</i>.
     *
     * @param obj the object to be compared for equality with this JsonValue
     * @return {@code true} if the specified object is equal to this
     * JsonValue
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof JsonValue) {
            return getValueType() == ((JsonValue) obj).getValueType();
        }
        return false;
    }

    /**
     * Returns the hash code value for this {@link JsonValue} object.
     * The hash code of the {@link JsonValue} object is defined to be
     * its {@link #getValueType()} object's hash code.
     *
     * @return the hash code value for this {@link JsonValue} object
     */
    @Override
    public int hashCode() {
        return valueType.hashCode();
    }

    @Override
    public String toString() {
        return valueType.name().toLowerCase();
    }

}
