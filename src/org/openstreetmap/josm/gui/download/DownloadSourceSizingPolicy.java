// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import java.awt.Component;

import org.openstreetmap.josm.data.preferences.AbstractProperty;

/**
 * Defines the sizing policy used for download tabs.
 * @author Michael Zangl
 * @since 12705
 */
public interface DownloadSourceSizingPolicy {
    /**
     * Gets the height of the download source panel.
     * @return The height the component should have.
     */
    public int getComponentHeight();

    /**
     * Check whether the user should be allowed to adjust the height of this download source panel
     * @return <code>true</code> if the height should be adjustable
     */
    public boolean isHeightAdjustable();

    /**
     * Stores the height
     * @param height the height in pixel
     */
    public default void storeHeight(int height) {
        throw new UnsupportedOperationException(
                "Setting the height is not supported for " + this.getClass().getCanonicalName());
    }

    /**
     * The download source has a fixed size provided by the component
     * @author Michael Zangl
     */
    public class FixedDownloadSourceSizePolicy implements DownloadSourceSizingPolicy {
        private final Component base;

        /**
         * Create a new fixed download source policy
         * @param base The component of which the size should be taken.
         */
        public FixedDownloadSourceSizePolicy(Component base) {
            this.base = base;
        }

        @Override
        public int getComponentHeight() {
            return (int) base.getPreferredSize().getHeight();
        }

        @Override
        public boolean isHeightAdjustable() {
            return false;
        }
    }

    /**
     * The height of this component is given by a preference entry.
     * <p>
     * Mind that using a preferred component size is not possible in this case, since the preference entry needs to have a onstant default value.
     */
    public class AdjustableDownloadSizePolicy implements DownloadSourceSizingPolicy {

        private final AbstractProperty<Integer> preference;

        /**
         * Create a new {@link AdjustableDownloadSizePolicy}
         * @param preference The preference key to use
         */
        public AdjustableDownloadSizePolicy(AbstractProperty<Integer> preference) {
            this.preference = preference;
        }

        @Override
        public int getComponentHeight() {
            return Math.max(1, preference.get());
        }

        @Override
        public boolean isHeightAdjustable() {
            return true;
        }

        @Override
        public void storeHeight(int height) {
            preference.put(height);
        }

    }
}
