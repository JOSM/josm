// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.AssignmentInstruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.tools.ImageProvider;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link MapPaintPreference} class.
 */
public class MapPaintPreferenceTestIT {

    /**
     * Global timeout applied to all test methods.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public Timeout globalTimeout = Timeout.seconds(15*60);

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test that available map paint styles are valid.
     * @throws Exception in case of error
     */
    @Test
    public void testValidityOfAvailableStyles() throws Exception {
        ImageProvider.clearCache();
        Collection<ExtendedSourceEntry> sources = new MapPaintPreference.MapPaintSourceEditor()
                .loadAndGetAvailableSources();
        // Drop everything from yopaseopor, pasharm and www.freietonne.de, too many errors
        sources.removeIf(x -> x.url.contains("yopaseopor/") || x.url.contains("/pasharm") || x.url.contains("www.freietonne.de"));
        assertFalse(sources.isEmpty());
        Map<String, Collection<Throwable>> allErrors = new HashMap<>();
        Map<String, Collection<String>> allWarnings = new HashMap<>();
        for (ExtendedSourceEntry source : sources) {
            // Do not validate XML styles
            if (!"xml".equalsIgnoreCase(source.styleType)) {
                System.out.println(source.url);
                StyleSource style = MapPaintStyles.addStyle(source);
                if (style instanceof MapCSSStyleSource) {
                    // Force loading of all icons to detect missing ones
                    for (MapCSSRule rule : ((MapCSSStyleSource) style).rules) {
                        for (Instruction instruction : rule.declaration.instructions) {
                            if (instruction instanceof AssignmentInstruction) {
                                AssignmentInstruction ai = (AssignmentInstruction) instruction;
                                if (StyleKeys.ICON_IMAGE.equals(ai.key)
                                 || StyleKeys.FILL_IMAGE.equals(ai.key)
                                 || StyleKeys.REPEAT_IMAGE.equals(ai.key)) {
                                    if (ai.val instanceof String) {
                                        MapPaintStyles.getIconProvider(new IconReference((String) ai.val, style), true);
                                    }
                                }
                            }
                        }
                    }
                }
                if (style != null) {
                    System.out.println(style.isValid() ? " => OK" : " => KO");
                    Collection<Throwable> errors = style.getErrors();
                    Collection<String> warnings = style.getWarnings();
                    if (!errors.isEmpty()) {
                        allErrors.put(source.url, errors);
                    }
                    if (!warnings.isEmpty()) {
                        allWarnings.put(source.url, warnings);
                    }
                } else {
                    allWarnings.put(source.url, Collections.singleton("MapPaintStyles.addStyle() returned null"));
                }
            }
        }
        ImageProvider.clearCache();
        assertTrue(allErrors.toString()+"\n"+allWarnings.toString(), allErrors.isEmpty() && allWarnings.isEmpty());
    }
}
