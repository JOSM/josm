// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

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

    protected static final int INVALID_URL = 3301;
    protected static final int INVALID_EMAIL = 3302;

    /**
     * List of keys subject to URL validation.
     */
    public static String[] URL_KEYS = new String[] {
        "url", "source:url",
        "website", "contact:website", "heritage:website", "source:website"
    };

    /**
     * List of keys subject to email validation.
     */
    public static String[] EMAIL_KEYS = new String[] {
        "email", "contact:email"
    };

    /**
     * Constructs a new {@code InternetTags} test.
     */
    public InternetTags() {
        super(tr("Internet tags"), tr("Checks for errors in internet-related tags."));
    }

    private boolean doTest(OsmPrimitive p, String k, String[] keys, AbstractValidator validator, int code) {
        for (String i : keys) {
            if (i.equals(k)) {
                if (!validator.isValid(p.get(k))) {
                    TestError error;
                    String msg = tr("''{0}'': {1}", k, validator.getErrorMessage());
                    String fix = validator.getFix();
                    if (fix != null) {
                        error = new FixableTestError(this, Severity.WARNING, msg, code, p,
                                new ChangePropertyCommand(p, k, fix));
                    } else {
                        error = new TestError(this, Severity.WARNING, msg, code, p);
                    }
                    return errors.add(error);
                }
                break;
            }
        }
        return false;
    }

    private void test(OsmPrimitive p) {
        for (String k : p.keySet()) {
            if (!doTest(p, k, URL_KEYS, UrlValidator.getInstance(), INVALID_URL)) {
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
