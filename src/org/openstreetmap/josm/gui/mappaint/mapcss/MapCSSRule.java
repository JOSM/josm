// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.List;

import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.Utils;

public class MapCSSRule {

    public Selector selector;
    public Declaration declaration;

    public static class Declaration {
        public List<Instruction> instructions;
        // usedId is an optimized way to make sure that
        // each declaration is only applied once for each primitive,
        // even if multiple of the comma separated selectors in the
        // rule match.
        public int usedId;

        public Declaration(List<Instruction> instructions) {
            this.instructions = instructions;
            usedId = 0;
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
        for (Instruction i : declaration.instructions) {
            i.execute(env);
        }
    }

    @Override
    public String toString() {
        return selector + " {\n  " + Utils.join("\n  ", declaration.instructions) + "\n}";
    }
}

