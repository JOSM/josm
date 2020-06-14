// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
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
public class InternetTags extends Test.TagTest {

    /** Error code for an invalid URL */
    public static final int INVALID_URL = 3301;
    /** Error code for an invalid e-mail */
    public static final int INVALID_EMAIL = 3302;

    /**
     * List of keys subject to URL validation.
     */
    private static final String[] URL_KEYS = {
        "url", "source:url",
        "website", "contact:website", "heritage:website", "source:website"
    };

    /**
     * List of keys subject to email validation.
     */
    private static final String[] EMAIL_KEYS = {
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
                return errors.addAll(validateTag(p, k, validator, code));
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
     * @since 14803 (return type)
     */
    public List<TestError> validateTag(OsmPrimitive p, String k, AbstractValidator validator, int code) {
        return doValidateTag(p, k, null, validator, code);
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
    private List<TestError> doValidateTag(OsmPrimitive p, String k, String v, AbstractValidator validator, int code) {
        List<TestError> errors = new ArrayList<>();
        String values = v != null ? v : p.get(k);
        for (String value : values.split(";", -1)) {
            if (!validator.isValid(value)) {
                Supplier<Command> fix = null;
                String errMsg = validator.getErrorMessage();
                if (tr("URL contains an invalid protocol: {0}", (String) null).equals(errMsg)) {
                    // Special treatment to allow URLs without protocol. See UrlValidator#isValid
                    String proto = validator instanceof EmailValidator ? "mailto://" : "http://";
                    return doValidateTag(p, k, proto+value, validator, code);
                } else if (tr("URL contains an invalid authority: {0}", (String) null).equals(errMsg)
                        && value.contains("\\") && validator.isValid(value.replaceAll("\\\\", "/"))) {
                    // Special treatment to autofix URLs with backslashes. See UrlValidator#isValid
                    errMsg = tr("URL contains backslashes instead of slashes");
                    fix = () -> new ChangePropertyCommand(p, k, value.replaceAll("\\\\", "/"));
                }
                errors.add(TestError.builder(this, Severity.WARNING, code)
                            .message(validator.getValidatorName(), marktr("''{0}'': {1}"), k, errMsg)
                            .primitives(p)
                            .fix(fix)
                            .build());
            }
        }
        return errors;
    }

    @Override
    public void check(OsmPrimitive p) {
        for (String k : p.keySet()) {
            // Test key against URL validator
            if (!doTest(p, k, URL_KEYS, UrlValidator.getInstance(), INVALID_URL)) {
                // Test key against e-mail validator only if the URL validator did not fail
                doTest(p, k, EMAIL_KEYS, EmailValidator.getInstance(), INVALID_EMAIL);
            }
        }
    }
}
