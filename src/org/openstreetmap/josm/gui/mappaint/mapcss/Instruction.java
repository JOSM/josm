// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.Arrays;

import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;

abstract public class Instruction {

    public abstract void execute(Environment env);

    public static class RelativeFloat {
        public float val;

        public RelativeFloat(float val) {
            this.val = val;
        }

        @Override
        public String toString() {
            return "RelativeFloat{" + "val=" + val + '}';
        }
    }

    public static class AssignmentInstruction extends Instruction {
        String key;
        Object val;

        public AssignmentInstruction(String key, Object val) {
            this.key = key;
            this.val = val;
        }

        @Override
        public void execute(Environment env) {
            Object value = (val instanceof Expression) ? ((Expression) val).evaluate(env) : val;
            if (key.equals("icon-image")) {
                if (value instanceof String) {
                    value = new IconReference((String) value, env.source);
                }
            }
            env.getCascade().putOrClear(key, value);
        }

        @Override
        public String toString() {
            return key + ':' + (val instanceof float[] ? Arrays.toString((float[]) val) : val) + ';';
        }
    }
}
