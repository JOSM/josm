// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
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
    public final Selector selector;
    /**
     * The instructions for this selector
     */
    public final Declaration declaration;

    /**
     * A declaration is a set of {@link Instruction}s
     */
    public static class Declaration {
        /**
         * The instructions in this declaration
         */
        public final List<Instruction> instructions;
        /**
         * The index of this declaration
         * <p>
         * declarations in the StyleSource are numbered consecutively
         */
        public final int idx;

        /**
         * Create a new {@link Declaration}
         * @param instructions The instructions for this dectlaration
         * @param idx The index in the {@link StyleSource}
         */
        public Declaration(List<Instruction> instructions, int idx) {
            this.instructions = Utils.toUnmodifiableList(instructions);
            this.idx = idx;
        }

        /**
         * <p>Executes the instructions against the environment {@code env}</p>
         *
         * @param env the environment
         */
        public void execute(Environment env) {
            for (Instruction i : instructions) {
                i.execute(env);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(instructions, idx);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Declaration that = (Declaration) obj;
            return idx == that.idx &&
                    Objects.equals(instructions, that.instructions);
        }

        @Override
        public String toString() {
            return "Declaration [instructions=" + instructions + ", idx=" + idx + ']';
        }
    }

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

