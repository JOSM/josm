// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.Utils;

/**
 * A MapCSS rule.
 *
 * A MapCSS style is simply a list of MapCSS rules. Each rule has a selector
 * and a declaration. Whenever the selector matches the primitive, the
 * declaration block is executed for this primitive.
 */
public class MapCSSRule implements Comparable<MapCSSRule> {

    /**
     * The selector. If it matches, this rule should be applied
     */
    public final List<Selector> selectors;
    /**
     * The instructions for this selector
     */
    public final Declaration declaration;

    /**
     * Constructs a new {@code MapCSSRule}.
     * @param selectors The selectors
     * @param declaration The declaration
     */
    public MapCSSRule(List<Selector> selectors, Declaration declaration) {
        this.selectors = Utils.toUnmodifiableList(selectors);
        this.declaration = declaration;
    }

    /**
     * Test whether the selector of this rule applies to the primitive.
     *
     * @param env the Environment. env.mc and env.layer are read-only when matching a selector.
     * env.source is not needed. This method will set the matchingReferrers field of env as
     * a side effect! Make sure to clear it before invoking this method.
     * @return true, if the selector applies
     * @see Selector#matches
     */
    public boolean matches(Environment env) {
        return selectors.stream().anyMatch(s -> s.matches(env));
    }

    /**
     * <p>Executes the instructions against the environment {@code env}</p>
     *
     * @param env the environment
     * @see Declaration#execute
     */
    public void execute(Environment env) {
        declaration.execute(env);
    }

    @Override
    public int compareTo(MapCSSRule o) {
        return declaration.idx - o.declaration.idx;
    }

    @Override
    public String toString() {
        final String selectorsString = selectors.stream().map(String::valueOf)
                .collect(Collectors.joining(",\n"));
        final String declarationString = declaration.instructions.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n  ", " {\n  ", "\n}"));
        return selectorsString + declarationString;
    }
}

