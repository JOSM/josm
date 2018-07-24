// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.openstreetmap.josm.gui.mappaint.mapcss.AllMapCSSTests;

import junit.framework.TestCase;

/**
 * All mappaint tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    LabelCompositionStrategyTest.class,
    MapCSSWithExtendedTextDirectivesTest.class,
    AllMapCSSTests.class
})
public class AllMappaintTests extends TestCase{

}
