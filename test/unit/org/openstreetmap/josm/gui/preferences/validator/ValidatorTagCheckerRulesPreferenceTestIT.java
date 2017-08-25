// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.ParseResult;

/**
 * Integration tests of {@link ValidatorTagCheckerRulesPreference} class.
 */
public class ValidatorTagCheckerRulesPreferenceTestIT {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test that available tag checker rules are valid.
     * @throws Exception in case of error
     */
    @Test
    public void testValidityOfAvailableRules() throws Exception {
        Collection<ExtendedSourceEntry> sources = new ValidatorTagCheckerRulesPreference.TagCheckerRulesSourceEditor()
                .loadAndGetAvailableSources();
        assertFalse(sources.isEmpty());
        Collection<Throwable> allErrors = new ArrayList<>();
        MapCSSTagChecker tagChecker = new MapCSSTagChecker();
        for (ExtendedSourceEntry source : sources) {
            System.out.print(source.url);
            try {
                ParseResult result = tagChecker.addMapCSS(source.url);
                assertFalse(result.parseChecks.isEmpty());
                System.out.println(result.parseErrors.isEmpty() ? " => OK" : " => KO");
                allErrors.addAll(result.parseErrors);
            } catch (IOException e) {
                System.out.println(" => KO");
                allErrors.add(e);
                e.printStackTrace();
            }
        }
        assertTrue(allErrors.isEmpty());
    }
}
