// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.openstreetmap.josm.gui.NavigatableComponent;

/**
 * Represents a layer that has native scales.
 * @author András Kolesár
 * @since  9818 (creation)
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface NativeScaleLayer {

    /**
     * Get native scales of this layer.
     * @return {@link ScaleList} of native scales
     */
    ScaleList getNativeScales();

    /**
     * Represents a scale with native flag, used in {@link ScaleList}
     */
    class Scale {
        /**
         * Scale factor, same unit as in {@link NavigatableComponent}
         */
        private final double scale;

        /**
         * True if this scale is native resolution for data source.
         */
        private final boolean isNative;

        private final int index;

        /**
         * Constructs a new Scale with given scale, native defaults to true.
         * @param scale as defined in WMTS (scaleDenominator)
         * @param index zoom index for this scale
         */
        public Scale(double scale, int index) {
            this.scale = scale;
            this.isNative = true;
            this.index = index;
        }

        /**
         * Constructs a new Scale with given scale, native and index values.
         * @param scale as defined in WMTS (scaleDenominator)
         * @param isNative is this scale native to the source or not
         * @param index zoom index for this scale
         */
        public Scale(double scale, boolean isNative, int index) {
            this.scale = scale;
            this.isNative = isNative;
            this.index = index;
        }

        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "%f [%s]", scale, isNative);
        }

        /**
         * Get index of this scale in a {@link ScaleList}
         * @return index
         */
        public int getIndex() {
            return index;
        }

        public double getScale() {
            return scale;
        }
    }

    /**
     * List of scales, may include intermediate steps between native resolutions
     */
    class ScaleList {
        private final List<Scale> scales = new ArrayList<>();

        protected ScaleList() {
        }

        public ScaleList(Collection<Double> scales) {
            int i = 0;
            for (Double scale: scales) {
                this.scales.add(new Scale(scale, i++));
            }
        }

        protected void addScale(Scale scale) {
            scales.add(scale);
        }

        /**
         * Returns a ScaleList that has intermediate steps between native scales.
         * Native steps are split to equal steps near given ratio.
         * @param ratio user defined zoom ratio
         * @return a {@link ScaleList} with intermediate steps
         */
        public ScaleList withIntermediateSteps(double ratio) {
            ScaleList result = new ScaleList();
            Scale previous = null;
            for (Scale current: this.scales) {
                if (previous != null) {
                    double step = previous.scale / current.scale;
                    double factor = Math.log(step) / Math.log(ratio);
                    int steps = (int) Math.round(factor);
                    if (steps != 0) {
                        double smallStep = Math.pow(step, 1.0/steps);
                        for (int j = 1; j < steps; j++) {
                            double intermediate = previous.scale / Math.pow(smallStep, j);
                            result.addScale(new Scale(intermediate, false, current.index));
                        }
                    }
                }
                result.addScale(current);
                previous = current;
            }
            return result;
        }

        /**
         * Get a scale from this ScaleList or a new scale if zoomed outside.
         * @param scale previous scale
         * @param floor use floor instead of round, set true when fitting view to objects
         * @return new {@link Scale}
         */
        public Scale getSnapScale(double scale, boolean floor) {
            return getSnapScale(scale, NavigatableComponent.PROP_ZOOM_RATIO.get(), floor);
        }

        /**
         * Get a scale from this ScaleList or a new scale if zoomed outside.
         * @param scale previous scale
         * @param ratio zoom ratio from starting from previous scale
         * @param floor use floor instead of round, set true when fitting view to objects
         * @return new {@link Scale}
         */
        public Scale getSnapScale(double scale, double ratio, boolean floor) {
            if (scales.isEmpty())
                return null;
            int size = scales.size();
            Scale first = scales.get(0);
            Scale last = scales.get(size-1);

            if (scale > first.scale) {
                double step = scale / first.scale;
                double factor = Math.log(step) / Math.log(ratio);
                int steps = (int) (floor ? Math.floor(factor) : Math.round(factor));
                if (steps == 0) {
                    return new Scale(first.scale, first.isNative, steps);
                } else {
                    return new Scale(first.scale * Math.pow(ratio, steps), false, steps);
                }
            } else if (scale < last.scale) {
                double step = last.scale / scale;
                double factor = Math.log(step) / Math.log(ratio);
                int steps = (int) (floor ? Math.floor(factor) : Math.round(factor));
                if (steps == 0) {
                    return new Scale(last.scale, last.isNative, size-1+steps);
                } else {
                    return new Scale(last.scale / Math.pow(ratio, steps), false, size-1+steps);
                }
            } else {
                Scale previous = null;
                for (int i = 0; i < size; i++) {
                    Scale current = this.scales.get(i);
                    if (previous != null && scale <= previous.scale && scale >= current.scale) {
                        if (floor || previous.scale / scale < scale / current.scale) {
                            return new Scale(previous.scale, previous.isNative, i-1);
                        } else {
                            return new Scale(current.scale, current.isNative, i);
                        }
                    }
                    previous = current;
                }
                return null;
            }
        }

        /**
         * Get new scale for zoom in/out with a ratio at a number of times.
         * Used by mousewheel zoom where wheel can step more than one between events.
         * @param scale previois scale
         * @param ratio user defined zoom ratio
         * @param times number of times to zoom
         * @return new {@link Scale} object from {@link ScaleList} or outside
         */
        public Scale scaleZoomTimes(double scale, double ratio, int times) {
            Scale next = getSnapScale(scale, ratio, false);
            int abs = Math.abs(times);
            for (int i = 0; i < abs; i++) {
                if (times < 0) {
                    next = getNextIn(next, ratio);
                } else {
                    next = getNextOut(next, ratio);
                }
            }
            return next;
        }

        /**
         * Get new scale for zoom in.
         * @param scale previous scale
         * @param ratio user defined zoom ratio
         * @return next scale in list or a new scale when zoomed outside
         */
        public Scale scaleZoomIn(double scale, double ratio) {
            Scale snap = getSnapScale(scale, ratio, false);
            return getNextIn(snap, ratio);
        }

        /**
         * Get new scale for zoom out.
         * @param scale previous scale
         * @param ratio user defined zoom ratio
         * @return next scale in list or a new scale when zoomed outside
         */
        public Scale scaleZoomOut(double scale, double ratio) {
            Scale snap = getSnapScale(scale, ratio, false);
            return getNextOut(snap, ratio);
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            for (Scale s: this.scales) {
                stringBuilder.append(s.toString() + '\n');
            }
            return stringBuilder.toString();
        }

        private Scale getNextIn(Scale scale, double ratio) {
            if (scale == null)
                return null;
            int nextIndex = scale.getIndex() + 1;
            if (nextIndex <= 0 || nextIndex > this.scales.size()-1) {
                return new Scale(scale.scale / ratio, nextIndex == 0, nextIndex);
            } else {
                Scale nextScale = this.scales.get(nextIndex);
                return new Scale(nextScale.scale, nextScale.isNative, nextIndex);
            }
        }

        private Scale getNextOut(Scale scale, double ratio) {
            if (scale == null)
                return null;
            int nextIndex = scale.getIndex() - 1;
            if (nextIndex < 0 || nextIndex >= this.scales.size()-1) {
                return new Scale(scale.scale * ratio, nextIndex == this.scales.size()-1, nextIndex);
            } else {
                Scale nextScale = this.scales.get(nextIndex);
                return new Scale(nextScale.scale, nextScale.isNative, nextIndex);
            }
        }
    }
}
