// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.List;

import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.Utils;

public class MapCSSRule {

    public List<Selector> selectors;
    public List<Instruction> declaration;

    public MapCSSRule(List<Selector> selectors, List<Instruction> declaration) {
        this.selectors = selectors;
        this.declaration = declaration;
    }

    /**
     * <p>Executes the instructions against the environment {@code env}</p>
     *
     * @param env the environment
     */
    public void execute(Environment env) {
        for (Instruction i : declaration) {
            i.execute(env);
        }
    }

    @Override
    public String toString() {
        return Utils.join(",", selectors) + " {\n  " + Utils.join("\n  ", declaration) + "\n}";
    }
}

