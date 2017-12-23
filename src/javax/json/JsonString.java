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

/**
 * An immutable JSON string value.
 */
public interface JsonString extends JsonValue {

    /**
     * Returns the JSON string value.
     *
     * @return a JSON string value
     */
    String getString();


    /**
     * Returns the char sequence for the JSON String value
     *
     * @return a char sequence for the JSON String value
     */
    CharSequence getChars();

    /**
     * Compares the specified object with this {@code JsonString} for equality.
     * Returns {@code true} if and only if the specified object is also a
     * {@code JsonString}, and their {@link #getString()} objects are
     * <i>equal</i>.
     *
     * @param obj the object to be compared for equality with this 
     *      {@code JsonString}
     * @return {@code true} if the specified object is equal to this 
     *      {@code JsonString}
     */
    @Override
    boolean equals(Object obj);

    /**
     * Returns the hash code value for this {@code JsonString} object.  
     * The hash code of a {@code JsonString} object is defined to be its 
     * {@link #getString()} object's hash code.
     *
     * @return the hash code value for this {@code JsonString} object
     */
    @Override
    int hashCode();

}
