// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.List;

import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.Utils;

public class MapCSSRule {

    public final Selector selector;
    public final Declaration declaration;

    public static class Declaration {
        public final List<Instruction> instructions;
        // declarations in the StyleSource are numbered consecutively
        public final int idx;

        public Declaration(List<Instruction> instructions, int idx) {
            this.instructions = instructions;
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
    }
    
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
    public String toString() {
        return selector + " {\n  " + Utils.join("\n  ", declaration.instructions) + "\n}";
    }
}

