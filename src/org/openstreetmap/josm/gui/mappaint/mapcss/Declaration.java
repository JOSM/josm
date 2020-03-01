// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.tools.Utils;

/**
 * A declaration is a list of {@link Instruction}s
 */
public class Declaration {
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
     * @param instructions The instructions for this declaration
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
