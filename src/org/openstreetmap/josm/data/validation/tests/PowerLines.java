// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

public class PowerLines extends Test {
    
    protected static final int POWER_LINES = 2501;
    
    protected Map<Way, String> towerPoleTagMap = new HashMap<Way, String>();
    
    public PowerLines() {
        super(tr("Power lines"), tr("Checks for nodes in power lines that do not have a power=tower/pole tag."));
    }
    
    @Override
    public void visit(Way w) {
        if (w.isUsable() && isPowerLine(w)) {
            String fixValue = null;
            boolean erroneous = false;
            boolean canFix = false;
            for (Node n : w.getNodes()) {
                if (!isPowerTower(n)) {
                    errors.add(new PowerLineError(n, w));
                    erroneous = true;
                } else if (fixValue == null) {
                    // First tower/pole tag found, remember it
                    fixValue = n.get("power");
                    canFix = true;
                } else if (!fixValue.equals(n.get("power"))) {
                    // The power line contains both "tower" and "pole" -> cannot fix this error
                    canFix = false;
                }
            }
            if (erroneous && canFix) {
                towerPoleTagMap.put(w, fixValue);
            }
        }
    }

    @Override
    public Command fixError(TestError testError) {
        if (isFixable(testError)) {
            return new ChangePropertyCommand(
                    testError.getPrimitives().iterator().next(), 
                    "power", towerPoleTagMap.get(((PowerLineError)testError).line));
        }
        return null;
    }

    @Override
    public boolean isFixable(TestError testError) {
        return testError instanceof PowerLineError && towerPoleTagMap.containsKey(((PowerLineError)testError).line);
    }
    
    /**
     * Determines if the specified way denotes a power line.
     * @param w The way to be tested
     * @return True if power key is set and equal to line/minor_line
     */
    protected static final boolean isPowerLine(Way w) {
        String v = w.get("power");
        return v != null && (v.equals("line") || v.equals("minor_line"));
    }

    /**
     * Determines if the specified node denotes a power tower/pole.
     * @param w The node to be tested
     * @return True if power key is set and equal to tower/pole
     */
    protected static final boolean isPowerTower(Node n) {
        String v = n.get("power");
        return v != null && (v.equals("tower") || v.equals("pole"));
    }
    
    protected class PowerLineError extends TestError {
        public final Way line;
        public PowerLineError(Node n, Way line) {
            super(PowerLines.this, Severity.WARNING, 
                    tr("Missing power tower/pole within power line"), POWER_LINES, n);
            this.line = line;
        }
    }
}
