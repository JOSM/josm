// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint
import java.awt.Color

import org.junit.*
import org.openstreetmap.josm.fixtures.JOSMFixture
import org.openstreetmap.josm.gui.mappaint.LabelCompositionStrategy.DeriveLabelFromNameTagsCompositionStrategy
import org.openstreetmap.josm.gui.mappaint.LabelCompositionStrategy.TagLookupCompositionStrategy
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.TagKeyReference
class MapCSSWithExtendedTextDirectivesTest {
    

    @BeforeClass
    public static void createJOSMFixture(){
        JOSMFixture.createUnitTestFixture().init()
    }

    @Test
    public void createAutoTextElement() {
        Cascade c = new Cascade()
        c.put("text", new Keyword("auto"))
        
        TextElement te = TextElement.create(c, Color.WHITE, false /* no default annotate */)
        assert te.labelCompositionStrategy != null
        assert te.labelCompositionStrategy instanceof DeriveLabelFromNameTagsCompositionStrategy
    }
    
    @Test
    public void createTextElementComposingTextFromTag() {
        Cascade c = new Cascade()
        c.put("text", new TagKeyReference("my_name"))
        
        TextElement te = TextElement.create(c, Color.WHITE, false /* no default annotate */)
        assert te.labelCompositionStrategy != null
        assert te.labelCompositionStrategy instanceof TagLookupCompositionStrategy
        assert te.labelCompositionStrategy.getDefaultLabelTag() == "my_name"
    }
        
    @Test
    public void createNullStrategy() {
        Cascade c = new Cascade()
        
        TextElement te = TextElement.create(c, Color.WHITE, false /* no default annotate */)
        assert te.labelCompositionStrategy == null
    }
}

