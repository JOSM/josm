// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.Arrays;

import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;

/**
 * A MapCSS Instruction.
 *
 * For example a simple assignment like <code>width: 3;</code>, but may also
 * be a set instruction (<code>set highway;</code>).
 * A MapCSS {@link Declaration} is a list of instructions.
 */
@FunctionalInterface
public interface Instruction extends StyleKeys {

    /**
     * Execute the instruction in the given environment.
     * @param env the environment
     */
    void execute(Environment env);

    /**
     * A float value that will be added to the current float value. Specified as +5 or -3 in MapCSS
     */
    class RelativeFloat {
        public final float val;

        public RelativeFloat(float val) {
            this.val = val;
        }

        @Override
        public String toString() {
            return "RelativeFloat{" + "val=" + val + '}';
        }
    }

    /**
     * An instruction that assigns a given value to a variable on evaluation
     */
    class AssignmentInstruction implements Instruction {
        public final String key;
        public final Object val;
        public final boolean isSetInstruction;

        public AssignmentInstruction(String key, Object val, boolean isSetInstruction) {
            this.key = key.intern();
            this.isSetInstruction = isSetInstruction;
            if (val instanceof LiteralExpression) {
                Object litValue = ((LiteralExpression) val).evaluate(null);
                if (litValue instanceof Keyword && "none".equals(((Keyword) litValue).val)) {
                    this.val = null;
                } else if (TEXT.equals(key)) {
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
            Object value;
            if (val instanceof Expression) {
                value = ((Expression) val).evaluate(env);
            } else {
                value = val;
            }
            if (ICON_IMAGE.equals(key) || FILL_IMAGE.equals(key) || REPEAT_IMAGE.equals(key)) {
                if (value instanceof String) {
                    value = new IconReference((String) value, env.source);
                }
            }
            env.mc.getOrCreateCascade(env.layer).putOrClear(key, value);
        }

        @Override
        public String toString() {
            return key + ": " + (val instanceof float[] ? Arrays.toString((float[]) val) :
                (val instanceof String ? ("String<"+val+'>') : val)) + ';';
        }
    }
}
