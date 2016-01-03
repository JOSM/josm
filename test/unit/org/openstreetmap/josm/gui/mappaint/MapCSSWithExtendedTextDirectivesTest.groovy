// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint
import java.awt.Color

import org.junit.*
import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.TagKeyReference
import org.openstreetmap.josm.gui.mappaint.styleelement.TextLabel
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.DeriveLabelFromNameTagsCompositionStrategy
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.TagLookupCompositionStrategy

class MapCSSWithExtendedTextDirectivesTest {

    @BeforeClass
    public static void createJOSMFixture(){
        JOSMFixture.createUnitTestFixture().init()
    }

    @Test
    public void createAutoTextElement() {
        MultiCascade mc = new MultiCascade()
        Cascade c = mc.getOrCreateCascade("default")
        c.put("text", new Keyword("auto"))
        Node osm = new Node()
        osm.put("ref", "A456");
        Environment env = new Environment(osm, mc, "default", null)

        TextLabel te = TextLabel.create(env, Color.WHITE, false /* no default annotate */)
        assert te.labelCompositionStrategy != null
        assert te.labelCompositionStrategy instanceof DeriveLabelFromNameTagsCompositionStrategy
    }

    @Test
    public void createTextElementComposingTextFromTag() {
        MultiCascade mc = new MultiCascade()
        Cascade c = mc.getOrCreateCascade("default")
        c.put("text", new TagKeyReference("my_name"))
        Node osm = new Node()
        osm.put("my_name", "foobar");
        Environment env = new Environment(osm, mc, "default", null)

        TextLabel te = TextLabel.create(env, Color.WHITE, false /* no default annotate */)
        assert te.labelCompositionStrategy != null
        assert te.labelCompositionStrategy instanceof TagLookupCompositionStrategy
        assert te.labelCompositionStrategy.getDefaultLabelTag() == "my_name"
    }

    @Test
    public void createNullStrategy() {
        MultiCascade mc = new MultiCascade()
        Node osm = new Node()
        Environment env = new Environment(osm, mc, "default", null)

        TextLabel te = TextLabel.create(env, Color.WHITE, false /* no default annotate */)
        assert te == null
    }
}
