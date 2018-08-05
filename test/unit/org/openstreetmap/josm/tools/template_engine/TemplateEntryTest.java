// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link TemplateEntry} class.
 */
public class TemplateEntryTest {

    /**
     * Setup rule.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link TemplateEntry#equals} and {@link TemplateEntry#hashCode}, including all subclasses.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        Set<Class<? extends TemplateEntry>> templates = TestUtils.getJosmSubtypes(TemplateEntry.class);
        Assert.assertTrue(templates.size() >= 3); // if it finds less than 3 classes, something is broken
        for (Class<?> c : templates) {
            Logging.debug(c.toString());
            EqualsVerifier.forClass(c).usingGetClass()
                .suppress(Warning.NONFINAL_FIELDS, Warning.INHERITED_DIRECTLY_FROM_OBJECT)
                .verify();
        }
    }
}
