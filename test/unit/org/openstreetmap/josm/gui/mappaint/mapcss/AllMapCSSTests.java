// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * All MapCSS tests.
 */
@Suite
@SuiteDisplayName("All MapCSS Tests")
@SelectClasses({
    KeyValueConditionTest.class,
    ParsingLinkSelectorTest.class,
    KeyConditionTest.class,
    MapCSSParserTest.class,
    ChildOrParentSelectorTest.class
})
public class AllMapCSSTests {

}
