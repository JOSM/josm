// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.Arrays;

import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;

abstract public class Instruction implements StyleKeys {

    public abstract void execute(Environment env);

    public static class RelativeFloat {
        public final float val;

        public RelativeFloat(float val) {
            this.val = val;
        }

        @Override
        public String toString() {
            return "RelativeFloat{" + "val=" + val + '}';
        }
    }

    public static class AssignmentInstruction extends Instruction {
        public final String key;
        public final Object val;
        public final boolean isSetInstruction;

        public AssignmentInstruction(String key, Object val, boolean isSetInstruction) {
            this.key = key;
            this.isSetInstruction = isSetInstruction;
            if (val instanceof LiteralExpression) {
                Object litValue = ((LiteralExpression) val).evaluate(null);
                if (key.equals(TEXT)) {
                    /* Special case for declaration 'text: ...'
                     *
                     * - Treat the value 'auto' as keyword.
                     * - Treat any other literal value 'litval' as as reference to tag with key 'litval'
                     *
                     * - Accept function expressions as is. This allows for
                     *     tag(a_tag_name)                 value of a tag
                     *     eval("a static text")           a static text
                     *     parent_tag(a_tag_name)          value of a tag of a parent relation
                     */
                    if (litValue.equals(Keyword.AUTO)) {
                        this.val = Keyword.AUTO;
                    } else {
                        String s = Cascade.convertTo(litValue, String.class);
                        if (s != null) {
                            this.val = new MapPaintStyles.TagKeyReference(s);
                        } else {
                            this.val = litValue;
                        }
                    }
                } else {
                    this.val = litValue;
                }
            } else {
                this.val = val;
            }
        }

        @Override
        public void execute(Environment env) {
            Object value = null;
            if (val instanceof Expression) {
                value = ((Expression) val).evaluate(env);
            } else {
                value = val;
            }
            if (key.equals(ICON_IMAGE) || key.equals(FILL_IMAGE) || key.equals("pattern-image") || key.equals(REPEAT_IMAGE)) {
                if (value instanceof String) {
                    value = new IconReference((String) value, env.source);
                }
            }
            env.mc.getOrCreateCascade(env.layer).putOrClear(key, value);
        }

        @Override
        public String toString() {
            return key + ':' + (val instanceof float[] ? Arrays.toString((float[]) val) : val) + ';';
        }
    }
}
