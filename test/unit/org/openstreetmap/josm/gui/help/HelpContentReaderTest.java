// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.junit.Assert.assertFalse;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link HelpContentReader} class.
 */
public class HelpContentReaderTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

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
