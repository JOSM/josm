// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openstreetmap.josm.tools.ImageProcessor;

/**
 * This class holds the filter settings for an imagery layer.
 * @author Michael Zangl
 * @since 10547
 */
public class ImageryFilterSettings {

    protected GammaImageProcessor gammaImageProcessor = new GammaImageProcessor();
    protected SharpenImageProcessor sharpenImageProcessor = new SharpenImageProcessor();
    protected ColorfulImageProcessor collorfulnessImageProcessor = new ColorfulImageProcessor();
    private final List<FilterChangeListener> filterChangeListeners = new CopyOnWriteArrayList<>();

    /**
     * Returns the currently set gamma value.
     * @return the currently set gamma value
     */
    public double getGamma() {
        return gammaImageProcessor.getGamma();
    }

    /**
     * Sets a new gamma value, {@code 1} stands for no correction.
     * @param gamma new gamma value
     */
    public void setGamma(double gamma) {
        gammaImageProcessor.setGamma(gamma);
        fireListeners();
    }

    /**
     * Gets the current sharpen level.
     * @return The sharpen level.
     */
    public double getSharpenLevel() {
        return sharpenImageProcessor.getSharpenLevel();
    }

    /**
     * Sets the sharpen level for the layer.
     * <code>1</code> means no change in sharpness.
     * Values in range 0..1 blur the image.
     * Values above 1 are used to sharpen the image.
     * @param sharpenLevel The sharpen level.
     */
    public void setSharpenLevel(double sharpenLevel) {
        sharpenImageProcessor.setSharpenLevel((float) sharpenLevel);
        fireListeners();
    }

    /**
     * Gets the colorfulness of this image.
     * @return The colorfulness
     */
    public double getColorfulness() {
        return collorfulnessImageProcessor.getColorfulness();
    }

    /**
     * Sets the colorfulness of this image.
     * 0 means grayscale.
     * 1 means normal colorfulness.
     * Values greater than 1 are allowed.
     * @param colorfulness The colorfulness.
     */
    public void setColorfulness(double colorfulness) {
        collorfulnessImageProcessor.setColorfulness(colorfulness);
        fireListeners();
    }

    /**
     * Gets the image processors for this setting.
     * @return The processors in the order in which they should be applied.
     */
    public List<ImageProcessor> getProcessors() {
        return Arrays.asList(collorfulnessImageProcessor, gammaImageProcessor, sharpenImageProcessor);
    }

    /**
     * Adds a filter change listener
     * @param l The listener
     */
    public void addFilterChangeListener(FilterChangeListener l) {
        filterChangeListeners.add(l);
    }

    /**
     * Removes a filter change listener
     * @param l The listener
     */
    public void removeFilterChangeListener(FilterChangeListener l) {
        filterChangeListeners.remove(l);
    }

    private void fireListeners() {
        for (FilterChangeListener l : filterChangeListeners) {
            l.filterChanged();
        }
    }

    /**
     * A listener that listens to filter changes
     * @author Michael Zangl
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    public interface FilterChangeListener {
        /**
         * Invoked when the filter is changed.
         */
        void filterChanged();
    }
}
