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
    public void testWithullData() {
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
    public void testSortData() {
        List<ImageEntry> list = getOneImage();

        new Expectations(Collections.class) {{
            Collections.sort(list);
        }};

        new ImageData(list);
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
        assertEquals(list.get(0), data.getSelectedImage());
    }

    @Test
    public void testSelectLastImage() {
        List<ImageEntry> list = getOneImage();
        list.add(new ImageEntry());

        ImageData data = new ImageData(list);
        data.selectLastImage();
        assertEquals(list.get(1), data.getSelectedImage());
    }

    @Test
    public void testSelectNextImage() {
        List<ImageEntry> list = getOneImage();

        ImageData data = new ImageData(list);
        assertTrue(data.hasNextImage());
        data.selectNextImage();
        assertEquals(list.get(0), data.getSelectedImage());
        assertFalse(data.hasNextImage());
        data.selectNextImage();
        assertEquals(list.get(0), data.getSelectedImage());
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
        assertEquals(list.get(0), data.getSelectedImage());
        data.selectPreviousImage();
        assertEquals(list.get(0), data.getSelectedImage());
    }

    @Test
    public void testSetSelectedImage() {
        List<ImageEntry> list = getOneImage();

        ImageData data = new ImageData(list);
        data.setSelectedImage(list.get(0));
        assertEquals(list.get(0), data.getSelectedImage());
    }

    @Test
    public void testClearSelectedImage() {
        List<ImageEntry> list = getOneImage();

        ImageData data = new ImageData(list);
        data.setSelectedImage(list.get(0));
        data.clearSelectedImage();
        assertNull(data.getSelectedImage());
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
        assertNull(data.getSelectedImage());
    }

    @Test
    public void testRemoveSelectedWithImageTriggerListener() {
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
        assertEquals(list1.get(0), data.getSelectedImage());
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
        assertEquals(list2.get(0), data.getSelectedImage());
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
