// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Unit tests of {@link HelpContentReader} class.
 */
@Timeout(30)
class HelpContentReaderTest {
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
