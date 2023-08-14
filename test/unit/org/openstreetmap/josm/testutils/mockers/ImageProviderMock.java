// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import java.awt.Image;

import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageResource;

import mockit.Mock;
import mockit.MockUp;

/**
 * Mock out {@link ImageProvider#getResource()} calls, which can be very expensive for SVGs.
 * Mostly useful for speeding up tests. If you are testing rendering, <i>do not call this mock!</i>
 */
public class ImageProviderMock extends MockUp<ImageProvider> {

    private static final Image EMPTY_IMAGE = ImageProvider.getEmpty(ImageProvider.ImageSizes.DEFAULT).getImage();

    @Mock
    public ImageResource getResource() {
        return new ImageResource(EMPTY_IMAGE);
    }
}
