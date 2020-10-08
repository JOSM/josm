// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.openstreetmap.josm.data.ImageData.ImageDataUpdateListener;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;

import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;

/**
 * Unit tests for class {@link ImageData}.
 */
public class ImageDataTest {

    private static List<ImageEntry> getOneImage() {
        ArrayList<ImageEntry> list = new ArrayList<>();
        list.add(new ImageEntry(new File("test")));
        return list;
    }

    @Test
    public void testWithNullData() {
        ImageData data = new ImageData();
        assertEquals(0, data.getImages().size());
        assertNull(data.getSelectedImage());
        data.selectFirstImage();
        assertNull(data.getSelectedImage());
        data.selectLastImage();
        assertNull(data.getSelectedImage());
        data.selectFirstImage();
        assertNull(data.getSelectedImage());
        data.selectPreviousImage();
        assertNull(data.getSelectedImage());
        assertFalse(data.hasNextImage());
        assertFalse(data.hasPreviousImage());
        data.removeSelectedImage();
    }

    @Test
    public void testImageEntryWithImages() {
        assertEquals(1, new ImageData(getOneImage()).getImages().size());
    }

    @Test
    public void testSortData(@Mocked Collections ignore) {
        List<ImageEntry> list = getOneImage();

        new ImageData(list);

        new Verifications() {{
            Collections.sort(list);
        }};
    }

    @Test
    public void testIsModifiedFalse() {
        assertFalse(new ImageData(getOneImage()).isModified());
    }

    @Test
    public void testIsModifiedTrue() {
        List<ImageEntry> list = getOneImage();

        new Expectations(list.get(0)) {{
            list.get(0).hasNewGpsData(); result = true;
        }};

        assertTrue(new ImageData(list).isModified());
    }

    @Test
    public void testSelectFirstImage() {
        List<ImageEntry> list = getOneImage();

        ImageData data = new ImageData(list);
        data.selectFirstImage();
        assertEquals(1, data.getSelectedImages().size());
        assertEquals(list.get(0), data.getSelectedImages().get(0));
    }

