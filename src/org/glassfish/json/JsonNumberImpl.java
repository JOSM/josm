/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.json.JsonNumber;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * JsonNumber impl. Subclasses provide optimized implementations
 * when backed by int, long, BigDecimal
 *
 * @author Jitendra Kotamraju
 */
abstract class JsonNumberImpl implements JsonNumber {

    static JsonNumber getJsonNumber(int num) {
        return new JsonIntNumber(num);
    }

    static JsonNumber getJsonNumber(long num) {
        return new JsonLongNumber(num);
    }

    static JsonNumber getJsonNumber(BigInteger value) {
        return new JsonBigDecimalNumber(new BigDecimal(value));
    }

    static JsonNumber getJsonNumber(double value) {
        //bigDecimal = new BigDecimal(value);
        // This is the preferred way to convert double to BigDecimal
        return new JsonBigDecimalNumber(BigDecimal.valueOf(value));
    }

    static JsonNumber getJsonNumber(BigDecimal value) {
        return new JsonBigDecimalNumber(value);
    }

    // Optimized JsonNumber impl for int numbers.
    private static final class JsonIntNumber extends JsonNumberImpl {
        private final int num;
        private BigDecimal bigDecimal;  // assigning it lazily on demand

        JsonIntNumber(int num) {
            this.num = num;
        }

        @Override
        public boolean isIntegral() {
            return true;
        }

        @Override
        public int intValue() {
            return num;
        }

        @Override
        public int intValueExact() {
            return num;
        }

        @Override
        public long longValue() {
            return num;
        }

        @Override
        public long longValueExact() {
            return num;
        }

        @Override
        public double doubleValue() {
            return num;
        }

        @Override
        public BigDecimal bigDecimalValue() {
            // reference assignments are atomic. At the most some more temp
            // BigDecimal objects are created
            BigDecimal bd = bigDecimal;
            if (bd == null) {
                bigDecimal = bd = new BigDecimal(num);
            }
            return bd;
        }

        @Override
        public Number numberValue() {
            return num;
        }

        @Override
        public String toString() {
            return Integer.toString(num);
        }
    }

    // Optimized JsonNumber impl for long numbers.
    private static final class JsonLongNumber extends JsonNumberImpl {
        private final long num;
        private BigDecimal bigDecimal;  // assigning it lazily on demand

        JsonLongNumber(long num) {
            this.num = num;
        }

        @Override
        public boolean isIntegral() {
            return true;
        }

        @Override
        public int intValue() {
            return (int) num;
        }

        @Override
        public int intValueExact() {
            return Math.toIntExact(num);
        }

        @Override
        public long longValue() {
            return num;
        }

        @Override
        public long longValueExact() {
            return num;
        }

        @Override
        public double doubleValue() {
            return num;
        }

        @Override
        public BigDecimal bigDecimalValue() {
            // reference assignments are atomic. At the most some more temp
            // BigDecimal objects are created
            BigDecimal bd = bigDecimal;
            if (bd == null) {
                bigDecimal = bd = new BigDecimal(num);
            }
            return bd;
        }

        @Override
        public Number numberValue() {
            return num;
        }

        @Override
        public String toString() {
            return Long.toString(num);
        }

    }

    // JsonNumber impl using BigDecimal numbers.
    private static final class JsonBigDecimalNumber extends JsonNumberImpl {
        private final BigDecimal bigDecimal;

        JsonBigDecimalNumber(BigDecimal value) {
            this.bigDecimal = value;
        }

        @Override
        public BigDecimal bigDecimalValue() {
            return bigDecimal;
        }

        @Override
        public Number numberValue() {
            return bigDecimalValue();
        }

    }

    @Override
    public boolean isIntegral() {
        return bigDecimalValue().scale() == 0;
    }

    @Override
    public int intValue() {
        return bigDecimalValue().intValue();
    }

    @Override
    public int intValueExact() {
        return bigDecimalValue().intValueExact();
    }

    @Override
    public long longValue() {
        return bigDecimalValue().longValue();
    }

    @Override
    public long longValueExact() {
        return bigDecimalValue().longValueExact();
    }

    @Override
    public double doubleValue() {
        return bigDecimalValue().doubleValue();
    }

    @Override
    public BigInteger bigIntegerValue() {
        return bigDecimalValue().toBigInteger();
    }

    @Override
    public BigInteger bigIntegerValueExact() {
        return bigDecimalValue().toBigIntegerExact();
    }

    @Override
    public ValueType getValueType() {
        return ValueType.NUMBER;
    }

    @Override
    public int hashCode() {
        return bigDecimalValue().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }
        if (!(obj instanceof JsonNumber)) {
            return false;
        }
        JsonNumber other = (JsonNumber)obj;
        return bigDecimalValue().equals(other.bigDecimalValue());
    }

    @Override
    public String toString() {
        return bigDecimalValue().toString();
    }

}

