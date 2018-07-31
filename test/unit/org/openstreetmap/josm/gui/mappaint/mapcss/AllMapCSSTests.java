// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * All MapCSS tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    KeyValueConditionTest.class,
    ParsingLinkSelectorTest.class,
    KeyConditionTest.class,
    MapCSSParserTest.class,
    ChildOrParentSelectorTest.class
})
public class AllMapCSSTests {

}
