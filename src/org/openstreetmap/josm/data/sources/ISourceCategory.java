// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.sources;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

/**
 * This is an enum for a source category (i.e. PHOTO/ELEVATION/etc.)
 *
 * @author Taylor Smock
 *
 * @param <T> The enum that is extending this interface
 * @since 16545
 */
public interface ISourceCategory<T extends Enum<T>> extends ICommonSource<T> {
    /**
     * Returns the unique string identifying this category.
     * @return the unique string identifying this category
     */
    String getCategoryString();

    /**
     * Returns the description of this category.
     * @return the description of this category
     */
    String getDescription();

    /**
     * Returns the category icon at the given size.
     * @param size icon wanted size
     * @return the category icon at the given size
     */
    ImageIcon getIcon(ImageSizes size);
}
