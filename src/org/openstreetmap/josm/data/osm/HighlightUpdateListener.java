// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * This is a listener that listens to highlight segment changes.
 * @author Michael Zangl
 * @since 12014
 */
@FunctionalInterface
public interface HighlightUpdateListener {

    /**
     * An event that is fired whenever highlighting on the OSM {@link DataSet} changed.
     * @author Michael Zangl
     * @since 12014
     */
    public class HighlightUpdateEvent {
        private final DataSet dataSet;

        /**
         * Create a new highlight update event.
         * @param dataSet The dataset that was changed.
         */
        public HighlightUpdateEvent(DataSet dataSet) {
            this.dataSet = dataSet;
        }

        /**
         * Get the modified data set.
         * @return The data set.
         */
        public DataSet getDataSet() {
            return dataSet;
        }
    }

    /**
     * Called whenever the highlighting of way segments in the dataset changed.
     * @param e The dataset highlight event.
     */
    void highlightUpdated(HighlightUpdateEvent e);
}
