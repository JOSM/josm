// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.stream.Collectors;

import org.openstreetmap.josm.gui.mappaint.Environment;

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
    public final Selector selector;
    /**
     * The instructions for this selector
     */
    public final Declaration declaration;

    /**
     * Constructs a new {@code MapCSSRule}.
     * @param selector The selector
     * @param declaration The declaration
     */
    public MapCSSRule(Selector selector, Declaration declaration) {
        this.selector = selector;
        this.declaration = declaration;
    }

    /**
     * <p>Executes the instructions against the environment {@code env}</p>
     *
     * @param env the environment
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
        return selector + declaration.instructions.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\n  ", " {\n  ", "\n}"));
    }
}

