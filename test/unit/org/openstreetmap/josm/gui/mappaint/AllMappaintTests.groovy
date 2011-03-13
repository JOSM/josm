// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint

import junit.framework.TestCase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses([
    LabelCompositionStrategyTest.class,
    MapCSSWithExtendedTextDirectivesTest.class
])
public class AllMappaintTests extends TestCase{}

