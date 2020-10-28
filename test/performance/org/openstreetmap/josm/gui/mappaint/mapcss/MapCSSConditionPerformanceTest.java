// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.PerformanceTestUtils;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.Op;

/**
 * Performance test of MapCSS Condition objects.
 * @author Michael Zangl
 */
class MapCSSConditionPerformanceTest {
    /**
     * Test the performance of all OP entries.
     */
    @Test
    void testAllOps() {
        // The JIT does some really heavy optimisations if it notices that other values are not used.
        // If we want to simulate a real scenario, we need to invoke every op several times to let the compiler
        // build the jump tables.
        for (Op op : Op.values()) {
            getRunner(op).run();
        }
        for (Op op : Op.values()) {
            runTest(op);
        }
    }

    private void runTest(Op op) {
        Runnable r = getRunner(op);
        PerformanceTestUtils.runPerformanceTest("Condition.Op." + op, r);
    }

    private Runnable getRunner(Op op) {
        Runnable r;
        if (EnumSet.of(Op.LESS, Op.LESS_OR_EQUAL, Op.GREATER, Op.GREATER_OR_EQUAL).contains(op)) {
            r = () -> {
                for (int i = 0; i < 10000; i++) {
                        op.eval(null, "0.2");
                        op.eval("nan", "0.1");
                        op.eval("0.2983", "192.312");
                        op.eval("0.2983", "0.2983");
                        op.eval("2983", "1000");
                        op.eval("1000", "1000");
                }
            };
        } else {
            // regexp are slow
            int runs = EnumSet.of(Op.ONE_OF, Op.REGEX, Op.NREGEX).contains(op) ? 10000 : 100000;
            r = () -> {
                for (int i = 0; i < runs; i++) {
                    op.eval("k1", "v1");
                    op.eval("k1", "k1");
                    op.eval("", "v1");
                    op.eval(null, "abc");
                    op.eval("extreamlylongkeyextreamlylongkeyextreamlylongkeyextreamlylongkey",
                            "longvaluelongvaluelongvaluelongvalue");
                    op.eval("0.2983", "192.312");
                    op.eval("0.2983", "0.2983");
                    op.eval("2983", "\\d+");
                }
            };
        }
        return r;
    }
}
