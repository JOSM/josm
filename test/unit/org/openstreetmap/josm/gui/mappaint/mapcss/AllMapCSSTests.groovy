// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss

import junit.framework.TestCase

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite.class)
@Suite.SuiteClasses([
    KeyValueConditionTest.class,
    ParsingLinkSelectorTest.class,
    KeyConditionTest.class,
    MapCSSParserTest.class,
    ChildOrParentSelectorTest
])
public class AllMapCSSTests extends TestCase{}

