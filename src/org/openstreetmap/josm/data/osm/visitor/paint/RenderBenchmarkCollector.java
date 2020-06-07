// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.io.PrintStream;
import java.util.List;
import java.util.function.Supplier;

import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer.StyleRecord;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class is notified of the various stages of a render pass.
 *
 * @author Michael Zangl
 * @since 10697
 */
public class RenderBenchmarkCollector {
    /**
     * Notified when the renderer method starts preparing the data
     * @param circum The current circum of the view.
     */
    public void renderStart(double circum) {
        // nop
    }

    /**
     * Notified when the renderer method starts sorting the styles
     * @return <code>true</code> if the renderer should continue to render
     */
    public boolean renderSort() {
        // nop
        return true;
    }

    /**
     * Notified when the renderer method starts drawing
     * @param allStyleElems All the elements that are painted. Unsorted
     * @return <code>true</code> if the renderer should continue to render
     */
    public boolean renderDraw(List<StyleRecord> allStyleElems) {
        // nop
        return true;
    }

    /**
     * Notified when the render method is done.
     */
    public void renderDone() {
     // nop
    }

    /**
     * A benchmark implementation that captures the times
     * @author Michael Zangl
     */
    public static class CapturingBenchmark extends RenderBenchmarkCollector {
        protected long timeStart;
        protected long timeGenerateDone;
        protected long timeSortingDone;
        protected long timeFinished;

        @Override
        public void renderStart(double circum) {
            timeStart = getCurrentTimeMilliseconds();
            super.renderStart(circum);
        }

        @Override
        public boolean renderSort() {
            timeGenerateDone = getCurrentTimeMilliseconds();
            return super.renderSort();
        }

        @Override
        public boolean renderDraw(List<StyleRecord> allStyleElems) {
            timeSortingDone = getCurrentTimeMilliseconds();
            return super.renderDraw(allStyleElems);
        }

        /**
         * Get the time needed for generating the styles
         * @return The time in ms
         */
        public long getGenerateTime() {
            return timeGenerateDone - timeStart;
        }

        /**
         * Get the time needed for computing the draw order
         * @return The time in ms
         */
        public long getSortTime() {
            return timeSortingDone - timeGenerateDone;
        }

        @Override
        public void renderDone() {
            timeFinished = getCurrentTimeMilliseconds();
            super.renderDone();
        }

        /**
         * Get the draw time
         * @return The time in ms
         */
        public long getDrawTime() {
            return timeFinished - timeGenerateDone;
        }
    }

    public static long getCurrentTimeMilliseconds() {
        return System.nanoTime() / 1000000; // System.currentTimeMillis has low accuracy, sometimes multiples of 16ms
    }

    /**
     * A special version of the benchmark class that logs the output to stderr.
     * @author Michael Zangl
     */
    public static class LoggingBenchmark extends RenderBenchmarkCollector.CapturingBenchmark {
        private final PrintStream outStream = System.err;
        private double circum;

        @Override
        public void renderStart(double circum) {
            this.circum = circum;
            super.renderStart(circum);
            outStream.print("BENCHMARK: rendering ");
        }

        @Override
        public boolean renderDraw(List<StyleRecord> allStyleElems) {
            boolean res = super.renderDraw(allStyleElems);
            outStream.print("phase 1 (calculate styles): " + Utils.getDurationString(timeSortingDone - timeStart));
            return res;
        }

        @Override
        public void renderDone() {
            super.renderDone();
            outStream.println("; phase 2 (draw): " + Utils.getDurationString(timeFinished - timeGenerateDone) +
                    "; total: " + Utils.getDurationString(timeFinished - timeStart) +
                    " (scale: " + circum + " zoom level: " + Selector.GeneralSelector.scale2level(circum) + ')');
        }
    }

    /**
     * A supplier that gets the default benchmark class.
     * @return A supplier that returns a nop or a logging benchmark.
     */
    public static Supplier<RenderBenchmarkCollector> defaultBenchmarkSupplier() {
        return () -> Logging.isTraceEnabled() || Config.getPref().getBoolean("mappaint.render.benchmark", false)
                ? new LoggingBenchmark() : new RenderBenchmarkCollector();
    }
}
