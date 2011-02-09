// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.List;

import org.openstreetmap.josm.tools.Utils;

public class MapCSSRule {
    
    public List<Selector> selectors;
    public List<Instruction> declaration;

    public MapCSSRule(List<Selector> selectors, List<Instruction> declaration) {
        this.selectors = selectors;
        this.declaration = declaration;
    }

    @Override
    public String toString() {
        return Utils.join(",", selectors) + " {\n  " + Utils.join("\n  ", declaration) + "\n}";
    }
}

