// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import java.awt.Component;
import java.util.function.IntSupplier;

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
    int getComponentHeight();

    /**
     * Check whether the user should be allowed to adjust the height of this download source panel
     * @return <code>true</code> if the height should be adjustable
     */
    boolean isHeightAdjustable();

    /**
     * Stores the height
     * @param height the height in pixel
     */
    default void storeHeight(int height) {
        throw new UnsupportedOperationException(
                "Setting the height is not supported for " + this.getClass().getCanonicalName());
    }

    /**
     * The download source has a fixed size provided by the component
     * @author Michael Zangl
     */
    class FixedDownloadSourceSizePolicy implements DownloadSourceSizingPolicy {
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
    class AdjustableDownloadSizePolicy implements DownloadSourceSizingPolicy {

        private final AbstractProperty<Integer> preference;
        private final IntSupplier minHeight;

        /**
         * Create a new {@link AdjustableDownloadSizePolicy}
         * @param preference The preference to use
         */
        public AdjustableDownloadSizePolicy(AbstractProperty<Integer> preference) {
            this(preference, () -> 1);
        }

        /**
         * Create a new {@link AdjustableDownloadSizePolicy}
         * @param preference The preference to use
         * @param minHeight A supplier that gives the minimum height of the component. Must be positive or 0.
         * @since 14418
         */
        public AdjustableDownloadSizePolicy(AbstractProperty<Integer> preference, IntSupplier minHeight) {
            this.preference = preference;
            this.minHeight = minHeight;
        }

        @Override
        public int getComponentHeight() {
            int computedMinHeight = this.minHeight.getAsInt();
            if (computedMinHeight < 0) {
                throw new IllegalStateException("Illegal minimum component height:" + computedMinHeight);
            }
            return Math.max(computedMinHeight, preference.get());
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
