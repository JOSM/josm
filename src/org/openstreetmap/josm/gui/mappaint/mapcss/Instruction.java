// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.Arrays;

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
            if (val instanceof Expression) {
                env.getCascade().putOrClear(key, ((Expression) val).evaluate(env));
            } else {
                env.getCascade().putOrClear(key, val);
            }
        }

        @Override
        public String toString() {
            return key + ':' + (val instanceof float[] ? Arrays.toString((float[]) val) : val) + ';';
        }
    }
}
