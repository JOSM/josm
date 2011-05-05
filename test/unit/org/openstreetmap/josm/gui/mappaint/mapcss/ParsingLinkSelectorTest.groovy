// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.Assert.*

import org.junit.*
import org.openstreetmap.josm.fixtures.JOSMFixture


class ParsingLinkSelectorTest {

    @BeforeClass
    public static void createJOSMFixture(){
        JOSMFixture.createUnitTestFixture().init()
    }
    
    @Test
    public void parseEmptyChildSelector() {
        def css = """
           relation > way {}
        """
        MapCSSStyleSource source = new MapCSSStyleSource(css)
        source.loadStyleSource()        
        assert source.rules.size() == 1
    }
    
    @Test
    public void parseEmptyParentSelector() {
        def css = """
           way < relation {}
        """
        MapCSSStyleSource source = new MapCSSStyleSource(css)
        source.loadStyleSource()
        assert source.rules.size() == 1
    }
    
    
    @Test
    public void parseChildSelectorWithKeyValueCondition() {
        def css = """
           relation >[role="my_role"] way {}
        """
        MapCSSStyleSource source = new MapCSSStyleSource(css)
        source.loadStyleSource()
        assert source.rules.size() == 1
    }
    
    @Test
    public void parseChildSelectorWithKeyCondition() {
        def css = """
           relation >["my_role"] way{}
        """
        MapCSSStyleSource source = new MapCSSStyleSource(css)
        source.loadStyleSource()
        assert source.rules.size() == 1
    }
}

