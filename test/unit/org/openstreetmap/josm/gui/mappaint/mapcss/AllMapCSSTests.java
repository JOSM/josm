// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * All MapCSS tests.
 */
@Suite
@SelectClasses({
    KeyValueConditionTest.class,
    ParsingLinkSelectorTest.class,
    KeyConditionTest.class,
    MapCSSParserTest.class,
    ChildOrParentSelectorTest.class
})
public class AllMapCSSTests {

}
