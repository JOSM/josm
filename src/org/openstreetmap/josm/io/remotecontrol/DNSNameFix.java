/*
 * Copyright (c) 1997, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openstreetmap.josm.io.remotecontrol;

import java.io.IOException;
import java.lang.reflect.Field;

import org.openstreetmap.josm.tools.Utils;

/**
 * This class implements the DNSName as required by the GeneralNames
 * ASN.1 object.
 * <p>
 * [RFC2459] When the subjectAltName extension contains a domain name service
 * label, the domain name MUST be stored in the dNSName (an IA5String).
 * The name MUST be in the "preferred name syntax," as specified by RFC
 * 1034 [RFC 1034]. Note that while upper and lower case letters are
 * allowed in domain names, no signifigance is attached to the case.  In
 * addition, while the string " " is a legal domain name, subjectAltName
 * extensions with a dNSName " " are not permitted.  Finally, the use of
 * the DNS representation for Internet mail addresses (wpolk.nist.gov
 * instead of wpolk@nist.gov) is not permitted; such identities are to
 * be encoded as rfc822Name.
 *
 * This class has been copied from OpenJDK8u repository and modified
 * in order to fix Java bug 8016345:
 * https://bugs.openjdk.java.net/browse/JDK-8016345
 *
 * It can be deleted after a migration to a Java release fixing this bug:
 * https://bugs.openjdk.java.net/browse/JDK-8054380
 * <p>
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @author JOSM developers
 * @since 7347
 */
public final class DNSNameFix extends sun.security.x509.DNSName {

    private static final String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String digitsAndHyphen = "0123456789-";
    private static final String alphaDigitsAndHyphen = alpha + digitsAndHyphen;

    /**
     * Create the DNSName object with the specified name.
     *
     * @param name the DNSName.
     * @throws IOException if the name is not a valid DNSName subjectAltName
     */
    public DNSNameFix(String name) throws IOException {
        super("fake");
        if (name == null || name.isEmpty())
            throw new IOException("DNS name must not be null");
        if (name.indexOf(' ') != -1)
            throw new IOException("DNS names or NameConstraints with blank components are not permitted");
        if (name.charAt(0) == '.' || name.charAt(name.length() -1) == '.')
            throw new IOException("DNS names or NameConstraints may not begin or end with a .");
        //Name will consist of label components separated by "."
        //startIndex is the index of the first character of a component
        //endIndex is the index of the last character of a component plus 1
        for (int endIndex, startIndex = 0; startIndex < name.length(); startIndex = endIndex+1) {
            endIndex = name.indexOf('.', startIndex);
            if (endIndex < 0) {
                endIndex = name.length();
            }
            if ((endIndex-startIndex) < 1)
                throw new IOException("DNSName SubjectAltNames with empty components are not permitted");

            //nonStartIndex: index for characters in the component beyond the first one
            for (int nonStartIndex = startIndex+1; nonStartIndex < endIndex; nonStartIndex++) {
                char x = name.charAt(nonStartIndex);
                if ((alphaDigitsAndHyphen).indexOf(x) < 0)
                    throw new IOException("DNSName components must consist of letters, digits, and hyphens");
            }
        }
        try {
            Field fName = getClass().getSuperclass().getDeclaredField("name");
            Utils.setObjectsAccessible(fName);
            fName.set(this, name);
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IOException(e);
        }
    }
}
