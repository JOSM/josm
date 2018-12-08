// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Check for missing name:* translations.
 * <p>
 * This test finds multilingual objects whose 'name' attribute is not
 * equal to any 'name:*' attribute and not a composition of some
 * 'name:*' attributes separated by ' - '.
 * <p>
 * For example, a node with name=Europe, name:de=Europa should have
 * name:en=Europe to avoid triggering this test.  An object with
 * name='Suomi - Finland' should have at least name:fi=Suomi and
 * name:sv=Finland to avoid a warning (name:et=Soome would not
 * matter).  Also, complain if an object has some name:* attribute but
 * no name.
 *
 * @author Skela
 */
public class NameMismatch extends Test.TagTest {
    protected static final int NAME_MISSING = 1501;
    protected static final int NAME_TRANSLATION_MISSING = 1502;
    private static final Pattern NAME_SPLIT_PATTERN = Pattern.compile(" - ");

    private static final List<String> EXCLUSIONS = Arrays.asList(
            "name:botanical",
            "name:etymology:wikidata",
            "name:full",
            "name:genitive",
            "name:left",
            "name:prefix",
            "name:right",
            "name:source"
            );

    /**
     * Constructs a new {@code NameMismatch} test.
     */
    public NameMismatch() {
        super(tr("Missing name:* translation"),
            tr("This test finds multilingual objects whose ''name'' attribute is not equal to some ''name:*'' attribute " +
                    "and not a composition of ''name:*'' attributes, e.g., Italia - Italien - Italy."));
    }

    /**
     * Report a missing translation.
     *
     * @param p The primitive whose translation is missing
     * @param name The name whose translation is missing
     */
    private void missingTranslation(OsmPrimitive p, String name) {
        errors.add(TestError.builder(this, Severity.OTHER, NAME_TRANSLATION_MISSING)
                .message(tr("Missing name:* translation"), marktr("Missing name:*={0}. Add tag with correct language key."), name)
                .primitives(p)
                .build());
    }

    /**
     * Check a primitive for a name mismatch.
     *
     * @param p The primitive to be tested
     */
    @Override
    public void check(OsmPrimitive p) {
        Set<String> names = p.getKeys().entrySet().stream()
                .filter(e -> e.getValue() != null && e.getKey().startsWith("name:") && !EXCLUSIONS.contains(e.getKey()))
                .map(Entry::getValue)
                .collect(Collectors.toSet());

        if (names.isEmpty()) return;

        String name = p.get("name");

        if (name == null) {
            errors.add(TestError.builder(this, Severity.OTHER, NAME_MISSING)
                    .message(tr("A name is missing, even though name:* exists."))
                    .primitives(p)
                    .build());
            return;
        }

        if (names.contains(name)) return;
        /* If name is not equal to one of the name:*, it should be a
        composition of some (not necessarily all) name:* labels.
        Check if this is the case. */

        String[] splitNames = NAME_SPLIT_PATTERN.split(name);
        if (splitNames.length == 1) {
            /* The name is not composed of multiple parts. Complain. */
            missingTranslation(p, splitNames[0]);
            return;
        }

        /* Check that each part corresponds to a translated name:*. */
        for (String n : splitNames) {
            if (!names.contains(n)) {
                missingTranslation(p, n);
            }
        }
    }
}
