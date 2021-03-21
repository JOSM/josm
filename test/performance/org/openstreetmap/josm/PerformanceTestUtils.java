// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.XmlWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Timer utilities for performance tests.
 * @author Michael Zangl
 */
public final class PerformanceTestUtils {
    /**
     * The Neubrandenburg data file
     */
    public static final String DATA_FILE = "nodist/data/neubrandenburg.osm.bz2";
    private static final int TIMES_WARMUP = 2;
    private static final int TIMES_RUN = 8;

    /**
     * Parses and returns the Neubrandenburg dataset
     * @return the Neubrandenburg dataset
     * @throws IllegalDataException in case of invalid data
     * @throws IOException in case of I/O error
     */
    public static DataSet getNeubrandenburgDataSet() throws IOException, IllegalDataException {
        try (InputStream in = Compression.getUncompressedFileInputStream(new File(DATA_FILE))) {
            DataSet dataSet = OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
            dataSet.setName(DATA_FILE);
            return dataSet;
        }
    }

    /**
     * A helper class that captures the time from object creation until #done() was called.
     * @author Michael Zangl
     */
    public static class PerformanceTestTimerCapture {
        private final long time;

        protected PerformanceTestTimerCapture() {
            time = System.nanoTime();
        }

        /**
         * Get the time since this object was created.
         * @return The time.
         */
        public long getTimeSinceCreation() {
            return (System.nanoTime() - time) / 1000000;
        }
    }

    /**
     * A timer that measures the time from it's creation to the {@link #done()} call.
     * @author Michael Zangl
     */
    public static class PerformanceTestTimer extends PerformanceTestTimerCapture {
        private final String name;
        private boolean measurementPlotsPlugin = true;

        protected PerformanceTestTimer(String name) {
            this.name = name;
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
            long dTime = getTimeSinceCreation();
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
     * Starts a new performance timer. The timer will output the measurements in a format understood by Jenkins.
     * <p>
     * The timer can only be used to meassure one value.
     * @param name The name/description of the timer.
     * @return A {@link PerformanceTestTimer} object of which you can call {@link PerformanceTestTimer#done()} when done.
     */
    @SuppressFBWarnings(value = "DM_GC", justification = "Performance test code")
    public static PerformanceTestTimer startTimer(String name) {
        cleanSystem();
        return new PerformanceTestTimer(name);
    }

    /**
     * Runs the given performance test several (approx. 10) times and prints the median run time.
     * @param name The name to use in the output
     * @param testRunner The test to run
     */
    public static void runPerformanceTest(String name, Runnable testRunner) {
        for (int i = 0; i < TIMES_WARMUP; i++) {
            cleanSystem();
            PerformanceTestTimerCapture capture = new PerformanceTestTimerCapture();
            testRunner.run();
            capture.getTimeSinceCreation();
        }
        ArrayList<Long> times = new ArrayList<>();
        for (int i = 0; i < TIMES_RUN; i++) {
            cleanSystem();
            PerformanceTestTimerCapture capture = new PerformanceTestTimerCapture();
            testRunner.run();
            times.add(capture.getTimeSinceCreation());
        }
        System.out.println(times);
        Collections.sort(times);
        // Sort out e.g. GC during test run.
        double avg = times.subList(2, times.size() - 2).stream().mapToLong(l -> l).average().getAsDouble();
        measurementPlotsPluginOutput(name, avg);
    }

    @SuppressFBWarnings(value = "DM_GC")
    private static void cleanSystem() {
        System.gc();
        System.runFinalization();
    }

    /**
     * Emit one data value for the Jenkins Measurement Plots Plugin.
     *
     * The plugin collects the values over multiple builds and plots them in a diagram.
     *
     * @param name the name / title of the measurement
     * @param value the value
     * @see "https://wiki.jenkins-ci.org/display/JENKINS/Measurement+Plots+Plugin"
     */
    public static void measurementPlotsPluginOutput(String name, double value) {
        System.err.println("<measurement><name>"+XmlWriter.encode(name)+"</name><value>"+value+"</value></measurement>");
    }
}
