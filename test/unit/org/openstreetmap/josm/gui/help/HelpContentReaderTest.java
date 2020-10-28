// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link HelpContentReader} class.
 */
class HelpContentReaderTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().timeout(30000);

    /**
     * Unit test of {@link HelpContentReader#fetchHelpTopicContent} - null case.
     */
    @Test
    void testFetchHelpTopicContentNull() {
        assertThrows(HelpContentReaderException.class, () -> new HelpContentReader(null).fetchHelpTopicContent(null, false));
    }

    /**
     * Unit test of {@link HelpContentReader#fetchHelpTopicContent} - nominal case.
     * @throws HelpContentReaderException never
     */
    @Test
    void testFetchHelpTopicContentNominal() throws HelpContentReaderException {
        String res = new HelpContentReader(HelpUtil.getWikiBaseUrl()).fetchHelpTopicContent(HelpBrowserTest.URL_1, false);
        assertFalse(res.trim().isEmpty());
    }
}
