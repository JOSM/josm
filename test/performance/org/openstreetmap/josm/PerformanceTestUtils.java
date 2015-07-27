// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

/**
 * Timer utilities for performance tests.
 * @author Michael Zangl
 */
public class PerformanceTestUtils {
    /**
     * A timer that measures the time from it's creation to the {@link #done()} call.
    * @author Michael Zangl
     */
    public static class PerformanceTestTimer {
        private String name;
        private long time;

        protected PerformanceTestTimer(String name) {
            this.name = name;
            time = System.nanoTime();
        }

        /**
         * Prints the time since this timer was created.
         */
        public void done() {
            long dTime = System.nanoTime() - time;
            System.out.println("TIMER " + name + ": " + dTime / 1000000 + "ms");
        }
    }

    private PerformanceTestUtils() {
    }

    /**
     * Starts a new performance timer.
     * @param name The name/description of the timer.
     * @return A {@link PerformanceTestTimer} object of which you can call {@link PerformanceTestTimer#done()} when done.
     */
    public static PerformanceTestTimer startTimer(String name) {
        System.gc();
        System.runFinalization();
        return new PerformanceTestTimer(name);
    }
}
