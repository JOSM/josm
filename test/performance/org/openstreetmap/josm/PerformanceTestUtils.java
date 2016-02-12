// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.openstreetmap.josm.io.XmlWriter;

/**
 * Timer utilities for performance tests.
 * @author Michael Zangl
 */
public final class PerformanceTestUtils {
    /**
     * A timer that measures the time from it's creation to the {@link #done()} call.
     * @author Michael Zangl
     */
    public static class PerformanceTestTimer {
        private final String name;
        private final long time;
        private boolean measurementPlotsPlugin = false;

        protected PerformanceTestTimer(String name) {
            this.name = name;
            time = System.nanoTime();
        }

        /**
         * Activate output for the Jenkins Measurement Plots Plugin.
         * @param active true if it should be activated
         */
        public void setMeasurementPlotsPluginOutput(boolean active) {
            measurementPlotsPlugin = active;
        }

        /**
         * Prints the time since this timer was created.
         */
        public void done() {
            long dTime = (System.nanoTime() - time) / 1000000;
            if (measurementPlotsPlugin) {
                measurementPlotsPluginOutput(name + "(ms)", dTime);
            } else {
                System.out.println("TIMER " + name + ": " + dTime + "ms");
            }
        }
    }

    private PerformanceTestUtils() {
    }

    /**
     * Starts a new performance timer.
     * @param name The name/description of the timer.
     * @return A {@link PerformanceTestTimer} object of which you can call {@link PerformanceTestTimer#done()} when done.
     */
    @SuppressFBWarnings(value = "DM_GC", justification = "Performance test code")
    public static PerformanceTestTimer startTimer(String name) {
        System.gc();
        System.runFinalization();
        return new PerformanceTestTimer(name);
    }

    /**
     * Emit one data value for the Jenkins Measurement Plots Plugin.
     * 
     * The plugin collects the values over multiple builds and plots them in a diagram.
     * 
     * @see https://wiki.jenkins-ci.org/display/JENKINS/Measurement+Plots+Plugin
     * @param name the name / title of the measurement
     * @param value the value
     */
    public static void measurementPlotsPluginOutput(String name, double value) {
        System.err.println("<measurement><name>"+XmlWriter.encode(name)+"</name><value>"+value+"</value></measurement>");
    }
}
