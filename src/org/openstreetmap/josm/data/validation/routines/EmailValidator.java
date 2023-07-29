/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openstreetmap.josm.data.validation.routines;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Perform email validations.</p>
 * <p>
 * Based on a script by <a href="mailto:stamhankar@hotmail.com">Sandeep V. Tamhankar</a>
 * http://javascript.internet.com
 * </p>
 * <p>
 * This implementation is not guaranteed to catch all possible errors in an email address.
 * </p>.
 *
 * @version $Revision: 1741724 $
 * @since Validator 1.4
 */
public class EmailValidator extends AbstractValidator {

    private static final String SPECIAL_CHARS = "\\p{Cntrl}\\(\\)<>@,;:'\\\\\\\"\\.\\[\\]";
    private static final String VALID_CHARS = "(\\\\.)|[^\\s" + SPECIAL_CHARS + "]";
    private static final String QUOTED_USER = "(\"(\\\\\"|[^\"])*\")";
    private static final String WORD = "((" + VALID_CHARS + "|')+|" + QUOTED_USER + ")";

    private static final String EMAIL_REGEX = "^\\s*?(.+)@(.+?)\\s*$";
    private static final String IP_DOMAIN_REGEX = "^\\[(.*)\\]$";
    private static final String USER_REGEX = "^\\s*" + WORD + "(\\." + WORD + ")*$";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final Pattern IP_DOMAIN_PATTERN = Pattern.compile(IP_DOMAIN_REGEX);
    private static final Pattern USER_PATTERN = Pattern.compile(USER_REGEX);

    private static final int MAX_USERNAME_LEN = 64;

    private final boolean allowLocal;
    private final boolean allowTld;

    /**
     * Singleton instance of this class, which
     *  doesn't consider local addresses as valid.
     */
    private static final EmailValidator EMAIL_VALIDATOR = new EmailValidator(false, false);

    /**
     * Singleton instance of this class, which
     *  doesn't consider local addresses as valid.
     */
    private static final EmailValidator EMAIL_VALIDATOR_WITH_TLD = new EmailValidator(false, true);

    /**
     * Singleton instance of this class, which does
     *  consider local addresses valid.
     */
    private static final EmailValidator EMAIL_VALIDATOR_WITH_LOCAL = new EmailValidator(true, false);

    /**
     * Returns the Singleton instance of this validator.
     *
     * @return singleton instance of this validator.
     */
    public static EmailValidator getInstance() {
        return EMAIL_VALIDATOR;
    }

    /**
     * Returns the Singleton instance of this validator,
     *  with local validation as required.
     *
     * @param allowLocal Should local addresses be considered valid?
     * @param allowTld Should TLDs be allowed?
     * @return singleton instance of this validator
     */
    public static EmailValidator getInstance(boolean allowLocal, boolean allowTld) {
        if (allowLocal) {
            return EMAIL_VALIDATOR_WITH_LOCAL;
        } else {
            if (allowTld) {
                return EMAIL_VALIDATOR_WITH_TLD;
            } else {
                return EMAIL_VALIDATOR;
            }
        }
    }

    /**
     * Returns the Singleton instance of this validator,
     *  with local validation as required.
     *
     * @param allowLocal Should local addresses be considered valid?
     * @return singleton instance of this validator
     */
    public static EmailValidator getInstance(boolean allowLocal) {
        return getInstance(allowLocal, false);
    }

    /**
     * Protected constructor for subclasses to use.
     *
     * @param allowLocal Should local addresses be considered valid?
     * @param allowTld Should TLDs be allowed?
     */
    protected EmailValidator(boolean allowLocal, boolean allowTld) {
        super();
        this.allowLocal = allowLocal;
        this.allowTld = allowTld;
    }

    /**
     * Checks if a field has a valid e-mail address.
     *
     * @param email The value validation is being performed on. A <code>null</code>
     *              value is considered invalid.
     * @return true if the email address is valid.
     */
    @Override
    public boolean isValid(String email) {
        if (email == null) {
            return false;
        }

        if (email.endsWith(".")) { // check this first - it's cheap!
            setErrorMessage(tr("E-mail address is invalid"));
            return false;
        }

        // Check the whole email address structure
        Matcher emailMatcher = EMAIL_PATTERN.matcher(email);
        if (!emailMatcher.matches()) {
            setErrorMessage(tr("E-mail address is invalid"));
            return false;
        }

        String username = emailMatcher.group(1);
        if (!isValidUser(username)) {
            setErrorMessage(tr("E-mail address contains an invalid username: {0}", username));
            return false;
        }

        String domain = emailMatcher.group(2);
        if (!isValidDomain(domain)) {
            setErrorMessage(tr("E-mail address contains an invalid domain: {0}", domain));
            return false;
        }

        return true;
    }

    @Override
    public String getValidatorName() {
        return tr("Email validator");
    }

    /**
     * Returns true if the domain component of an email address is valid.
     *
     * @param domain being validated, may be in IDN format
     * @return true if the email address's domain is valid.
     */
    protected boolean isValidDomain(String domain) {
        // see if domain is an IP address in brackets
        Matcher ipDomainMatcher = IP_DOMAIN_PATTERN.matcher(domain);

        if (ipDomainMatcher.matches()) {
            InetAddressValidator inetAddressValidator =
                    InetAddressValidator.getInstance();
            return inetAddressValidator.isValid(ipDomainMatcher.group(1));
        }
        // Domain is symbolic name
        DomainValidator domainValidator =
                DomainValidator.getInstance(allowLocal);
        if (allowTld) {
            return domainValidator.isValid(domain) || (!domain.startsWith(".") && domainValidator.isValidTld(domain));
        } else {
            return domainValidator.isValid(domain);
        }
    }

    /**
     * Returns true if the user component of an email address is valid.
     *
     * @param user being validated
     * @return true if the user name is valid.
     */
    protected boolean isValidUser(String user) {

        if (user == null || user.length() > MAX_USERNAME_LEN) {
            return false;
        }

        return USER_PATTERN.matcher(user).matches();
    }

}