    @Test
    public void testSelectLastImage() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry());

        ImageData data = new ImageData(list);
        data.selectLastImage();
        assertEquals(1, data.getSelectedImages().size());
        assertEquals(list.get(1), data.getSelectedImages().get(0));
    }

    @Test
    public void testSelectNextImage() {
        List<ImageEntry> list = getOneImage();

        ImageData data = new ImageData(list);
        assertTrue(data.hasNextImage());
        data.selectNextImage();
        assertEquals(1, data.getSelectedImages().size());
        assertEquals(list.get(0), data.getSelectedImages().get(0));
        assertFalse(data.hasNextImage());
        data.selectNextImage();
        assertEquals(list.get(0), data.getSelectedImages().get(0));
    }

    @Test
    public void testSelectPreviousImage() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry());

        ImageData data = new ImageData(list);
        assertFalse(data.hasPreviousImage());
        data.selectLastImage();
        assertTrue(data.hasPreviousImage());
        data.selectPreviousImage();
        assertEquals(1, data.getSelectedImages().size());
        assertEquals(list.get(0), data.getSelectedImages().get(0));
        data.selectPreviousImage();
        assertEquals(list.get(0), data.getSelectedImages().get(0));
    }

    @Test
    public void testSetSelectedImage() {
        List<ImageEntry> list = getOneImage();

        ImageData data = new ImageData(list);
        data.setSelectedImage(list.get(0));
        assertEquals(1, data.getSelectedImages().size());
        assertEquals(list.get(0), data.getSelectedImages().get(0));
    }

    @Test
    public void testClearSelectedImages() {
        List<ImageEntry> list = getOneImage();

        ImageData data = new ImageData(list);
        data.setSelectedImage(list.get(0));
        data.clearSelectedImage();
        assertTrue(data.getSelectedImages().isEmpty());
    }

    @Test
    public void testSelectionListener() {
        List<ImageEntry> list = getOneImage();
        ImageData data = new ImageData(list);
        ImageDataUpdateListener listener = new ImageDataUpdateListener() {
            @Override
            public void selectedImageChanged(ImageData data) {}

            @Override
            public void imageDataUpdated(ImageData data) {}
        };
        new Expectations(listener) {{
            listener.selectedImageChanged(data); times = 1;
        }};
        data.addImageDataUpdateListener(listener);
        data.selectFirstImage();
        data.selectFirstImage();
    }

    @Test
    public void testRemoveSelectedImage() {
        List<ImageEntry> list = getOneImage();
        ImageData data = new ImageData(list);
        data.selectFirstImage();
        data.removeSelectedImage();
        assertEquals(0, data.getImages().size());
        assertEquals(0, data.getSelectedImages().size());
    }

    @Test
    public void testRemoveSelectedImages() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry());

        ImageData data = new ImageData(list);
        data.selectFirstImage();
        data.addImageToSelection(list.get(1));
        data.removeSelectedImages();
        assertEquals(0, data.getImages().size());
        assertEquals(0, data.getSelectedImages().size());
    }

    @Test
    public void testRemoveSelectedImagesWithRemainingImages() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry());
        list.add(new ImageEntry());

        ImageData data = new ImageData(list);
        data.selectLastImage();
        data.addImageToSelection(list.get(1));
        data.removeSelectedImages();
        assertEquals(1, data.getImages().size());
        assertEquals(1, data.getSelectedImages().size());
    }

    @Test
    public void testSelectImageAfterRemove() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry());

        ImageData data = new ImageData(list);
        data.selectFirstImage();
        data.removeSelectedImages();
        assertEquals(1, data.getImages().size());
        assertEquals(1, data.getSelectedImages().size());
        assertEquals(list.get(0), data.getSelectedImages().get(0));
    }

    @Test
    public void testSelectImageAfterRemoveWhenTheLastIsSelected() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry());

        ImageData data = new ImageData(list);
        data.selectLastImage();
        data.removeSelectedImages();
        assertEquals(1, data.getImages().size());
        assertEquals(1, data.getSelectedImages().size());
        assertEquals(list.get(0), data.getSelectedImages().get(0));
    }

    @Test
    public void testRemoveSelectedImageTriggerListener() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry());
        ImageData data = new ImageData(list);
        ImageDataUpdateListener listener = new ImageDataUpdateListener() {
            @Override
            public void selectedImageChanged(ImageData data) {}

            @Override
            public void imageDataUpdated(ImageData data) {}
        };
        new Expectations(listener) {{
            listener.selectedImageChanged(data); times = 2;
        }};
        data.addImageDataUpdateListener(listener);
        data.selectFirstImage();
        data.removeSelectedImage();
    }

    @Test
    public void testRemoveSelectedImagesTriggerListener() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry());
        ImageData data = new ImageData(list);
        ImageDataUpdateListener listener = new ImageDataUpdateListener() {
            @Override
            public void selectedImageChanged(ImageData data) {}

            @Override
            public void imageDataUpdated(ImageData data) {}
        };
        new Expectations(listener) {{
            listener.selectedImageChanged(data); times = 2;
        }};
        data.addImageDataUpdateListener(listener);
        data.selectFirstImage();
        data.removeSelectedImages();
    }

    @Test
    public void testRemoveImageAndTriggerListener() {
        List<ImageEntry> list = getOneImage();
        ImageData data = new ImageData(list);
        ImageDataUpdateListener listener = new ImageDataUpdateListener() {
            @Override
            public void selectedImageChanged(ImageData data) {}

            @Override
            public void imageDataUpdated(ImageData data) {}
        };
        new Expectations(listener) {{
            listener.imageDataUpdated(data); times = 1;
        }};
        data.addImageDataUpdateListener(listener);
        data.removeImage(list.get(0));
        assertEquals(0, data.getImages().size());
    }

    @Test
    public void testMergeFrom() {
        ImageEntry image = new ImageEntry(new File("test2"));
        List<ImageEntry> list1 = getOneImage();
        list1.add(image);
        List<ImageEntry> list2 = getOneImage();
        list2.add(new ImageEntry(new File("test3")));

        ImageData data = new ImageData(list1);
        data.setSelectedImage(list1.get(0));
        ImageData data2 = new ImageData(list2);

        new MockUp<Collections>() {
            @Mock
            public void sort(List<ImageEntry> o) {
                list1.remove(image);
                list1.add(image);
            }
        };

        data.mergeFrom(data2);
        assertEquals(3, data.getImages().size());
        assertEquals(1, data.getSelectedImages().size());
        assertEquals(list1.get(0), data.getSelectedImages().get(0));
    }

    @Test
    public void testMergeFromSelectedImage() {
        ImageEntry image = new ImageEntry(new File("test2"));
        List<ImageEntry> list1 = getOneImage();
        list1.add(image);
        List<ImageEntry> list2 = getOneImage();

        ImageData data = new ImageData(list1);
        ImageData data2 = new ImageData(list2);
        data2.setSelectedImage(list2.get(0));

        data.mergeFrom(data2);
        assertEquals(3, data.getImages().size());
        assertEquals(1, data.getSelectedImages().size());
        assertEquals(list2.get(0), data.getSelectedImages().get(0));
    }

    @Test
    public void testAddImageToSelection() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry(new File("test2")));

        ImageData data = new ImageData(list);
        data.addImageToSelection(list.get(0));
        data.addImageToSelection(list.get(0));
        assertEquals(1, data.getSelectedImages().size());
        data.addImageToSelection(list.get(1));
        assertEquals(2, data.getSelectedImages().size());
    }

    @Test
    public void testRemoveImageToSelection() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry());

        ImageData data = new ImageData(list);
        data.selectLastImage();
        data.removeImageToSelection(list.get(1));
        assertEquals(0, data.getSelectedImages().size());
        data.selectFirstImage();
        assertEquals(1, data.getSelectedImages().size());
    }

    @Test
    public void testIsSelected() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry(new File("test2")));

        ImageData data = new ImageData(list);
        assertFalse(data.isImageSelected(list.get(0)));
        data.selectFirstImage();
        assertTrue(data.isImageSelected(list.get(0)));
        data.addImageToSelection(list.get(1));
        assertTrue(data.isImageSelected(list.get(0)));
        assertTrue(data.isImageSelected(list.get(1)));
        assertFalse(data.isImageSelected(new ImageEntry()));
    }

    @Test
    public void testActionsWithMultipleImagesSelected() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry(new File("test2")));
        list.add(new ImageEntry(new File("test3")));
        list.add(new ImageEntry(new File("test3")));

        ImageData data = new ImageData(list);
        data.addImageToSelection(list.get(1));
        data.addImageToSelection(list.get(2));

        assertFalse(data.hasNextImage());
        assertFalse(data.hasPreviousImage());

        data.clearSelectedImage();
        assertEquals(0, data.getSelectedImages().size());
        data.addImageToSelection(list.get(1));
        data.selectFirstImage();
        assertEquals(1, data.getSelectedImages().size());
    }

    @Test
    public void testTriggerListenerWhenNewImageIsSelectedAndRemoved() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry());
        ImageData data = new ImageData(list);
        ImageDataUpdateListener listener = new ImageDataUpdateListener() {
            @Override
            public void selectedImageChanged(ImageData data) {}

            @Override
            public void imageDataUpdated(ImageData data) {}
        };
        new Expectations(listener) {{
            listener.selectedImageChanged(data); times = 3;
        }};
        data.addImageDataUpdateListener(listener);
        data.selectFirstImage();
        data.addImageToSelection(list.get(1));
        data.removeImageToSelection(list.get(0));
    }

    @Test
    public void testUpdatePosition() {
        List<ImageEntry> list = getOneImage();
        ImageData data = new ImageData(list);

        new Expectations(list.get(0)) {{
            list.get(0).setPos((LatLon) any);
            list.get(0).flagNewGpsData();
        }};
        data.updateImagePosition(list.get(0), new LatLon(0, 0));
    }

    @Test
    public void testUpdateDirection() {
        List<ImageEntry> list = getOneImage();
        ImageData data = new ImageData(list);

        new Expectations(list.get(0)) {{
            list.get(0).setExifImgDir(0.0);
            list.get(0).flagNewGpsData();
        }};
        data.updateImageDirection(list.get(0), 0);
    }

    @Test
    public void testTriggerListenerOnUpdate() {
        List<ImageEntry> list = getOneImage();
        ImageData data = new ImageData(list);

        ImageDataUpdateListener listener = new ImageDataUpdateListener() {
            @Override
            public void selectedImageChanged(ImageData data) {}

            @Override
            public void imageDataUpdated(ImageData data) {}
        };
        new Expectations(listener) {{
            listener.imageDataUpdated(data); times = 1;
        }};

        data.addImageDataUpdateListener(listener);
        data.updateImageDirection(list.get(0), 0);
    }

    @Test
    public void testManuallyTriggerUpdateListener() {
        List<ImageEntry> list = getOneImage();
        ImageData data = new ImageData(list);

        ImageDataUpdateListener listener = new ImageDataUpdateListener() {
            @Override
            public void selectedImageChanged(ImageData data) {}

            @Override
            public void imageDataUpdated(ImageData data) {}
        };
        new Expectations(listener) {{
            listener.imageDataUpdated(data); times = 1;
        }};

        data.addImageDataUpdateListener(listener);
        data.notifyImageUpdate();
    }
}
