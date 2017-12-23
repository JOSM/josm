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

import javax.json.JsonString;

/**
 * JsonString impl
 *
 * @author Jitendra Kotamraju
 */
final class JsonStringImpl implements JsonString {

    private final String value;

    JsonStringImpl(String value) {
        this.value = value;
    }

    @Override
    public String getString() {
        return value;
    }

    @Override
    public CharSequence getChars() {
        return value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.STRING;
    }

    @Override
    public int hashCode() {
        return getString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }
        if (!(obj instanceof JsonString)) {
            return false;
        }
        JsonString other = (JsonString)obj;
        return getString().equals(other.getString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('"');

        for(int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // unescaped = %x20-21 | %x23-5B | %x5D-10FFFF
            if (c >= 0x20 && c <= 0x10ffff && c != 0x22 && c != 0x5c) {
                sb.append(c);
            } else {
                switch (c) {
                    case '"':
                    case '\\':
                        sb.append('\\'); sb.append(c);
                        break;
                    case '\b':
                        sb.append('\\'); sb.append('b');
                        break;
                    case '\f':
                        sb.append('\\'); sb.append('f');
                        break;
                    case '\n':
                        sb.append('\\'); sb.append('n');
                        break;
                    case '\r':
                        sb.append('\\'); sb.append('r');
                        break;
                    case '\t':
                        sb.append('\\'); sb.append('t');
                        break;
                    default:
                        String hex = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(hex.substring(hex.length() - 4));
                }
            }
        }

        sb.append('"');
        return sb.toString();
    }
}

