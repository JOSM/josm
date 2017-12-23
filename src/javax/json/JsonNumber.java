/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An immutable JSON number value.
 *
 * <p>
 * Implementations may use a {@link BigDecimal} object to store the numeric
 * value internally.
 * The {@code BigDecimal} object can be constructed from the following types:
 * <code>int</code> {@link BigDecimal#BigDecimal(int)},
 * <code>long</code> {@link BigDecimal#BigDecimal(long)},
 * <code>BigInteger</code> {@link BigDecimal#BigDecimal(BigInteger)},
 * <code>double</code> {@link BigDecimal#valueOf(double)}, and
 * <code>String</code> {@link BigDecimal#BigDecimal(String)}.
 * Some of the method semantics in this class are defined using the
 * {@code BigDecimal} semantics.
 */
public interface JsonNumber extends JsonValue {

    /**
     * Returns true if this JSON number is a integral number. This method
     * semantics are defined using {@code bigDecimalValue().scale()}. If the
     * scale is zero, then it is considered integral type. This integral type
     * information can be used to invoke an appropriate accessor method to
     * obtain a numeric value as in the following example:
     *
     * <pre>
     * <code>
     * JsonNumber num = ...
     * if (num.isIntegral()) {
     *     num.longValue();     // or other methods to get integral value
     * } else {
     *     num.doubleValue();   // or other methods to get decimal number value
     * }
     * </code>
     * </pre>
     *
     * @return true if this number is a integral number, otherwise false
     */
    boolean isIntegral();

    /**
     * Returns this JSON number as an {@code int}. Note that this conversion
     * can lose information about the overall magnitude and precision of the
     * number value as well as return a result with the opposite sign.
     *
     * @return an {@code int} representation of the JSON number
     * @see java.math.BigDecimal#intValue()
     */
    int intValue();

    /**
     * Returns this JSON number as an {@code int}.
     *
     * @return an {@code int} representation of the JSON number
     * @throws ArithmeticException if the number has a nonzero fractional
     *         part or if it does not fit in an {@code int}
     * @see java.math.BigDecimal#intValueExact()
     */
    int intValueExact();

    /**
     * Returns this JSON number as a {@code long}. Note that this conversion
     * can lose information about the overall magnitude and precision of the
     * number value as well as return a result with the opposite sign.
     *
     * @return a {@code long} representation of the JSON number.
     * @see java.math.BigDecimal#longValue()
     */
    long longValue();

    /**
     * Returns this JSON number as a {@code long}.
     *
     * @return a {@code long} representation of the JSON number
     * @throws ArithmeticException if the number has a non-zero fractional
     *         part or if it does not fit in a {@code long}
     * @see java.math.BigDecimal#longValueExact()
     */
    long longValueExact();

    /**
     * Returns this JSON number as a {@link BigInteger} object. This is a
     * a convenience method for {@code bigDecimalValue().toBigInteger()}.
     * Note that this conversion can lose information about the overall
     * magnitude and precision of the number value as well as return a result
     * with the opposite sign.
     *
     * @return a {@code BigInteger} representation of the JSON number.
     * @see java.math.BigDecimal#toBigInteger()
     */
    BigInteger bigIntegerValue();

    /**
     * Returns this JSON number as a {@link BigInteger} object. This is a
     * convenience method for {@code bigDecimalValue().toBigIntegerExact()}.
     *
     * @return a {@link BigInteger} representation of the JSON number
     * @throws ArithmeticException if the number has a nonzero fractional part
     * @see java.math.BigDecimal#toBigIntegerExact()
     */
    BigInteger bigIntegerValueExact();

    /**
     * Returns this JSON number as a {@code double}. This is a
     * a convenience method for {@code bigDecimalValue().doubleValue()}.
     * Note that this conversion can lose information about the overall
     * magnitude and precision of the number value as well as return a result
     * with the opposite sign.
     *
     * @return a {@code double} representation of the JSON number
     * @see java.math.BigDecimal#doubleValue()
     */
    double doubleValue();

    /**
     * Returns this JSON number as a {@link BigDecimal} object.
     *
     * @return a {@link BigDecimal} representation of the JSON number
     */
    BigDecimal bigDecimalValue();

    /**
     * Returns this JSON number as a {@link Number} object.
     *
     * @return a {@link Number} representation of the JSON number
     *
     * @since 1.1
     */
    default Number numberValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a JSON text representation of the JSON number. The
     * representation is equivalent to {@link BigDecimal#toString()}.
     *
     * @return JSON text representation of the number
     */
    @Override
    String toString();

    /**
     * Compares the specified object with this {@code JsonNumber} object for
     * equality. Returns {@code true} if and only if the type of the specified
     * object is also {@code JsonNumber} and their {@link #bigDecimalValue()}
     * objects are <i>equal</i>
     *
     * @param obj the object to be compared for equality with
     *      this {@code JsonNumber}
     * @return {@code true} if the specified object is equal to this
     *      {@code JsonNumber}
     */
    @Override
    boolean equals(Object obj);

    /**
     * Returns the hash code value for this {@code JsonNumber} object.  The
     * hash code of a {@code JsonNumber} object is defined as the hash code of
     * its {@link #bigDecimalValue()} object.
     *
     * @return the hash code value for this {@code JsonNumber} object
     */
    @Override
    int hashCode();

}
