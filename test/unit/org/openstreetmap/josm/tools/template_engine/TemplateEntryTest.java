// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.tools.Logging;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link TemplateEntry} class.
 */
class TemplateEntryTest {
    /**
     * Unit test of methods {@link TemplateEntry#equals} and {@link TemplateEntry#hashCode}, including all subclasses.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        Set<Class<? extends TemplateEntry>> templates = TestUtils.getJosmSubtypes(TemplateEntry.class);
        assertTrue(templates.size() >= 3); // if it finds less than 3 classes, something is broken
        for (Class<?> c : templates) {
            Logging.debug(c.toString());
            EqualsVerifier.forClass(c).usingGetClass()
                .suppress(Warning.NONFINAL_FIELDS, Warning.INHERITED_DIRECTLY_FROM_OBJECT)
                .verify();
        }
    }
}
