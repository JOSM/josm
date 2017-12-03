// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.junit.Assert.assertFalse;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link HelpContentReader} class.
 */
public class HelpContentReaderTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().timeout(20000);

    /**
     * Unit test of {@link HelpContentReader#fetchHelpTopicContent} - null case.
     * @throws HelpContentReaderException always
     */
    @Test(expected = HelpContentReaderException.class)
    public void testFetchHelpTopicContentNull() throws HelpContentReaderException {
        new HelpContentReader(null).fetchHelpTopicContent(null, false);
    }

    /**
     * Unit test of {@link HelpContentReader#fetchHelpTopicContent} - nominal case.
     * @throws HelpContentReaderException never
     */
    @Test
    public void testFetchHelpTopicContentNominal() throws HelpContentReaderException {
        String res = new HelpContentReader(HelpUtil.getWikiBaseUrl()).fetchHelpTopicContent(HelpBrowserTest.URL_1, false);
        assertFalse(res.trim().isEmpty());
    }
}
