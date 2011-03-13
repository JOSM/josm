// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint

import org.junit.*
import org.openstreetmap.josm.fixtures.JOSMFixture;
import org.openstreetmap.josm.gui.mappaint.LabelCompositionStrategy.DeriveLabelFromNameTagsCompositionStrategy 
import org.openstreetmap.josm.gui.mappaint.LabelCompositionStrategy.StaticLabelCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.LabelCompositionStrategy.TagLookupCompositionStrategy 
import org.openstreetmap.josm.data.osm.Node;

class LabelCompositionStrategyTest {
    
    @BeforeClass
    public static void createJOSMFixture(){
        JOSMFixture.createUnitTestFixture().init()
    }

    @Test
    public void createStaticLabelCompositionStrategy() {
        def n = new Node()
        
        def strat = new StaticLabelCompositionStrategy(null)
        assert strat.compose(n) == null
        
        strat = new StaticLabelCompositionStrategy("a label")
        assert strat.compose(n) == "a label"        
    }
    
    @Test
    public void createTagLookupCompositionStrategy() {
        def n = new Node()
        n.put("my-tag", "my-value")
        
        def strat = new TagLookupCompositionStrategy(null)
        assert strat.compose(n) == null
        
        strat = new TagLookupCompositionStrategy("name")
        assert strat.compose(n) == null
        
        strat = new TagLookupCompositionStrategy("my-tag")
        assert strat.compose(n) == "my-value"
    }
    
    @Test
    public void createDeriveLabelFromNameTagsCompositionStrategy() {
        def n 
        def strat
        
        strat = new DeriveLabelFromNameTagsCompositionStrategy()
        strat.setNameTags(null)
        assert strat.getNameTags() == []
        
        strat = new DeriveLabelFromNameTagsCompositionStrategy()
        strat.setNameTags(["name", "brand"])
        assert strat.getNameTags() == ["name", "brand"]
        
        n = new Node()
        n.put("brand", "my brand")        
        assert strat.compose(n) == "my brand"
        
        n = new Node()
        n.put("name", "my name")
        n.put("brand", "my brand")
        assert strat.compose(n) == "my name"        
    }
}

