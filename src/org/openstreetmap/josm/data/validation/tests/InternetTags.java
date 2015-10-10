// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.IDN;
import java.util.regex.Pattern;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.FixableTestError;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.routines.AbstractValidator;
import org.openstreetmap.josm.data.validation.routines.EmailValidator;
import org.openstreetmap.josm.data.validation.routines.UrlValidator;

/**
 * Performs validation tests on internet-related tags (websites, e-mail addresses, etc.).
 * @since 7489
 */
public class InternetTags extends Test {

    /** Error code for an invalid URL */
    public static final int INVALID_URL = 3301;
    /** Error code for an invalid e-mail */
    public static final int INVALID_EMAIL = 3302;

    private static final Pattern ASCII_PATTERN = Pattern.compile("^\\p{ASCII}+$");

    /**
     * List of keys subject to URL validation.
     */
    private static String[] URL_KEYS = new String[] {
        "url", "source:url",
        "website", "contact:website", "heritage:website", "source:website"
    };

    /**
     * List of keys subject to email validation.
     */
    private static String[] EMAIL_KEYS = new String[] {
        "email", "contact:email"
    };

    /**
     * Constructs a new {@code InternetTags} test.
     */
    public InternetTags() {
        super(tr("Internet tags"), tr("Checks for errors in internet-related tags."));
    }

    /**
     * Potentially validates a given primitive key against a given validator.
     * @param p The OSM primitive to test
     * @param k The key to validate
     * @param keys The list of keys to check. If {@code k} is not inside this collection, do nothing
     * @param validator The validator to run if {@code k} is inside {@code keys}
     * @param code The error code to set if the validation fails
     * @return {@code true} if the validation fails. In this case, a new error has been created.
     */
    private boolean doTest(OsmPrimitive p, String k, String[] keys, AbstractValidator validator, int code) {
        for (String i : keys) {
            if (i.equals(k)) {
                TestError error = validateTag(p, k, validator, code);
                if (error != null) {
                    errors.add(error);
                }
                break;
            }
        }
        return false;
    }

    /**
     * Validates a given primitive tag against a given validator.
     * @param p The OSM primitive to test
     * @param k The key to validate
     * @param validator The validator to run
     * @param code The error code to set if the validation fails
     * @return The error if the validation fails, {@code null} otherwise
     * @since 7824
     */
    public TestError validateTag(OsmPrimitive p, String k, AbstractValidator validator, int code) {
        TestError error = doValidateTag(p, k, null, validator, code);
        if (error != null) {
            // Workaround to https://issues.apache.org/jira/browse/VALIDATOR-290
            // Apache Commons Validator 1.4.1-SNAPSHOT does not support yet IDN URLs
            // To remove if it gets fixed on Apache side
            String v = p.get(k);
            if (!ASCII_PATTERN.matcher(v).matches()) {
                try {
                    String protocol = "";
                    if (v.contains("://")) {
                        protocol = v.substring(0, v.indexOf("://")+3);
                    }
                    String domain = !protocol.isEmpty() ? v.substring(protocol.length(), v.length()) : v;
                    String ending = "";
                    if (domain.contains("/")) {
                        int idx = domain.indexOf('/');
                        ending = domain.substring(idx, domain.length());
                        domain = domain.substring(0, idx);
                    }
                    // Try to apply ToASCII algorithm
                    error = doValidateTag(p, k, protocol+IDN.toASCII(domain)+ending, validator, code);
                } catch (IllegalArgumentException e) {
                    error.setMessage(error.getMessage() +
                            tr(" URL cannot be converted to ASCII: {0}", e.getMessage()));
                }
            }
        }
        return error;
    }

    /**
     * Validates a given primitive tag against a given validator.
     * @param p The OSM primitive to test
     * @param k The key to validate
     * @param v The value to validate. May be {@code null} to use {@code p.get(k)}
     * @param validator The validator to run
     * @param code The error code to set if the validation fails
     * @return The error if the validation fails, {@code null} otherwise
     */
    private TestError doValidateTag(OsmPrimitive p, String k, String v, AbstractValidator validator, int code) {
        TestError error = null;
        String value = v != null ? v : p.get(k);
        if (!validator.isValid(value)) {
            String errMsg = validator.getErrorMessage();
            // Special treatment to allow URLs without protocol. See UrlValidator#isValid
            if (tr("URL contains an invalid protocol: {0}", (String) null).equals(errMsg)) {
                String proto = validator instanceof EmailValidator ? "mailto://" : "http://";
                return doValidateTag(p, k, proto+value, validator, code);
            }
            String msg = tr("''{0}'': {1}", k, errMsg);
            String fix = validator.getFix();
            if (fix != null) {
                error = new FixableTestError(this, Severity.WARNING, msg, code, p,
                        new ChangePropertyCommand(p, k, fix));
            } else {
                error = new TestError(this, Severity.WARNING, msg, code, p);
            }
        }
        return error;
    }

    private void test(OsmPrimitive p) {
        for (String k : p.keySet()) {
            // Test key against URL validator
            if (!doTest(p, k, URL_KEYS, UrlValidator.getInstance(), INVALID_URL)) {
                // Test key against e-mail validator only if the URL validator did not fail
                doTest(p, k, EMAIL_KEYS, EmailValidator.getInstance(), INVALID_EMAIL);
            }
        }
    }

    @Override
    public void visit(Node n) {
        test(n);
    }

    @Override
    public void visit(Way w) {
        test(w);
    }

    @Override
    public void visit(Relation r) {
        test(r);
    }
}
