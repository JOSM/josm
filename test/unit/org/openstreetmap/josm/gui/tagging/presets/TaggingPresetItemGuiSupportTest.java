// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.template_engine.TemplateEntry;
import org.openstreetmap.josm.tools.template_engine.TemplateParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests of {@link TaggingPresetItemGuiSupport}
 */
class TaggingPresetItemGuiSupportTest {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Tests {@link TemplateEntry} evaluation
     */
    @Test
    void testTemplate() throws Exception {
        ArrayList<Tag> tags = new ArrayList<>(Arrays.asList(
                new Tag("name", "xxx"),
                new Tag("from", "Foo"),
                new Tag("to", "Bar")));
        Collection<OsmPrimitive> primitives = Collections.singleton(
                OsmUtils.createPrimitive("relation ref=42"));

        TaggingPresetItemGuiSupport support = TaggingPresetItemGuiSupport.create(false, primitives, () -> tags);
        TemplateEntry templateEntry = new TemplateParser("Bus {ref}: {from} -> {to}").parse();
        assertEquals("Bus 42: Foo -> Bar", templateEntry.getText(support));
    }
}
